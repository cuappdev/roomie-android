package com.example.roomie.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * In-memory stand-in for the Roomie backend. Everything lives in one [StateFlow] so screens
 * update instantly, the way they would with a real-time backend. Data resets when the app
 * process dies.
 *
 * Sign in as `lk452` to land in the seeded demo house ("The Linden Loft"). Any other NetID is
 * treated as a new account and goes through onboarding; invite code `LOFT-4827` joins the demo
 * house.
 */
object FakeRoomieRepository {

    sealed interface SignInResult {
        data object Invalid : SignInResult
        data class Success(val hasHouse: Boolean) : SignInResult
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(RoomieState())
    val state: StateFlow<RoomieState> = _state.asStateFlow()

    fun today(): LocalDate = LocalDate.now()
    fun now(): LocalDateTime = LocalDateTime.now()

    val userId: String? get() = _state.value.session?.userId

    // region Session and onboarding

    fun normalizeNetId(input: String): String =
        input.trim().lowercase().removeSuffix("@cornell.edu")

    fun isValidNetId(netId: String): Boolean = NET_ID.matches(normalizeNetId(netId))

    fun signIn(netIdInput: String, password: String): SignInResult {
        val netId = normalizeNetId(netIdInput)
        if (!NET_ID.matches(netId) || password.isBlank()) return SignInResult.Invalid
        if (netId == DEMO_OWNER_NET_ID) {
            _state.value = seedDemoHouse(currentUserNetId = netId)
            return SignInResult.Success(hasHouse = true)
        }
        val known = DIRECTORY[netId]
        _state.value = RoomieState(
            session = Session(
                userId = netId,
                netId = netId,
                firstName = known?.first ?: netId,
                lastName = known?.second ?: "",
                buddy = Buddy.Mug,
                hasHouse = false,
            )
        )
        return SignInResult.Success(hasHouse = false)
    }

    fun signOut() {
        _state.value = RoomieState()
    }

    fun setBuddy(buddy: Buddy) = _state.update { s ->
        val session = s.session ?: return@update s
        s.copy(
            session = session.copy(buddy = buddy),
            house = s.house?.let { h ->
                h.copy(members = h.members.map { if (it.id == session.userId) it.copy(buddy = buddy) else it })
            },
        )
    }

    /** Starts a draft house owned by the current user; it's named on the last onboarding step. */
    fun startHouse() = _state.update { s ->
        val session = s.session ?: return@update s
        if (s.house != null && s.house.members.firstOrNull()?.id == session.userId) return@update s
        s.copy(
            house = House(
                name = "",
                inviteCode = inviteCodeFor(""),
                members = listOf(session.toRoommate(MemberStatus.Owner)),
            ),
            notes = emptyList(),
            chores = emptyList(),
            shopping = emptyList(),
        )
    }

    fun isValidInviteCode(code: String): Boolean = code.trim().uppercase() == DEMO_INVITE_CODE

    fun joinHouse(code: String): Boolean {
        if (!isValidInviteCode(code)) return false
        val session = _state.value.session ?: return false
        val demo = seedDemoHouse(currentUserNetId = DEMO_OWNER_NET_ID)
        val me = session.toRoommate(MemberStatus.Joined)
        _state.value = demo.copy(
            session = session.copy(hasHouse = true),
            house = demo.house!!.copy(members = demo.house.members + me),
        )
        return true
    }

    /** Returns an error message, or null when the invite was sent. */
    fun inviteRoommate(netIdInput: String): String? {
        val netId = normalizeNetId(netIdInput)
        if (!NET_ID.matches(netId)) return "Enter a Cornell NetID, like ab123."
        val house = _state.value.house ?: return "Start a house first."
        if (house.members.any { it.netId == netId }) return "$netId is already in the house."
        val known = DIRECTORY[netId]
        val buddy = Buddy.entries[(house.members.size) % Buddy.entries.size]
        val roommate = Roommate(
            id = netId,
            netId = netId,
            firstName = known?.first ?: netId,
            lastName = known?.second ?: "",
            buddy = known?.third ?: buddy,
            status = MemberStatus.InviteSent,
        )
        _state.update { s -> s.copy(house = s.house?.copy(members = s.house.members + roommate)) }
        // Pretend roommates in the fake Cornell directory accept their email right away.
        if (known != null) {
            scope.launch {
                delay(2_500)
                updateMember(netId) { it.copy(status = MemberStatus.Joined) }
            }
        }
        return null
    }

    fun removeRoommate(id: String) = _state.update { s ->
        s.copy(house = s.house?.copy(members = s.house.members.filterNot { it.id == id }))
    }

    fun renameHouse(name: String) = _state.update { s ->
        s.copy(house = s.house?.copy(name = name.trim(), inviteCode = inviteCodeFor(name, s.house.inviteCode)))
    }

    fun finishOnboarding() = _state.update { s ->
        s.copy(session = s.session?.copy(hasHouse = s.house != null))
    }

    fun leaveHouse() = _state.update { s ->
        s.copy(session = s.session?.copy(hasHouse = false), house = null, notes = emptyList(), chores = emptyList(), shopping = emptyList())
    }

    private fun updateMember(id: String, transform: (Roommate) -> Roommate) = _state.update { s ->
        s.copy(house = s.house?.copy(members = s.house.members.map { if (it.id == id) transform(it) else it }))
    }

    // endregion

    // region Notes

    fun addNote(text: String, dueDate: LocalDate?, pin: Boolean) {
        val me = userId ?: return
        val trimmed = text.trim().take(NoteRules.MAX_LENGTH)
        if (trimmed.isEmpty()) return
        val today = today()
        val canPin = pin && NoteRules.pinsInUse(_state.value.notes, today) < NoteRules.MAX_PINS
        val index = _state.value.notes.size
        val note = Note(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            authorId = me,
            createdAt = now(),
            dueDate = dueDate,
            pinnedUntil = if (canPin) today.plusDays(NoteRules.PIN_DAYS) else null,
            color = NoteColor.entries[index % NoteColor.entries.size],
            tack = Tack.entries[index % Tack.entries.size],
        )
        _state.update { it.copy(notes = it.notes + note) }
    }

    fun deleteNote(id: String) = _state.update { s -> s.copy(notes = s.notes.filterNot { it.id == id }) }

    fun togglePin(id: String) = _state.update { s ->
        val today = today()
        val pins = NoteRules.pinsInUse(s.notes, today)
        s.copy(notes = s.notes.map { n ->
            when {
                n.id != id -> n
                n.isPinned(today) -> n.copy(pinnedUntil = null)
                pins < NoteRules.MAX_PINS -> n.copy(pinnedUntil = today.plusDays(NoteRules.PIN_DAYS))
                else -> n
            }
        })
    }

    // endregion

    // region Chores

    fun addChore(title: String, assigneeId: String, dueDate: LocalDate, notes: String) {
        if (title.isBlank()) return
        val chore = Chore(UUID.randomUUID().toString(), title.trim(), assigneeId, dueDate, notes.trim())
        _state.update { it.copy(chores = it.chores + chore) }
    }

    fun toggleChoreDone(id: String) = _state.update { s ->
        s.copy(chores = s.chores.map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    fun reassignChore(id: String, assigneeId: String) = _state.update { s ->
        s.copy(chores = s.chores.map { if (it.id == id) it.copy(assigneeId = assigneeId) else it })
    }

    fun updateChore(id: String, title: String, assigneeId: String, dueDate: LocalDate, notes: String) {
        if (title.isBlank()) return
        _state.update { s ->
            s.copy(chores = s.chores.map {
                if (it.id == id) it.copy(title = title.trim(), assigneeId = assigneeId, dueDate = dueDate, notes = notes.trim()) else it
            })
        }
    }

    fun deleteChore(id: String) = _state.update { s -> s.copy(chores = s.chores.filterNot { it.id == id }) }

    // endregion

    // region Shopping

    fun addItem(name: String) {
        val me = userId ?: return
        if (name.isBlank()) return
        val item = ShoppingItem(UUID.randomUUID().toString(), name.trim(), addedById = me, addedAt = now())
        _state.update { it.copy(shopping = it.shopping + item) }
    }

    fun incrementQuantity(id: String) = updateItem(id) { it.copy(quantity = it.quantity + 1) }

    fun markBought(id: String) {
        val me = userId ?: return
        updateItem(id) { it.copy(boughtAt = now(), boughtById = me, couldntFindById = null) }
    }

    fun undoBought(id: String) = updateItem(id) { it.copy(boughtAt = null, boughtById = null) }

    fun toggleCouldntFind(id: String) {
        val me = userId ?: return
        updateItem(id) { it.copy(couldntFindById = if (it.couldntFindById == null) me else null) }
    }

    fun saveItem(id: String, name: String, quantity: Int, note: String) = updateItem(id) {
        it.copy(name = name.trim().ifEmpty { it.name }, quantity = quantity.coerceAtLeast(1), note = note.trim())
    }

    fun deleteItem(id: String) = _state.update { s -> s.copy(shopping = s.shopping.filterNot { it.id == id }) }

    private fun updateItem(id: String, transform: (ShoppingItem) -> ShoppingItem) = _state.update { s ->
        s.copy(shopping = s.shopping.map { if (it.id == id) transform(it) else it })
    }

    // endregion

    // region Seed data

    private const val DEMO_OWNER_NET_ID = "lk452"
    private const val DEMO_INVITE_CODE = "LOFT-4827"
    private val NET_ID = Regex("^[a-z]{2,3}[0-9]{1,5}$")

    /** Fake Cornell directory: NetID to (first name, last name, buddy). */
    private val DIRECTORY = mapOf(
        "lk452" to Triple("Leah", "Kim", Buddy.Mug),
        "mk482" to Triple("Mina", "Kaur", Buddy.Sock),
        "mp217" to Triple("Mihili", "Perera", Buddy.Plant),
        "jr905" to Triple("Jarmin", "Rahman", Buddy.Toaster),
        "sl318" to Triple("Stef", "Lopez", Buddy.Teapot),
    )

    private fun Session.toRoommate(status: MemberStatus) =
        Roommate(userId, netId, firstName, lastName, buddy, status)

    private fun inviteCodeFor(name: String, current: String? = null): String {
        val word = name.split(" ").lastOrNull { it.isNotBlank() }?.filter { it.isLetter() }?.uppercase()?.take(4)
        val digits = current?.substringAfter('-', "")?.takeIf { it.length == 4 } ?: (1000..9999).random().toString()
        return "${word?.ifEmpty { null } ?: "HOME"}-$digits"
    }

    private fun seedDemoHouse(currentUserNetId: String): RoomieState {
        val today = today()
        val now = now()
        val members = DIRECTORY.entries.mapIndexed { i, (netId, info) ->
            Roommate(
                id = netId, netId = netId, firstName = info.first, lastName = info.second, buddy = info.third,
                status = when (i) {
                    0 -> MemberStatus.Owner
                    1, 2 -> MemberStatus.Joined
                    else -> MemberStatus.InviteSent
                },
            )
        }
        val me = members.first { it.netId == currentUserNetId }
        val leah = "lk452"; val mina = "mk482"; val mihili = "mp217"; val jarmin = "jr905"; val stef = "sl318"

        fun note(id: Int, text: String, by: String, daysAgo: Long, color: NoteColor, tack: Tack, due: LocalDate? = null, pinned: Boolean = false) =
            Note("n$id", text, by, now.minusDays(daysAgo).minusMinutes(id.toLong()), due, if (pinned) today.plusDays(5) else null, color, tack)

        fun next(day: DayOfWeek) = today.with(TemporalAdjusters.next(day))
        val rentDue = today.with(TemporalAdjusters.firstDayOfNextMonth())

        val notes = listOf(
            note(1, "Quiet hours after 11 this week. Exams!!", jarmin, 2, NoteColor.Yellow, Tack.StarOrange, pinned = true),
            note(2, "Rent is due ${rentDue.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}", leah, 3, NoteColor.Blue, Tack.DotBlue, due = rentDue, pinned = true),
            note(3, "Plumber visit, 10 AM", leah, 1, NoteColor.Peach, Tack.StarOrange, due = next(DayOfWeek.TUESDAY)),
            note(4, "Parents visiting", leah, 2, NoteColor.Blue, Tack.DotBlue, due = next(DayOfWeek.SATURDAY)),
            note(5, "Movie night Friday? I'll make popcorn", mina, 1, NoteColor.Purple, Tack.StarPink),
            note(6, "Who took my mug?", stef, 2, NoteColor.Pink, Tack.DotRed),
            note(7, "Trash day is Thursday, don't forget!", mihili, 3, NoteColor.Green, Tack.DotGreen),
            note(8, "Borrowed your charger, will return it tonight, promise", jarmin, 4, NoteColor.Yellow, Tack.StarOrange),
            note(9, "Buy oat milk", stef, 5, NoteColor.Blue, Tack.DotBlue),
            note(10, "WiFi password changed, check the fridge whiteboard", mina, 6, NoteColor.Pink, Tack.DotRed),
            note(11, "Thanks for cleaning!", leah, 7, NoteColor.Peach, Tack.StarOrange),
            note(12, "Candle smell okay?", mihili, 8, NoteColor.Purple, Tack.StarPink),
            note(13, "Welcome to the Loft! Spare key is with Leah.", leah, 30, NoteColor.Yellow, Tack.StarOrange),
            note(14, "Fridge cleanout Sunday", mihili, 21, NoteColor.Green, Tack.DotGreen),
            note(15, "Packages pile up at the front desk", jarmin, 19, NoteColor.Peach, Tack.StarOrange),
            note(16, "House dinner next week?", stef, 17, NoteColor.Pink, Tack.DotRed),
            note(17, "Lost a blue umbrella", mina, 16, NoteColor.Blue, Tack.DotBlue),
        )

        // Weekly rota from the design (S M T W Th F S); this week and next so Upcoming is never empty.
        val rota = mapOf(
            DayOfWeek.SUNDAY to ("Grocery shopping" to jarmin),
            DayOfWeek.MONDAY to ("Mop floors" to mina),
            DayOfWeek.WEDNESDAY to ("Take out the trash!" to leah),
            DayOfWeek.THURSDAY to ("Vacuum living room" to mihili),
            DayOfWeek.FRIDAY to ("Scrub toilet" to stef),
            DayOfWeek.SATURDAY to ("Laundry day" to leah),
        )
        val chores = (weekOf(today) + weekOf(today.plusWeeks(1))).mapNotNull { day ->
            rota[day.dayOfWeek]?.let { (title, who) -> Chore("c$day", title, who, day, done = day.isBefore(today)) }
        }

        fun item(id: Int, name: String, by: String, hoursAgo: Long, qty: Int = 1, note: String = "", boughtBy: String? = null, boughtDaysAgo: Long? = null, couldntFind: String? = null) =
            ShoppingItem(
                "s$id", name, qty, note, by, now.minusHours(hoursAgo),
                boughtById = boughtBy, boughtAt = boughtDaysAgo?.let { now.minusDays(it) }, couldntFindById = couldntFind,
            )

        val shopping = listOf(
            item(1, "Eggs (dozen)", jarmin, 60, qty = 2, note = "free range please"),
            item(2, "Oat milk", leah, 50, note = "Oatly barista edition, green carton"),
            item(3, "Dish soap", mina, 30, couldntFind = stef),
            item(4, "Trash bags", leah, 5),
            item(5, "Bananas", mihili, 2),
            item(6, "Whole milk", leah, 80, boughtBy = leah, boughtDaysAgo = 2),
            item(7, "Paper towels", mina, 100, boughtBy = mina, boughtDaysAgo = 3),
            item(8, "Rice", jarmin, 150, boughtBy = jarmin, boughtDaysAgo = 5),
            item(9, "Almond milk", mina, 700, boughtBy = mina, boughtDaysAgo = 26),
            item(10, "Bananas", mihili, 300, boughtBy = mihili, boughtDaysAgo = 10),
        )

        return RoomieState(
            session = Session(me.id, me.netId, me.firstName, me.lastName, me.buddy, hasHouse = true),
            house = House("The Linden Loft", DEMO_INVITE_CODE, members, atStoreId = mina),
            notes = notes,
            chores = chores,
            shopping = shopping,
        )
    }

    // endregion

    /** Sunday-start week containing [date], matching the S M T W Th F S chore list. */
    fun weekOf(date: LocalDate): List<LocalDate> {
        val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return (0L..6L).map { start.plusDays(it) }
    }
}
