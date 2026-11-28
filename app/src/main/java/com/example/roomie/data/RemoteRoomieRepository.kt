package com.example.roomie.data

import android.content.Context
import com.example.roomie.data.api.ApiError
import com.example.roomie.data.api.ApiErrorCode
import com.example.roomie.data.api.ChoreDto
import com.example.roomie.data.api.ChoreFrequency
import com.example.roomie.data.api.CreateChoreBody
import com.example.roomie.data.api.CreateHouseBody
import com.example.roomie.data.api.HouseDto
import com.example.roomie.data.api.MeResponse
import com.example.roomie.data.api.NoteBoardResponse
import com.example.roomie.data.api.RoomieApiResponse
import com.example.roomie.data.api.ShoppingListResponse
import com.example.roomie.data.api.TurnDto
import com.example.roomie.data.api.UpdateChoreBody
import com.example.roomie.data.api.UpdateHouseBody
import com.example.roomie.data.api.UpdateShoppingItemBody
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [RoomieRepository] backed by the Roomie backend. It combines the per-resource repositories
 * ([AuthRepository], [HouseRepository], [ChoreRepository], [NoteRepository], [ShoppingRepository])
 * into one [RoomieState], using [RemoteMapping] to turn DTOs into the app's models.
 *
 * Sign-in uses the backend's dev tokens (`dev:<netid>`), so it works against a local backend or
 * the mock server, not the deployed one. The NetID is remembered so the app stays signed in.
 */
@Singleton
class RemoteRoomieRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val auth: AuthRepository,
    private val houses: HouseRepository,
    private val chores: ChoreRepository,
    private val notes: NoteRepository,
    private val shopping: ShoppingRepository,
) : RoomieRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prefs = context.getSharedPreferences("roomie_session", Context.MODE_PRIVATE)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val messages: Flow<String> = _messages.asSharedFlow()

    /** Onboarding's "Start a House" until its last step creates the house on the server. */
    private data class Draft(val name: String = "", val invites: List<String> = emptyList())

    /** What only lives on this device. [buddyVersion] changes when a saved buddy does. */
    private data class Local(val draft: Draft? = null, val restoring: Boolean = false, val buddyVersion: Int = 0)

    private val local = MutableStateFlow(Local(restoring = savedNetId != null))

    private class Data(
        val chores: RoomieApiResponse<List<ChoreDto>>,
        val weeks: Map<LocalDate, RoomieApiResponse<List<TurnDto>>>,
        val notes: RoomieApiResponse<NoteBoardResponse>,
        val shopping: RoomieApiResponse<ShoppingListResponse>,
    )

    override val state: StateFlow<RoomieState> = combine(
        combine(auth.me, houses.house, local, ::Triple),
        combine(chores.chores, chores.weeks, notes.noteBoard, shopping.shoppingList, ::Data),
    ) { (me, house, device), data -> build(me, house, device, data) }
        .stateIn(scope, SharingStarted.Eagerly, RoomieState(capabilities = Capabilities.Backend, restoring = local.value.restoring))

    private val houseId: Int? get() = auth.me.value.dataOrNull()?.house?.id

    /** "Today" is the house's, since that's what the server uses for due and overdue. */
    private val houseZone: ZoneId
        get() = houses.house.value.dataOrNull()?.takeIf { it.id == houseId }?.timezone
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()

    init {
        // Load (or forget) the house's data whenever the house you're in changes.
        scope.launch {
            auth.me.map { it.dataOrNull()?.house?.id }.distinctUntilChanged().collect { id ->
                houses.clear()
                chores.clear()
                notes.clear()
                shopping.clear()
                if (id != null) loadHouse(id, refresh = false)
            }
        }
        savedNetId?.let { netId ->
            scope.launch {
                when (val result = auth.devLogin(netId)) {
                    is RoomieApiResponse.Error -> {
                        if (result.error.code == ApiErrorCode.UNAUTHORIZED) forgetSignIn()
                        report(result.error, "Couldn't restore your sign-in.")
                    }
                    else -> Unit
                }
                local.update { it.copy(restoring = false) }
            }
        }
    }

    override fun today(): LocalDate = LocalDate.now(houseZone)

    override fun now(): LocalDateTime = LocalDateTime.now()

    override fun refresh() {
        if (auth.me.value !is RoomieApiResponse.Success) return
        auth.fetchMe(refresh = true)
        houseId?.let { loadHouse(it, refresh = true) }
    }

    // region Session and onboarding

    override fun isValidNetId(netId: String): Boolean = RemoteMapping.isValidNetId(netId)

    /** The backend's dev sign-in has no password, so [password] is only checked for being filled in. */
    override suspend fun signIn(netIdInput: String, password: String): RoomieRepository.SignInResult {
        val netId = RemoteMapping.normalizeNetId(netIdInput)
        return when (val result = auth.devLogin(netId)) {
            is RoomieApiResponse.Success -> {
                prefs.edit().putString(KEY_NET_ID, netId).apply()
                local.update { it.copy(draft = null) }
                RoomieRepository.SignInResult.Success(hasHouse = result.data.house != null)
            }
            is RoomieApiResponse.Error -> {
                auth.signOut()
                RoomieRepository.SignInResult.Invalid(
                    when (result.error.code) {
                        ApiErrorCode.NETWORK -> NETWORK_MESSAGE
                        ApiErrorCode.UNAUTHORIZED -> "The server turned down dev sign-in. Is the app pointed at a local backend?"
                        else -> result.error.message
                    },
                )
            }
            RoomieApiResponse.Pending -> RoomieRepository.SignInResult.Invalid("Try again.")
        }
    }

    override fun signOut() {
        forgetSignIn()
        auth.signOut()
        local.update { it.copy(draft = null) }
    }

    override fun setBuddy(buddy: Buddy) {
        val netId = savedNetId ?: return
        prefs.edit().putString(KEY_BUDDY_PREFIX + netId, buddy.name).apply()
        local.update { it.copy(buddyVersion = it.buddyVersion + 1) }
    }

    override fun startHouse() {
        if (houseId != null) return
        local.update { if (it.draft == null) it.copy(draft = Draft()) else it }
    }

    override suspend fun joinHouse(code: String): String? =
        when (val result = houses.joinHouse(code)) {
            is RoomieApiResponse.Success -> {
                local.update { it.copy(draft = null) }
                auth.fetchMe(refresh = true).join()
                null
            }
            is RoomieApiResponse.Error -> when (result.error.code) {
                ApiErrorCode.NOT_FOUND -> "We couldn't find a house with that code."
                ApiErrorCode.CONFLICT -> "You're already in a house. Leave it first."
                ApiErrorCode.VALIDATION_ERROR -> "That doesn't look like an invite code."
                else -> messageFor(result.error, "Couldn't join the house.")
            }
            RoomieApiResponse.Pending -> null
        }

    override suspend fun inviteRoommate(netIdInput: String): String? {
        val netId = RemoteMapping.normalizeNetId(netIdInput)
        if (!RemoteMapping.isValidNetId(netId)) return "Enter a Cornell NetID, like ab123."
        val house = state.value.house ?: return "Start a house first."
        if (house.members.any { it.netId == netId }) return "$netId is already in the house."
        val id = houseId
        if (id == null) {
            local.update { it.copy(draft = (it.draft ?: Draft()).let { d -> d.copy(invites = d.invites + netId) }) }
            return null
        }
        return when (val result = houses.invite(id, listOf(netId))) {
            is RoomieApiResponse.Error -> messageFor(result.error, "Couldn't send the invite.")
            else -> null
        }
    }

    override fun removeRoommate(id: String) {
        when {
            id.startsWith(RemoteMapping.DRAFT_INVITE_PREFIX) -> {
                val netId = id.removePrefix(RemoteMapping.DRAFT_INVITE_PREFIX)
                local.update { it.copy(draft = it.draft?.let { d -> d.copy(invites = d.invites - netId) }) }
            }
            id.startsWith(RemoteMapping.INVITE_PREFIX) -> {
                val inviteId = id.removePrefix(RemoteMapping.INVITE_PREFIX).toIntOrNull() ?: return
                inHouse("Couldn't cancel the invite.") { houses.cancelInvite(it, inviteId) }
            }
            else -> {
                val userId = id.toIntOrNull() ?: return
                inHouse("Couldn't remove them.") { houses.removeMember(it, userId) }
            }
        }
    }

    /** Onboarding calls this on every keystroke, so a draft is only renamed locally. */
    override fun renameHouse(name: String) {
        val trimmed = name.trim()
        if (houseId == null) {
            local.update { it.copy(draft = it.draft?.copy(name = trimmed)) }
            return
        }
        if (trimmed.isEmpty() || trimmed == state.value.house?.name) return
        inHouse("Couldn't rename the house.") { houses.updateHouse(it, UpdateHouseBody(name = trimmed)) }
    }

    override suspend fun finishOnboarding(name: String): String? {
        val draft = local.value.draft ?: return null
        val body = CreateHouseBody(name = name.trim(), inviteNetids = draft.invites.ifEmpty { null })
        return when (val result = houses.createHouse(body)) {
            is RoomieApiResponse.Success -> {
                local.update { it.copy(draft = null) }
                auth.fetchMe(refresh = true).join()
                null
            }
            is RoomieApiResponse.Error -> when (result.error.code) {
                ApiErrorCode.CONFLICT -> "You're already in a house."
                else -> messageFor(result.error, "Couldn't create the house.")
            }
            RoomieApiResponse.Pending -> null
        }
    }

    override fun leaveHouse() {
        inHouse("Couldn't leave the house.") { id ->
            houses.leaveHouse(id).also { if (it is RoomieApiResponse.Success) auth.fetchMe(refresh = true) }
        }
    }

    // endregion

    // region Notes

    override fun addNote(text: String, dueDate: LocalDate?, pin: Boolean) {
        val trimmed = text.trim().take(NoteRules.MAX_LENGTH)
        if (trimmed.isEmpty()) return
        val canPin = pin && NoteRules.pinsInUse(state.value.notes, today()) < NoteRules.MAX_PINS
        inHouse("Couldn't post the note.") { notes.createNote(it, trimmed, dueDate, canPin) }
    }

    override fun updateNote(id: String, text: String, dueDate: LocalDate?) {
        val noteId = id.toIntOrNull() ?: return
        if (text.isBlank()) return
        inHouse("Couldn't save the note.") { notes.updateNote(it, noteId, text.take(NoteRules.MAX_LENGTH), dueDate) }
    }

    override fun deleteNote(id: String) {
        val noteId = id.toIntOrNull() ?: return
        inHouse("Couldn't delete the note.") { notes.deleteNote(it, noteId) }
    }

    override fun togglePin(id: String) {
        val noteId = id.toIntOrNull() ?: return
        val today = today()
        val note = state.value.notes.firstOrNull { it.id == id } ?: return
        when {
            note.isPinned(today) -> inHouse("Couldn't unpin the note.") { notes.unpinNote(it, noteId) }
            NoteRules.pinsInUse(state.value.notes, today) < NoteRules.MAX_PINS ->
                inHouse("Couldn't pin the note.") { notes.pinNote(it, noteId) }
        }
    }

    // endregion

    // region Chores

    override fun showWeek(weekStart: LocalDate) {
        val id = houseId ?: return
        if (weekStart !in chores.weeks.value) chores.fetchWeek(id, weekStart)
    }

    override fun addChore(title: String, assigneeIds: List<String>, dueDate: LocalDate) {
        _messages.tryEmit("One-time chores aren't available yet. Use Rotate Weekly.")
    }

    override fun addRotation(title: String, dueDay: DayOfWeek, memberIds: List<String>, peoplePerWeek: Int, startWeek: LocalDate) {
        val rotation = memberIds.mapNotNull { it.toIntOrNull() }
        if (title.isBlank() || rotation.isEmpty()) return
        val body = CreateChoreBody(
            title = title.trim(),
            frequency = ChoreFrequency.WEEKLY,
            startsOn = RemoteMapping.firstTurn(startWeek, dueDay, today()),
            rotation = rotation,
        )
        inHouse("Couldn't add the chore.") { chores.createChore(it, body) }
    }

    override fun toggleChoreDone(id: String) {
        val turnId = id.removePrefix(RemoteMapping.TURN_PREFIX).toIntOrNull() ?: return
        val turn = chores.weeks.value.values.firstNotNullOfOrNull { week ->
            week.dataOrNull()?.firstOrNull { it.id == turnId }
        } ?: return
        if (turn.completedAt != null) {
            inHouse("Couldn't undo that.") { chores.uncompleteTurn(it, turnId) }
        } else {
            inHouse("Couldn't mark it done.") { chores.completeTurn(it, turnId) }
        }
    }

    /** Only one-time chores can be deleted on their own, and the backend doesn't have those. */
    override fun deleteChore(id: String) {
        _messages.tryEmit("Delete the rotation to remove this chore.")
    }

    /**
     * The server gives the next turn to whoever is first in the new rotation, so the list is
     * reordered to start with whoever was up next (as [ChoreRules.withPlan] does for the fake).
     * [peoplePerWeek] and [fromWeek] aren't supported (see [Capabilities.Backend]).
     */
    override fun updateRotation(id: String, memberIds: List<String>, peoplePerWeek: Int, fromWeek: LocalDate) {
        val choreId = id.toIntOrNull() ?: return
        val rotation = state.value.rotations.firstOrNull { it.id == id } ?: return
        if (memberIds.isEmpty()) return
        val ordered = ChoreRules.withPlan(rotation, rotation.startWeek, memberIds, 1).plans.last().memberIds
        val body = UpdateChoreBody(rotation = ordered.mapNotNull { it.toIntOrNull() })
        inHouse("Couldn't save the rotation.") { chores.updateChore(it, choreId, body) }
    }

    override fun deleteRotation(id: String) {
        val choreId = id.toIntOrNull() ?: return
        inHouse("Couldn't delete the rotation.") { chores.deleteChore(it, choreId) }
    }

    // endregion

    // region Shopping

    override fun addItem(name: String) {
        if (name.isBlank()) return
        inHouse("Couldn't add the item.") { shopping.addItem(it, RemoteMapping.encodeItemName(name, 1)) }
    }

    override fun incrementQuantity(id: String) {
        val item = state.value.shopping.firstOrNull { it.id == id } ?: return
        rename(item, item.name, item.quantity + 1)
    }

    override fun markBought(id: String) {
        val itemId = id.toIntOrNull() ?: return
        inHouse("Couldn't mark it bought.") { shopping.buyItem(it, itemId) }
    }

    override fun undoBought(id: String) {
        val itemId = id.toIntOrNull() ?: return
        inHouse("Couldn't undo that.") { shopping.unbuyItem(it, itemId) }
    }

    override fun toggleCouldntFind(id: String) {
        _messages.tryEmit("\"Couldn't find it\" isn't available yet.")
    }

    /** [note] isn't saved: the backend has no item notes yet (see [Capabilities.Backend]). */
    override fun saveItem(id: String, name: String, quantity: Int, note: String) {
        val item = state.value.shopping.firstOrNull { it.id == id } ?: return
        rename(item, name.trim().ifEmpty { item.name }, quantity.coerceAtLeast(1))
    }

    override fun deleteItem(id: String) {
        val itemId = id.toIntOrNull() ?: return
        inHouse("Couldn't delete the item.") { shopping.deleteItem(it, itemId) }
    }

    private fun rename(item: ShoppingItem, name: String, quantity: Int) {
        val itemId = item.id.toIntOrNull() ?: return
        val encoded = RemoteMapping.encodeItemName(name, quantity)
        if (encoded == RemoteMapping.encodeItemName(item.name, item.quantity)) return
        inHouse("Couldn't save the item.") { shopping.updateItem(it, itemId, UpdateShoppingItemBody(name = encoded)) }
    }

    // endregion

    // region Plumbing

    private fun loadHouse(id: Int, refresh: Boolean) {
        houses.fetchHouse(id, refresh)
        chores.fetchChores(id, refresh)
        notes.fetchNoteBoard(id, refresh)
        shopping.fetchShoppingList(id, refresh)
        if (refresh) chores.refreshWeeks(id)
        val thisWeek = ChoreRules.weekStart(today())
        if (thisWeek !in chores.weeks.value) chores.fetchWeek(id, thisWeek)
    }

    /** Runs [call] for the current house and reports a failure on [messages]. */
    private fun inHouse(failure: String, call: suspend (houseId: Int) -> RoomieApiResponse<*>) {
        val id = houseId ?: return
        scope.launch {
            val result = call(id)
            if (result is RoomieApiResponse.Error) report(result.error, failure)
        }
    }

    private fun report(error: ApiError, failure: String) {
        _messages.tryEmit(messageFor(error, failure))
    }

    private fun messageFor(error: ApiError, failure: String): String = when (error.code) {
        ApiErrorCode.NETWORK -> NETWORK_MESSAGE
        ApiErrorCode.UNKNOWN -> failure
        ApiErrorCode.VALIDATION_ERROR -> "$failure ${error.fieldErrors.values.firstOrNull() ?: error.message}"
        else -> "$failure ${error.message}"
    }

    private val savedNetId: String? get() = prefs.getString(KEY_NET_ID, null)

    private fun forgetSignIn() {
        prefs.edit().remove(KEY_NET_ID).apply()
    }

    private fun savedBuddy(netId: String): Buddy? =
        prefs.getString(KEY_BUDDY_PREFIX + netId, null)?.let { name -> Buddy.entries.firstOrNull { it.name == name } }

    private fun build(
        me: RoomieApiResponse<MeResponse>,
        house: RoomieApiResponse<HouseDto>,
        local: Local,
        data: Data,
    ): RoomieState {
        val base = RoomieState(capabilities = Capabilities.Backend, restoring = local.restoring)
        val session = me.dataOrNull() ?: return base
        val user = session.user
        val (first, last) = RemoteMapping.splitName(user.name)
        val buddy = savedBuddy(user.netid) ?: RemoteMapping.buddyFor(user.id)
        val houseDto = house.dataOrNull()?.takeIf { it.id == session.house?.id }
        val uiSession = Session(user.id.toString(), user.netid, first, last, buddy, hasHouse = session.house != null)
        val zone = houseDto?.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()

        val uiHouse = when {
            houseDto != null -> RemoteMapping.house(houseDto, user.id, buddy)
            session.house == null && local.draft != null -> RemoteMapping.draftHouse(
                local.draft.name,
                Roommate(user.id.toString(), user.netid, first, last, buddy, MemberStatus.Owner),
                local.draft.invites,
            )
            else -> null
        }
        if (houseDto == null) return base.copy(session = uiSession, house = uiHouse)

        val board = data.notes.dataOrNull()
        val list = data.shopping.dataOrNull()
        return base.copy(
            session = uiSession,
            house = uiHouse,
            notes = board?.let { (it.pinned + it.notes).distinctBy { n -> n.id } }.orEmpty()
                .map { RemoteMapping.note(it, ZoneId.systemDefault()) },
            chores = data.weeks.values.flatMap { it.dataOrNull().orEmpty() }
                .distinctBy { it.id }
                .map { RemoteMapping.chore(it, zone) },
            rotations = data.chores.dataOrNull().orEmpty().map(RemoteMapping::rotation),
            shopping = list?.let { it.needed + it.bought }.orEmpty()
                .map { RemoteMapping.item(it, ZoneId.systemDefault()) },
        )
    }

    // endregion

    private companion object {
        const val KEY_NET_ID = "netId"
        const val KEY_BUDDY_PREFIX = "buddy."
        const val NETWORK_MESSAGE = "Couldn't reach the server. Check your connection and try again."
    }
}
