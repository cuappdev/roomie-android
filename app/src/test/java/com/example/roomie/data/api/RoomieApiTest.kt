package com.example.roomie.data.api

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class RoomieApiTest {

    private val server = MockWebServer()
    private var refreshes = 0
    private val tokens = TokenProvider { forceRefresh ->
        if (forceRefresh) "fresh-${++refreshes}" else "dev:mnh49"
    }
    private lateinit var choreApi: ChoreApiService
    private lateinit var houseApi: HouseApiService
    private lateinit var shoppingApi: ShoppingApiService

    @Before
    fun setUp() {
        server.start()
        val retrofit = roomieRetrofit(server.url("/").toString(), tokens, logBodies = false)
        choreApi = retrofit.create(ChoreApiService::class.java)
        houseApi = retrofit.create(HouseApiService::class.java)
        shoppingApi = retrofit.create(ShoppingApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun sendsTokenAndParsesDatesAndEnums() = runBlocking {
        server.enqueue(json(200, """
            {"assignments":[{"id":7,"chore":{"id":1,"title":"Trash","frequency":"EVERY_OTHER_DAY"},
              "dueOn":"2026-10-28","status":"DUE_TODAY",
              "assignee":{"id":3,"netid":"mnh49","name":"Mihili","avatarUrl":null,"presence":"AWAY"},
              "completedAt":"2026-10-28T14:05:00.000Z","completedBy":null,"canSwap":false}]}
        """))

        val today = LocalDate.of(2026, 10, 28)
        val turn = apiCall {
            choreApi.getAssignments(1, today, today, assignee = "me", includeOverdue = true)
        }.dataOrNull()!!.assignments.single()

        val request = server.takeRequest()
        assertEquals("Bearer dev:mnh49", request.getHeader("Authorization"))
        assertEquals(
            "/houses/1/chore-assignments?from=2026-10-28&to=2026-10-28&assignee=me&includeOverdue=true",
            request.path,
        )
        assertEquals(today, turn.dueOn)
        assertEquals(TurnStatus.DUE_TODAY, turn.status)
        assertEquals(ChoreFrequency.EVERY_OTHER_DAY, turn.chore.frequency)
        assertEquals(Instant.parse("2026-10-28T14:05:00Z"), turn.completedAt)
        assertNull(turn.completedBy)
        assertEquals(Presence.AWAY, turn.assignee.presence)
    }

    @Test
    fun retriesOnceWithRefreshedTokenAfter401() = runBlocking {
        server.enqueue(json(401, """{"status":"fail","statusCode":401,"message":"Expired","errorCode":"UNAUTHORIZED"}"""))
        server.enqueue(MockResponse().setResponseCode(204))

        val result = apiCall { houseApi.leaveHouse(1) }

        assertEquals(RoomieApiResponse.Success(Unit), result)
        assertEquals("Bearer dev:mnh49", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer fresh-1", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun givesUpAfterSecond401() = runBlocking {
        repeat(2) { server.enqueue(json(401, """{"errorCode":"UNAUTHORIZED","message":"Bad token"}""")) }

        val error = (apiCall { houseApi.leaveHouse(1) } as RoomieApiResponse.Error).error

        assertEquals(ApiErrorCode.UNAUTHORIZED, error.code)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun stillRefreshesWhenARedirectCameBeforeThe401() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(307).setHeader("Location", "/houses/1/leave"))
        server.enqueue(json(401, """{"errorCode":"UNAUTHORIZED","message":"Expired"}"""))
        server.enqueue(MockResponse().setResponseCode(204))

        val result = apiCall { houseApi.leaveHouse(1) }

        assertEquals(RoomieApiResponse.Success(Unit), result)
        assertEquals(3, server.requestCount)
        server.takeRequest()
        server.takeRequest()
        assertEquals("Bearer fresh-1", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun doesNotRetryAsAnotherAccount() = runBlocking {
        // Signs in as someone else between sending the request and getting its 401.
        val switching = object : TokenProvider {
            @Volatile var current = 1L
            override val session get() = current
            override suspend fun token(forceRefresh: Boolean) = "dev:user$current"
        }
        val api = roomieRetrofit(server.url("/").toString(), switching, logBodies = false)
            .create(HouseApiService::class.java)
        server.enqueue(json(401, """{"errorCode":"UNAUTHORIZED","message":"Expired"}""").apply {
            // MockWebServer can't run code mid-call, so switch before it answers.
            setHeadersDelay(300, java.util.concurrent.TimeUnit.MILLISECONDS)
        })
        server.enqueue(MockResponse().setResponseCode(204))

        val call = async(kotlinx.coroutines.Dispatchers.IO) { apiCall { api.leaveHouse(1) } }
        server.takeRequest()
        switching.current = 2L
        val error = (call.await() as RoomieApiResponse.Error).error

        assertEquals(ApiErrorCode.UNAUTHORIZED, error.code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun parsesValidationErrors() = runBlocking {
        server.enqueue(json(400, """
            {"status":"fail","statusCode":400,"message":"Request validation failed",
             "errorCode":"VALIDATION_ERROR","data":{"body.name":"Too small"}}
        """))

        val error = (apiCall { shoppingApi.addItem(1, AddShoppingItemBody("")) } as RoomieApiResponse.Error).error

        assertEquals(ApiErrorCode.VALIDATION_ERROR, error.code)
        assertEquals(400, error.statusCode)
        assertEquals("Request validation failed", error.message)
        assertEquals(mapOf("body.name" to "Too small"), error.fieldErrors)
    }

    @Test
    fun fallsBackToStatusWhenBodyIsNotJson() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(409).setBody("Conflict"))

        val error = (apiCall { houseApi.joinHouse(JoinHouseBody("LOFT-4827")) } as RoomieApiResponse.Error).error

        assertEquals(ApiErrorCode.CONFLICT, error.code)
    }

    @Test
    fun leavesUnsetFieldsOutOfRequestBodies() = runBlocking {
        server.enqueue(json(200, """{"item":null}"""))

        apiCall { shoppingApi.updateItem(1, 2, UpdateShoppingItemBody(priceCents = 429)) }

        assertEquals("""{"priceCents":429}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun clearsANotesDueDateWithAnExplicitNull() = runBlocking {
        server.enqueue(json(200, """{"note":null}"""))
        val noteApi = roomieRetrofit(server.url("/").toString(), tokens, logBodies = false).create(NoteApiService::class.java)

        apiCall { noteApi.updateNote(1, 2, updateNoteBody("Rent", dueOn = null)) }

        assertEquals("""{"body":"Rent","dueOn":null}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun reportsNetworkFailures() = runBlocking {
        server.shutdown()

        val error = (apiCall { houseApi.getHouse(1) } as RoomieApiResponse.Error).error

        assertEquals(ApiErrorCode.NETWORK, error.code)
        assertTrue(error.statusCode == null)
    }

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body.trimIndent())
}
