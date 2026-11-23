package com.example.roomie.data.api

import androidx.compose.runtime.Composable
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.format.DateTimeParseException

/**
 * The state of an API call returning [T]: still loading ([Pending]), done ([Success]), or failed
 * ([Error]). Endpoints that return 204 give `Success(Unit)`.
 */
sealed class RoomieApiResponse<out T> {
    data object Pending : RoomieApiResponse<Nothing>()
    data class Success<out T>(val data: T) : RoomieApiResponse<T>()
    data class Error(val error: ApiError) : RoomieApiResponse<Nothing>()

    fun <R> map(transform: (T) -> R): RoomieApiResponse<R> = when (this) {
        Pending -> Pending
        is Success -> Success(transform(data))
        is Error -> this
    }

    /** Both responses' data as a pair. [Error] wins over [Pending]. */
    fun <K> combine(other: RoomieApiResponse<K>): RoomieApiResponse<Pair<T, K>> = when {
        this is Error -> this
        other is Error -> other
        this is Success && other is Success -> Success(data to other.data)
        else -> Pending
    }

    fun dataOrNull(): T? = (this as? Success)?.data

    fun asSuccessOrNull(): Success<T>? = this as? Success

    /** Basically a force `!!`, but for [Success] responses. */
    fun asSuccess(): Success<T> =
        this as? Success ?: throw IllegalStateException("Response is not a success: $this")

    /** Runs [block] if the response is a [Success]. */
    fun ifSuccess(block: (T) -> Unit) {
        if (this is Success) block(data)
    }

    @Composable
    fun ComposableIfSuccess(block: @Composable (T) -> Unit) {
        if (this is Success) block(data)
    }
}

/** A response with its data dropped, for screens that only show loading, done, or failed. */
sealed class RoomieApiState {
    data object Success : RoomieApiState()
    data object Error : RoomieApiState()
    data object Loading : RoomieApiState()
}

fun RoomieApiResponse<*>.toRoomieApiState(): RoomieApiState = when (this) {
    is RoomieApiResponse.Success -> RoomieApiState.Success
    is RoomieApiResponse.Error -> RoomieApiState.Error
    RoomieApiResponse.Pending -> RoomieApiState.Loading
}

data class ApiError(
    val code: ApiErrorCode,
    /** The HTTP status, or null when the request never got a response. */
    val statusCode: Int?,
    val message: String,
    /** For [ApiErrorCode.VALIDATION_ERROR]: each bad field (e.g. `body.name`) and what's wrong with it. */
    val fieldErrors: Map<String, String> = emptyMap(),
)

/** The backend's `errorCode`s, plus [NETWORK] and [UNKNOWN] for failures it didn't describe. */
enum class ApiErrorCode {
    VALIDATION_ERROR, BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, TOO_MANY_REQUESTS,
    NETWORK, UNKNOWN,
}

/** Runs one API call and turns any failure into a [RoomieApiResponse.Error] instead of throwing. */
suspend fun <T> apiCall(call: suspend () -> T): RoomieApiResponse<T> = try {
    RoomieApiResponse.Success(call())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    RoomieApiResponse.Error(e.toApiError())
} catch (e: IOException) {
    RoomieApiResponse.Error(ApiError(ApiErrorCode.NETWORK, null, "Couldn't reach the server."))
} catch (e: JsonParseException) {
    RoomieApiResponse.Error(ApiError(ApiErrorCode.UNKNOWN, null, "Unexpected response: ${e.message}"))
} catch (e: DateTimeParseException) {
    RoomieApiResponse.Error(ApiError(ApiErrorCode.UNKNOWN, null, "Unexpected date: ${e.message}"))
}

/** The scope a repository runs its fetches in. Lives as long as the (singleton) repository. */
internal fun repositoryScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * A [RoomieApiResponse] that a repository fetches into and patches, exposed as [state].
 *
 * Only the latest fetch may publish: starting a fetch, or replacing the value with [set], cancels
 * the fetch in flight and discards its result even if it already finished. Every write goes
 * through one lock, so concurrent [update]s can't overwrite each other.
 */
internal class ResponseFlow<T>(private val scope: CoroutineScope) {
    private val lock = Any()
    private val flow = MutableStateFlow<RoomieApiResponse<T>>(RoomieApiResponse.Pending)

    /** Bumped by every [fetch] and [set]; a fetch publishes only if it's still the latest. */
    private var generation = 0L
    private var inFlight: Job? = null

    val state: StateFlow<RoomieApiResponse<T>> = flow.asStateFlow()

    /**
     * Starts [call] and pipelines the result into [state], like Resell's `fetchX()`s. Shows
     * [RoomieApiResponse.Pending] first unless [refresh] is true; a failed refresh keeps the data
     * already shown instead of replacing it with an error. Returns the fetch's [Job].
     */
    fun fetch(refresh: Boolean = false, call: suspend () -> T): Job = synchronized(lock) {
        val mine = invalidate()
        if (!refresh) flow.value = RoomieApiResponse.Pending
        scope.launch {
            val result = apiCall(call)
            synchronized(lock) {
                if (mine != generation) return@synchronized
                val keepShownData = refresh && result is RoomieApiResponse.Error &&
                    flow.value is RoomieApiResponse.Success
                if (!keepShownData) flow.value = result
                inFlight = null
            }
        }.also { inFlight = it }
    }

    /** Replaces the value outright. Any fetch in flight is cancelled and can't overwrite it. */
    fun set(value: RoomieApiResponse<T>) = synchronized(lock) {
        invalidate()
        flow.value = value
    }

    /**
     * Patches the value in place, atomically with respect to other writes. Unlike [set], this
     * leaves a fetch in flight alone: that fetch is what the caller last asked to see.
     */
    fun update(transform: (RoomieApiResponse<T>) -> RoomieApiResponse<T>) = synchronized(lock) {
        flow.value = transform(flow.value)
    }

    /** Must hold [lock]. */
    private fun invalidate(): Long {
        inFlight?.cancel()
        inFlight = null
        return ++generation
    }
}

/** `{ status, statusCode, message, errorCode, data }`, the body of every error response. */
private class ErrorBody(val message: String?, val errorCode: String?, val data: JsonElement?)

private fun HttpException.toApiError(): ApiError {
    val body = try {
        response()?.errorBody()?.string()?.let { RoomieGson.gson.fromJson(it, ErrorBody::class.java) }
    } catch (e: Exception) {
        null
    }
    val code = ApiErrorCode.entries.firstOrNull { it.name == body?.errorCode } ?: when (code()) {
        400 -> ApiErrorCode.BAD_REQUEST
        401 -> ApiErrorCode.UNAUTHORIZED
        403 -> ApiErrorCode.FORBIDDEN
        404 -> ApiErrorCode.NOT_FOUND
        409 -> ApiErrorCode.CONFLICT
        429 -> ApiErrorCode.TOO_MANY_REQUESTS
        else -> ApiErrorCode.UNKNOWN
    }
    val fieldErrors = body?.data?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()
        ?.filter { it.value.isJsonPrimitive }
        ?.associate { it.key to it.value.asString }
        .orEmpty()
    return ApiError(code, code(), body?.message ?: message(), fieldErrors)
}
