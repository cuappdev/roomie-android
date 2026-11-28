package com.example.roomie.data.api

import com.example.roomie.data.ChoreRules
import com.example.roomie.data.NoteColor
import com.example.roomie.data.RemoteMapping
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Runs the app's real client against a running backend, to catch DTOs that drift from the API.
 * Skipped unless `ROOMIE_E2E_URL` is set, e.g.
 * `ROOMIE_E2E_URL=http://localhost:8000/ ./gradlew :app:testDebugUnitTest --tests '*LiveBackendTest*'`.
 * The backend needs `AUTH_DEV_BYPASS=true`. Each run signs up two new NetIDs.
 */
class LiveBackendTest {

    private val baseUrl: String? = System.getenv("ROOMIE_E2E_URL")

    /** `zz123456`-style NetIDs, new each run so runs don't collide. */
    private val run = (100_000..999_999).random()
    private val alice = "za$run"
    private val bob = "zb$run"

    private fun client(netId: String): RetrofitInstance {
        val tokens = TokenProvider { "dev:$netId" }
        return RetrofitInstance(roomieRetrofit(baseUrl!!, tokens, logBodies = false))
    }

    private fun <T> RoomieApiResponse<T>.ok(step: String): T {
        assertTrue("$step failed: $this", this is RoomieApiResponse.Success)
        return (this as RoomieApiResponse.Success).data
    }

    @Test
    fun wholeFlowParses() = runBlocking {
        assumeTrue("Set ROOMIE_E2E_URL to run", baseUrl != null)
        val a = client(alice)
        val b = client(bob)

        // Sign up both; the login house is a summary.
        val aliceLogin = apiCall { a.userApi.login() }.ok("login A")
        assertNull(aliceLogin.house)
        apiCall { b.userApi.login() }.ok("login B")

        // Alice makes a house and invites Bob, who accepts.
        val house = apiCall { a.houseApi.createHouse(CreateHouseBody("E2E House $run", inviteNetids = listOf(bob))) }.ok("create house").house
        assertEquals(listOf(bob), house.pendingInvites.map { it.netid })
        val invite = apiCall { b.userApi.getMe() }.ok("me B").pendingInvites.single()
        apiCall { b.houseApi.acceptInvite(invite.id) }.ok("accept")
        val me = apiCall { a.userApi.getMe() }.ok("me A")
        assertEquals(house.id, me.house?.id)
        val full = apiCall { a.houseApi.getHouse(house.id) }.ok("get house").house
        assertEquals(2, full.members.size)
        assertNotNull(full.color)
        val uiHouse = RemoteMapping.house(full, me.user.id, RemoteMapping.buddyFor(me.user.id))
        assertEquals(2, uiHouse.members.size)

        // Presence round-trips.
        val away = apiCall { a.userApi.updateMe(UpdateMeBody(presence = Presence.AWAY)) }.ok("presence")
        assertEquals(Presence.AWAY, away.user.presence)

        // A weekly chore rotating Alice, Bob; its turns map onto the app's chores.
        val zone = ZoneId.of(full.timezone)
        val today = LocalDate.now(zone)
        val week = ChoreRules.weekStart(today)
        val ids = full.members.map { it.id }
        val startsOn = RemoteMapping.firstTurn(week, today.dayOfWeek, today)
        val chore = apiCall {
            a.choreApi.createChore(house.id, CreateChoreBody("Trash", ChoreFrequency.WEEKLY, startsOn, ids))
        }.ok("create chore").chore
        val rotation = RemoteMapping.rotation(chore)
        assertEquals(chore.comingUp.first().assignee.id.toString(), rotation.assigneesFor(ChoreRules.weekStart(chore.comingUp.first().dueOn)).single())
        val turns = apiCall {
            a.choreApi.getAssignments(house.id, week, week.plusDays(6), includeOverdue = true)
        }.ok("week").assignments
        val turn = turns.single { it.chore.id == chore.id }
        assertEquals(today, turn.dueOn)
        val done = apiCall { a.choreApi.completeAssignment(house.id, turn.id) }.ok("complete").assignment
        assertEquals(today, RemoteMapping.chore(done, zone).completedOn)
        apiCall { a.choreApi.uncompleteAssignment(house.id, turn.id) }.ok("uncomplete")
        // Edit the rotation, reversed, as updateRotation does.
        apiCall { a.choreApi.updateChore(house.id, chore.id, UpdateChoreBody(rotation = ids.reversed())) }.ok("edit rotation")

        // Notes: due date, pin, clearing the due date with an explicit null.
        val note = apiCall {
            a.noteApi.createNote(house.id, CreateNoteBody("Rent due", dueOn = today.plusDays(3), pinned = true))
        }.ok("create note").note
        assertTrue(note.pinned)
        val board = apiCall { b.noteApi.getNotes(house.id) }.ok("board")
        assertEquals(listOf(note.id), board.pinned.map { it.id })
        val cleared = apiCall { a.noteApi.updateNote(house.id, note.id, updateNoteBody("Rent paid", dueOn = null)) }.ok("edit note").note
        assertNull(cleared.dueOn)
        assertEquals("Rent paid", cleared.body)
        apiCall { b.noteApi.unpinNote(house.id, note.id) }.ok("unpin")
        val mapped = RemoteMapping.note(cleared, zone)
        assertTrue(mapped.color in NoteColor.entries)
        apiCall { a.noteApi.deleteNote(house.id, note.id) }.ok("delete note")

        // Shopping, with the quantity stored in the name.
        val item = apiCall {
            b.shoppingApi.addItem(house.id, AddShoppingItemBody(RemoteMapping.encodeItemName("Eggs", 2)))
        }.ok("add item").item
        assertEquals("Eggs" to 2, RemoteMapping.decodeItemName(item.name))
        apiCall { a.shoppingApi.buyItem(house.id, item.id, BuyItemBody()) }.ok("buy")
        val list = apiCall { a.shoppingApi.getShoppingList(house.id) }.ok("list")
        assertEquals(listOf(item.id), list.bought.map { it.id })
        assertEquals(alice, list.bought.single().boughtBy?.netid)
        apiCall { a.shoppingApi.unbuyItem(house.id, item.id) }.ok("unbuy")

        // Both leave; the last one out deletes the house.
        apiCall { b.houseApi.leaveHouse(house.id) }.ok("leave B")
        apiCall { a.houseApi.leaveHouse(house.id) }.ok("leave A")
        assertNull(apiCall { a.userApi.getMe() }.ok("me after").house)
    }
}
