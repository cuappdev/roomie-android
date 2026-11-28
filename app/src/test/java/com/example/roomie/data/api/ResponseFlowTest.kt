package com.example.roomie.data.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ResponseFlowTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Test
    fun olderFetchCannotOverwriteNewerOne() = runBlocking {
        val flow = ResponseFlow<String>(scope)
        val january = CompletableDeferred<String>()

        flow.fetch { january.await() }
        flow.fetch { "february" }.join()
        january.complete("january")

        assertEquals(RoomieApiResponse.Success("february"), flow.state.value)
    }

    @Test
    fun setDiscardsFetchInFlight() = runBlocking {
        val flow = ResponseFlow<String>(scope)
        val house = CompletableDeferred<String>()

        val fetch = flow.fetch { house.await() }
        flow.set(RoomieApiResponse.Pending)
        house.complete("departed house")
        fetch.join()

        assertEquals(RoomieApiResponse.Pending, flow.state.value)
    }

    @Test
    fun keyedFetchesOnlyReplaceTheirOwnKey() = runBlocking {
        val weeks = KeyedResponseFlow<Int, String>(scope)
        val slowFirst = CompletableDeferred<String>()

        weeks.fetch(1) { slowFirst.await() }
        weeks.fetch(1) { "first, again" }.join()
        weeks.fetch(2) { "second" }.join()
        slowFirst.complete("first, stale")

        assertEquals(
            mapOf(1 to RoomieApiResponse.Success("first, again"), 2 to RoomieApiResponse.Success("second")),
            weeks.state.value,
        )
    }

    @Test
    fun clearDiscardsKeyedFetchesInFlight() = runBlocking {
        val weeks = KeyedResponseFlow<Int, String>(scope)
        val slow = CompletableDeferred<String>()

        val fetch = weeks.fetch(1) { slow.await() }
        weeks.clear()
        slow.complete("from before sign-out")
        fetch.join()

        assertEquals(emptyMap<Int, RoomieApiResponse<String>>(), weeks.state.value)
    }

    @Test
    fun concurrentUpdatesAreNotLost() = runBlocking {
        val flow = ResponseFlow<Int>(scope)
        flow.set(RoomieApiResponse.Success(0))

        (1..1000).map { async(Dispatchers.Default) { flow.update { r -> r.map { it + 1 } } } }.awaitAll()

        assertEquals(RoomieApiResponse.Success(1000), flow.state.value)
    }
}
