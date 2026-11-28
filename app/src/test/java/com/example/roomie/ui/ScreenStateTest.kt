package com.example.roomie.ui

import com.example.roomie.data.Buddy
import com.example.roomie.data.House
import com.example.roomie.data.MemberStatus
import com.example.roomie.data.Note
import com.example.roomie.data.NoteColor
import com.example.roomie.data.NoteRules
import com.example.roomie.data.RoomieState
import com.example.roomie.data.Roommate
import com.example.roomie.data.Session
import com.example.roomie.data.ShoppingItem
import com.example.roomie.data.Tack
import com.example.roomie.ui.home.Greeting
import com.example.roomie.ui.home.greetingFor
import com.example.roomie.ui.home.homeUiState
import com.example.roomie.ui.notes.BoardSelection
import com.example.roomie.ui.notes.LeaveNoteEvent
import com.example.roomie.ui.notes.LeaveNoteForm
import com.example.roomie.ui.notes.noteBoardUiState
import com.example.roomie.ui.notes.reduce
import com.example.roomie.ui.onboarding.roommatesOf
import com.example.roomie.ui.onboarding.setUpHouseUiState
import com.example.roomie.ui.profile.meUiState
import com.example.roomie.ui.shopping.EditItemEvent
import com.example.roomie.ui.shopping.ItemDraft
import com.example.roomie.ui.shopping.ItemSubtitle
import com.example.roomie.ui.shopping.MatchItem
import com.example.roomie.ui.shopping.Person
import com.example.roomie.ui.shopping.reduce
import com.example.roomie.ui.shopping.shoppingUiState
import com.example.roomie.ui.shopping.subtitleFor
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Home, Note Board, Shopping, Me and onboarding UI states, built outside Compose. */
class ScreenStateTest {
    private val now = LocalDateTime.of(2026, 11, 19, 9, 30)
    private val today = now.toLocalDate()

    private fun roommate(id: String, status: MemberStatus = MemberStatus.Joined) =
        Roommate(id, id, id.replaceFirstChar { it.uppercase() }, buddy = Buddy.Mug, status = status)

    private fun note(id: String, by: String, daysAgo: Long = 0, due: LocalDate? = null, pinned: Boolean = false) = Note(
        id,
        "Note $id",
        by,
        now.minusDays(daysAgo),
        due,
        if (pinned) today.plusDays(3) else null,
        NoteColor.Yellow,
        Tack.DotRed,
    )

    private fun item(id: String, by: String = "mina", daysAgo: Long = 0, boughtBy: String? = null, note: String = "") =
        ShoppingItem(
            id = id,
            name = "Item $id",
            note = note,
            addedById = by,
            addedAt = now.minusDays(daysAgo),
            boughtById = boughtBy,
            boughtAt = boughtBy?.let { now.minusHours(1) },
        )

    private fun state(notes: List<Note> = emptyList(), shopping: List<ShoppingItem> = emptyList()) = RoomieState(
        session = Session("leah", "leah", "Leah", "Kim", Buddy.Plant, hasHouse = true),
        house = House("Loft", "LOFT-1234", listOf(roommate("leah", MemberStatus.Owner), roommate("mina"))),
        notes = notes,
        shopping = shopping,
    )

    @Test
    fun greetingFollowsTheTimeOfDay() {
        assertEquals(Greeting.Morning, greetingFor(now))
        assertEquals(Greeting.Afternoon, greetingFor(now.withHour(13)))
        assertEquals(Greeting.Evening, greetingFor(now.withHour(22)))
    }

    @Test
    fun homeShowsComingUpAndOpensANote() {
        val s = state(notes = listOf(note("a", "mina", due = today.plusDays(2)), note("b", "gone")))
        val ui = homeUiState(s, openNoteId = "b", today = today, now = now)!!
        assertEquals(listOf("a"), ui.comingUp.map { it.note.id })
        // The author left the house, so the screen shows "Someone".
        assertNull(ui.openNote!!.item.authorName)
        assertFalse(ui.openNote!!.item.isMine)
    }

    @Test
    fun boardHidesOldNotesUntilAsked() {
        val old = note("old", "mina", daysAgo = NoteRules.OLDER_AFTER_DAYS + 1)
        val s = state(notes = listOf(note("new", "leah"), old))
        val board = noteBoardUiState(s, BoardSelection(), today)
        assertEquals(listOf("new"), board.notes.map { it.note.id })
        assertEquals(1, board.olderCount)
        assertTrue(board.notes.single().isMine)
        assertEquals(
            listOf("old"),
            noteBoardUiState(s, BoardSelection(showingOlder = true), today).notes.map {
                it.note.id
            },
        )
    }

    @Test
    fun leaveNoteCapsLengthAndKnowsWhenPinsAreFull() {
        val long = "x".repeat(NoteRules.MAX_LENGTH + 10)
        assertEquals(NoteRules.MAX_LENGTH, LeaveNoteForm().reduce(LeaveNoteEvent.TextChanged(long)).text.length)
        val pinned = (1..NoteRules.MAX_PINS).map { note("p$it", "mina", pinned = true) }
        val composing = noteBoardUiState(
            state(notes = pinned),
            BoardSelection(form = LeaveNoteForm(text = "Hi")),
            today,
        ).composing!!
        assertTrue(composing.pinsFull)
        assertTrue(composing.canPost)
    }

    @Test
    fun shoppingSubtitleSaysWhoAndWhen() {
        val s = state()
        assertEquals(ItemSubtitle.Added(Person.Named("Mina"), null), subtitleFor(s, item("a"), now))
        val old = item("b", daysAgo = 3)
        assertEquals(ItemSubtitle.Added(Person.Named("Mina"), old.addedAt), subtitleFor(s, old, now))
        assertEquals(
            ItemSubtitle.WithNote(Person.Me, "blue one"),
            subtitleFor(s, item("c", by = "leah", note = "blue one"), now),
        )
        assertEquals(
            ItemSubtitle.CouldntFind(Person.Unknown),
            subtitleFor(s, item("d").copy(couldntFindById = "gone"), now),
        )
    }

    @Test
    fun typingFindsWhatsOnTheListAndWhatWasBought() {
        val s = state(shopping = listOf(item("1"), item("2", boughtBy = "leah")))
        val ui = shoppingUiState(s, "item", null, null, now)!!
        assertEquals(listOf("1"), ui.needed.map { it.item.id })
        assertTrue(ui.matches.first() is MatchItem.OnList)
        val recent = ui.matches.filterIsInstance<MatchItem.RecentlyBought>()
        // "Item 2" isn't needed again yet, so it can be added again.
        assertEquals(Person.Me, recent.single().by)
    }

    @Test
    fun editDraftKeepsQuantityAtLeastOne() {
        val draft = ItemDraft("1", "Milk", 1, "")
        assertEquals(1, draft.reduce(EditItemEvent.Decrease).quantity)
        assertEquals(2, draft.reduce(EditItemEvent.Increase).quantity)
    }

    @Test
    fun meJoinsFirstAndLastName() {
        assertEquals("Leah Kim", meUiState(state())!!.fullName)
        assertNull(meUiState(RoomieState()))
    }

    @Test
    fun onboardingListsOtherRoommatesAndWhetherAnyJoined() {
        assertEquals(listOf("mina"), roommatesOf(state())!!.map { it.id })
        val ui = setUpHouseUiState(state(), typedName = "  ")!!
        assertTrue(ui.hasRoommates)
        assertFalse("A blank name can't finish", ui.canFinish)
        assertEquals("Loft", setUpHouseUiState(state(), null)!!.name)
    }
}
