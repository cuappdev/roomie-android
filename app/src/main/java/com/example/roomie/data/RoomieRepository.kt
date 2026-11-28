package com.example.roomie.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Everything the screens read and do, as one [RoomieState]. Two implementations:
 * [FakeRoomieRepository] (in memory, seeded demo data) and [RemoteRoomieRepository] (the Roomie
 * backend). `roomie.fakeData` in `local.properties` picks between them; see `RepositoryModule`.
 *
 * Calls that return nothing apply their change as soon as the data source confirms it. If one
 * fails, a one-line explanation arrives on [messages] instead.
 */
interface RoomieRepository {

    sealed interface SignInResult {
        data class Invalid(val message: String) : SignInResult
        data class Success(val hasHouse: Boolean) : SignInResult
    }

    val state: StateFlow<RoomieState>

    /** Problems to show the user, e.g. "Couldn't reach the server." Each is shown once. */
    val messages: Flow<String>

    fun today(): LocalDate
    fun now(): LocalDateTime

    /** Reloads everything from the data source, e.g. when the app comes back to the foreground. */
    fun refresh() {}

    // Session and onboarding

    fun isValidNetId(netId: String): Boolean
    suspend fun signIn(netIdInput: String, password: String): SignInResult
    fun signOut()
    fun setBuddy(buddy: Buddy)
    /** Starts a draft house owned by you; it's named on the last onboarding step. */
    fun startHouse()
    /** Returns an error message, or null once you've joined. */
    suspend fun joinHouse(code: String): String?
    /** Returns an error message, or null when the invite was sent (or queued in the draft). */
    suspend fun inviteRoommate(netIdInput: String): String?
    fun removeRoommate(id: String)
    fun renameHouse(name: String)
    /** Creates the draft house, if there is one. Returns an error message, or null on success. */
    suspend fun finishOnboarding(name: String): String?
    fun leaveHouse()

    // Notes

    fun addNote(text: String, dueDate: LocalDate?, pin: Boolean)
    fun updateNote(id: String, text: String, dueDate: LocalDate?)
    fun deleteNote(id: String)
    fun togglePin(id: String)

    // Chores

    /** Loads the week starting [weekStart] (a Monday) if the data source fetches by week. */
    fun showWeek(weekStart: LocalDate) {}
    fun addChore(title: String, assigneeIds: List<String>, dueDate: LocalDate)
    fun addRotation(title: String, dueDay: DayOfWeek, memberIds: List<String>, peoplePerWeek: Int, startWeek: LocalDate)
    fun toggleChoreDone(id: String)
    fun deleteChore(id: String)
    fun updateRotation(id: String, memberIds: List<String>, peoplePerWeek: Int, fromWeek: LocalDate)
    fun deleteRotation(id: String)

    // Shopping

    fun addItem(name: String)
    fun incrementQuantity(id: String)
    fun markBought(id: String)
    fun undoBought(id: String)
    fun toggleCouldntFind(id: String)
    fun saveItem(id: String, name: String, quantity: Int, note: String)
    fun deleteItem(id: String)
}
