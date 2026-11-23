package com.example.roomie.data.api

import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** Where [RetrofitInstance] gets the token it sends as `Authorization: Bearer <token>`. */
fun interface TokenProvider {
    /** Returns the token, or null when signed out. [forceRefresh] is true after a 401. */
    suspend fun token(forceRefresh: Boolean): String?

    /**
     * Changes whenever the signed-in account changes. [RetrofitInstance] won't retry a request
     * after a 401 if the session changed since the request started. Providers with only one
     * account can leave this at 0.
     */
    val session: Long get() = 0L
}

/**
 * Signs in as [netId] with the `dev:<netid>` tokens that the local backend and the mock server
 * accept. The deployed server rejects them, so it needs a Firebase ID token instead.
 * [NetworkModule] binds this as the app's [TokenProvider]; swap the binding once Google sign-in exists.
 */
@Singleton
class DevTokenProvider @Inject constructor() : TokenProvider {
    private val generation = AtomicLong()

    @Volatile
    var netId: String? = null
        set(value) {
            field = value
            generation.incrementAndGet()
        }

    override val session: Long get() = generation.get()

    override suspend fun token(forceRefresh: Boolean): String? = netId?.let { "dev:$it" }
}
