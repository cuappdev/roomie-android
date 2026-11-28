package com.example.roomie.ui.chores

import com.example.roomie.data.Buddy
import com.example.roomie.data.Capabilities
import com.example.roomie.data.Chore
import com.example.roomie.data.ChoreRules
import com.example.roomie.data.House
import com.example.roomie.data.MemberStatus
import com.example.roomie.data.RoomieState
import com.example.roomie.data.Roommate
import com.example.roomie.data.Rotation
import com.example.roomie.data.RotationPlan
import com.example.roomie.data.Session
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The chores screens' UI state, built and changed outside Compose. */
class ChoresUiStateTest {
    // Thursday; the week runs Mon Nov 16 – Sun Nov 22.
    private val today = LocalDate.of(2026, 11, 19)
    private val week = LocalDate.of(2026, 11, 16)

    private fun roommate(id: String) = Roommate(
        id,
        id,
        id.replaceFirstChar {
            it.uppercase()
        },
        buddy = Buddy.Mug,
        status = MemberStatus.Joined,
    )

    private val members = listOf("mina", "leah", "mihili").map(::roommate)
    private val houseOrder = members.map { it.id }

    private fun state(
        chores: List<Chore> = emptyList(),
        rotations: List<Rotation> = emptyList(),
        capabilities: Capabilities = Capabilities(),
    ) = RoomieState(
        session = Session("leah", "leah", "Leah", "", Buddy.Mug, hasHouse = true),
        house = House("Loft", "LOFT-1234", members),
        chores = chores,
        rotations = rotations,
        capabilities = capabilities,
    )

    private val recycling = Rotation("r", "Recycling", DayOfWeek.THURSDAY, listOf(RotationPlan(week, houseOrder, 1)))

    @Test
    fun toggledPeopleKeepHouseOrder() {
        // Ticking Mihili, then Mina, still lists Mina first.
        val picked = ChoreRules.toggleMember(
            houseOrder,
            ChoreRules.toggleMember(houseOrder, emptyList(), "mihili", true),
            "mina",
            true,
        )
        assertEquals(listOf("mina", "mihili"), picked)
        assertEquals(listOf("mihili"), ChoreRules.toggleMember(houseOrder, picked, "mina", false))
    }

    @Test
    fun mineOnlyShowsJustMyChores() {
        val s = state(
            chores = listOf(
                Chore("a", "Dishes", listOf("leah"), today),
                Chore("b", "Vacuum", listOf("mina"), today),
            ),
        )
        assertEquals(
            listOf("a"),
            choresUiState(s, week, mineOnly = true, form = null, today)!!.chores.map {
                it.chore.id
            },
        )
        assertEquals(2, choresUiState(s, week, mineOnly = false, form = null, today)!!.chores.size)
    }

    @Test
    fun noHouseMeansNothingToShow() {
        assertNull(choresUiState(RoomieState(), week, mineOnly = true, form = null, today))
    }

    @Test
    fun pickingALaterDueDateMovesTheStartingWeek() {
        val form = newAddChoreForm(
            state(),
            today,
        ).reduce(AddChoreEvent.DuePicked(week.plusWeeks(2).plusDays(3)), houseOrder)
        assertEquals(week.plusWeeks(2), form.startWeek)
        // An earlier date leaves it alone.
        assertEquals(week.plusWeeks(2), form.reduce(AddChoreEvent.DuePicked(today), houseOrder).startWeek)
    }

    @Test
    fun removingPeopleClampsPeoplePerWeek() {
        var form = newAddChoreForm(state(), today).reduce(AddChoreEvent.RotateChanged(true), houseOrder)
        form =
            form.reduce(
                AddChoreEvent.EveryoneToggled(true),
                houseOrder,
            ).reduce(AddChoreEvent.PerWeekPicked(3), houseOrder)
        form = form.reduce(AddChoreEvent.PersonToggled("leah", false), houseOrder)
        assertEquals(listOf("mina", "mihili"), form.rotatePeople)
        assertEquals(2, form.perWeek)
    }

    @Test
    fun assignOnceStartsWithMe() {
        val form = newAddChoreForm(state(), today)
        assertFalse(form.rotate)
        assertEquals(listOf("leah"), form.oncePeople)
    }

    @Test
    fun addChoreSheetFollowsBackendCapabilities() {
        val s = state(capabilities = Capabilities.Backend)
        val form = newAddChoreForm(s, today)
        assertTrue("Without one-time chores the sheet only rotates", form.rotate)
        val ui = addChoreUiState(form.copy(rotatePeople = houseOrder), s, members, today)
        assertFalse(ui.showModeSwitch)
        assertFalse(ui.showPerWeek)
        assertEquals(listOf(1), ui.perWeekOptions)
    }

    @Test
    fun canCreateOnceTitleDateAndPeopleAreSet() {
        val s = state()
        val blank = newAddChoreForm(s, today)
        assertFalse(addChoreUiState(blank, s, members, today).canCreate)
        val filled = blank.reduce(
            AddChoreEvent.TitleChanged("Dishes"),
            houseOrder,
        ).reduce(AddChoreEvent.DuePicked(today), houseOrder)
        assertTrue(addChoreUiState(filled, s, members, today).canCreate)
    }

    @Test
    fun rotationPreviewShowsTheNextThreeWeeks() {
        val s = state()
        val form = newAddChoreForm(s, today).copy(rotate = true, rotatePeople = houseOrder, due = today)
        val preview = addChoreUiState(form, s, members, today).preview
        assertEquals(listOf(week, week.plusWeeks(1), week.plusWeeks(2)), preview.map { it.first })
        assertEquals(listOf("mina", "leah", "mihili"), preview.map { it.second.single().id })
    }

    @Test
    fun unfinishedRotationTurnStaysWithItsPeople() {
        val s = state(rotations = listOf(recycling))
        val id = ChoreRules.rotationChoreId("r", week)
        val ui = choreDetailUiState(s, id, today)!!
        assertTrue(ui.isRotation)
        assertTrue(ui.showStaysWithNote)
        // Next week's turn isn't "this week's assignment" yet.
        assertFalse(
            choreDetailUiState(s, ChoreRules.rotationChoreId("r", week.plusWeeks(1)), today)!!.showStaysWithNote,
        )
    }

    @Test
    fun manageRotationsShowsWhoIsNext() {
        val summary = manageRotationsUiState(state(rotations = listOf(recycling)), today).rotations.single()
        assertEquals(listOf("Mina", "Leah", "Mihili"), summary.memberNames)
        assertEquals(listOf("Leah"), summary.nextNames)
        assertEquals(LocalDate.of(2026, 11, 26), summary.nextDue)
    }

    @Test
    fun editRotationStartsFromNextWeekOrTheBackendsNextTurn() {
        val local = editRotationUiState(state(rotations = listOf(recycling)), "r", null, today)!!
        assertEquals(week.plusWeeks(1), local.draft.fromWeek)
        assertTrue(local.showStartWeek)

        val backend = editRotationUiState(
            state(rotations = listOf(recycling), capabilities = Capabilities.Backend),
            "r",
            null,
            today,
        )!!
        assertEquals(recycling.startWeek, backend.draft.fromWeek)
        assertFalse(backend.showStartWeek)
    }

    @Test
    fun editRotationCantSaveWithNobody() {
        val ui = editRotationUiState(state(rotations = listOf(recycling)), "r", null, today)!!
        val empty = ui.draft.reduce(EditRotationEvent.EveryoneToggled(false), houseOrder)
        val after = editRotationUiState(state(rotations = listOf(recycling)), "r", empty, today)!!
        assertFalse(after.canSave)
        assertTrue(after.preview.isEmpty())
    }

    @Test
    fun deletedRotationClosesTheEditor() {
        assertNull(editRotationUiState(state(), "r", null, today))
    }
}
