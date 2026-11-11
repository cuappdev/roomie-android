package com.example.roomie.data.api

import javax.inject.Inject
import javax.inject.Singleton

/** Where [RetrofitInstance] gets the token it sends as `Authorization: Bearer <token>`. */
fun interface TokenProvider {
    /** Returns the token, or null when signed out. [forceRefresh] is true after a 401. */
    suspend fun token(forceRefresh: Boolean): String?
}

/**
 * Signs in as [netId] with the `dev:<netid>` tokens that the local backend and the mock server
 * accept. The deployed server rejects them, so it needs a Firebase ID token instead.
 * [NetworkModule] binds this as the app's [TokenProvider]; swap the binding once Google sign-in exists.
 */
@Singleton
class DevTokenProvider @Inject constructor() : TokenProvider {
    @Volatile
    var netId: String? = null

    override suspend fun token(forceRefresh: Boolean): String? = netId?.let { "dev:$it" }
}
