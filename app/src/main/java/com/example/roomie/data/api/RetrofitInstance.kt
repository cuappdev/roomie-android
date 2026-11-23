package com.example.roomie.data.api

import com.example.roomie.BuildConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Retrofit client every Roomie API service shares. It adds `Authorization: Bearer <token>`
 * to each request and, on a 401, asks [TokenProvider] for a fresh token and retries once, as long as
 * the request went to the API's own origin and the user hasn't switched accounts in the meantime.
 */
@Singleton
class RetrofitInstance internal constructor(retrofit: Retrofit) {

    @Inject
    constructor(tokenProvider: TokenProvider) :
        this(roomieRetrofit(BuildConfig.API_URL, tokenProvider, logBodies = BuildConfig.DEBUG))

    val userApi: UserApiService by lazy { retrofit.create(UserApiService::class.java) }
    val houseApi: HouseApiService by lazy { retrofit.create(HouseApiService::class.java) }
    val choreApi: ChoreApiService by lazy { retrofit.create(ChoreApiService::class.java) }
}

/** Builds the client. Separate from [RetrofitInstance] so tests can point it at a fake server. */
internal fun roomieRetrofit(
    baseUrl: String,
    tokenProvider: TokenProvider,
    logBodies: Boolean,
): Retrofit {
    val apiOrigin = baseUrl.toHttpUrl()

    val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        // Read the session before the token: if they disagree, the retry is refused (the safe side).
        val session = AuthSession(tokenProvider.session)
        val token = runBlocking { tokenProvider.token(forceRefresh = false) }
        val tagged = request.newBuilder().tag(AuthSession::class.java, session)
        if (token != null && request.header("Authorization") == null) {
            tagged.header("Authorization", "Bearer $token")
        }
        chain.proceed(tagged.build())
    }

    val authenticator = Authenticator { _, response ->
        val request = response.request
        // Never send the token anywhere but the API itself, e.g. after a cross-origin redirect.
        if (!request.url.hasSameOrigin(apiOrigin)) return@Authenticator null
        // Retry once. Count only earlier 401s: redirects and retries also show up in priorResponse.
        if (response.prior401Count() > 0) return@Authenticator null
        // The request belongs to the session it started in. If the user switched accounts since,
        // replaying it with the new account's token would act as the wrong user.
        val startedIn = request.tag(AuthSession::class.java) ?: return@Authenticator null
        if (startedIn.id != tokenProvider.session) return@Authenticator null
        val token = runBlocking { tokenProvider.token(forceRefresh = true) }
            ?: return@Authenticator null
        if (startedIn.id != tokenProvider.session) return@Authenticator null
        request.newBuilder().header("Authorization", "Bearer $token").build()
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .apply {
            if (logBodies) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    redactHeader("Authorization")
                })
            }
        }
        .authenticator(authenticator)
        .build()

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(RoomieGson.gson))
        .build()
}

/** Which [TokenProvider.session] a request was started in. Attached as an OkHttp request tag. */
private class AuthSession(val id: Long)

private fun HttpUrl.hasSameOrigin(other: HttpUrl) =
    scheme == other.scheme && host == other.host && port == other.port

/** How many 401s came before this response in the same call (not counting this one). */
private fun Response.prior401Count(): Int =
    generateSequence(priorResponse) { it.priorResponse }.count { it.code == 401 }
