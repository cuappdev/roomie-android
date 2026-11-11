package com.example.roomie.data.api

import androidx.compose.runtime.Composable
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Starts [call] on this scope and pipelines the result into [into], like Resell's `fetchX()`s.
 * Shows [RoomieApiResponse.Pending] first unless [refresh] is true; a failed refresh keeps the
 * data already shown instead of replacing it with an error.
 */
internal fun <T> CoroutineScope.fetchInto(
    into: MutableStateFlow<RoomieApiResponse<T>>,
    refresh: Boolean = false,
    call: suspend () -> T,
) {
    if (!refresh) into.value = RoomieApiResponse.Pending
    launch {
        val result = apiCall(call)
        if (!(refresh && result is RoomieApiResponse.Error && into.value is RoomieApiResponse.Success)) {
            into.value = result
        }
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
