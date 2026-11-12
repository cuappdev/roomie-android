package com.example.roomie.data.api

import com.example.roomie.BuildConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Retrofit client every Roomie API service shares. It adds `Authorization: Bearer <token>`
 * to each request and, on a 401, asks [TokenProvider] for a fresh token and retries once.
 */
@Singleton
class RetrofitInstance internal constructor(retrofit: Retrofit) {

    @Inject
    constructor(tokenProvider: TokenProvider) :
        this(roomieRetrofit(BuildConfig.API_URL, tokenProvider, logBodies = BuildConfig.DEBUG))

    val userApi: UserApiService by lazy { retrofit.create(UserApiService::class.java) }
    val houseApi: HouseApiService by lazy { retrofit.create(HouseApiService::class.java) }
    val choreApi: ChoreApiService by lazy { retrofit.create(ChoreApiService::class.java) }
    val shoppingApi: ShoppingApiService by lazy { retrofit.create(ShoppingApiService::class.java) }
    val noteApi: NoteApiService by lazy { retrofit.create(NoteApiService::class.java) }
}

/** Builds the client. Separate from [RetrofitInstance] so tests can point it at a fake server. */
internal fun roomieRetrofit(
    baseUrl: String,
    tokenProvider: TokenProvider,
    logBodies: Boolean,
): Retrofit {
    val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = runBlocking { tokenProvider.token(forceRefresh = false) }
        if (token == null || request.header("Authorization") != null) {
            return@Interceptor chain.proceed(request)
        }
        chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
    }

    val authenticator = Authenticator { _, response ->
        // Retry once. A second 401 means the fresh token was rejected too.
        if (response.priorResponse != null) return@Authenticator null
        val token = runBlocking { tokenProvider.token(forceRefresh = true) }
            ?: return@Authenticator null
        response.request.newBuilder().header("Authorization", "Bearer $token").build()
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
