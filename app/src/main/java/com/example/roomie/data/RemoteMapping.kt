package com.example.roomie.data

import com.example.roomie.data.api.ChoreDto
import com.example.roomie.data.api.HouseDto
import com.example.roomie.data.api.MemberRole
import com.example.roomie.data.api.NoteColorDto
import com.example.roomie.data.api.NoteDto
import com.example.roomie.data.api.ShoppingItemDto
import com.example.roomie.data.api.TurnDto
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Turns the backend's DTOs into the app's models (data/Models.kt), so the screens built against
 * [FakeRoomieRepository] work unchanged. Pure functions only; [RemoteRoomieRepository] does the I/O.
 *
 * IDs become strings. Turns get a `t` prefix ("t42") because a chore's ID and its turns' IDs come
 * from different tables, and pending invites get `invite-` so they can't collide with user IDs.
 */
internal object RemoteMapping {

    const val TURN_PREFIX = "t"
    const val INVITE_PREFIX = "invite-"
    const val DRAFT_INVITE_PREFIX = "draft-"

    /** The backend's NetID rule (`houses.schema.ts`). Looser than the fake's. */
    private val NET_ID = Regex("^[a-z]{1,4}[0-9]{1,6}$")

    /** Shopping item names can be at most this long on the server. */
    private const val MAX_ITEM_NAME = 80

    fun normalizeNetId(input: String): String = input.trim().lowercase().removeSuffix("@cornell.edu")

    fun isValidNetId(input: String): Boolean = NET_ID.matches(normalizeNetId(input))

    /** "Leah Kim" → ("Leah", "Kim"); "Stef Rivera-Osorio" keeps the rest together as the last name. */
    fun splitName(name: String): Pair<String, String> {
        val parts = name.trim().split(Regex("\\s+"), limit = 2)
        return parts[0] to parts.getOrElse(1) { "" }
    }

    /**
     * The backend has no house buddy, so roommates get one picked from their user ID: stable across
     * launches and devices, and different for neighbours in the member list. Your own pick is
     * remembered on this device and shown instead.
     */
    fun buddyFor(userId: Int): Buddy = Buddy.entries[Math.floorMod(userId, Buddy.entries.size)]

    fun house(dto: HouseDto, myId: Int, myBuddy: Buddy): House = House(
        name = dto.name,
        inviteCode = dto.inviteCode,
        members = dto.members.map { member ->
            val (first, last) = splitName(member.name)
            Roommate(
                id = member.id.toString(),
                netId = member.netid,
                firstName = first,
                lastName = last,
                buddy = if (member.id == myId) myBuddy else buddyFor(member.id),
                status = if (member.role == MemberRole.OWNER) MemberStatus.Owner else MemberStatus.Joined,
            )
        } + dto.pendingInvites.map { invite ->
            Roommate(
                id = INVITE_PREFIX + invite.id,
                netId = invite.netid,
                firstName = invite.netid,
                buddy = buddyFor(invite.netid.hashCode()),
                status = MemberStatus.InviteSent,
            )
        },
        // The backend has no "at the store" status yet.
        atStoreId = null,
    )

    /** A house that only exists on this device until onboarding's last step creates it. */
    fun draftHouse(name: String, me: Roommate, invites: List<String>): House = House(
        name = name,
        // The server makes the code when the house is created.
        inviteCode = "",
        members = listOf(me) + invites.map { netId ->
            Roommate(DRAFT_INVITE_PREFIX + netId, netId, netId, buddy = buddyFor(netId.hashCode()), status = MemberStatus.InviteSent)
        },
    )

    fun noteColor(dto: NoteColorDto): NoteColor = when (dto) {
        NoteColorDto.YELLOW -> NoteColor.Yellow
        NoteColorDto.ORANGE -> NoteColor.Peach
        NoteColorDto.BLUE -> NoteColor.Blue
        NoteColorDto.GREEN -> NoteColor.Green
        NoteColorDto.PINK -> NoteColor.Pink
        NoteColorDto.PURPLE -> NoteColor.Purple
    }

    /**
     * Pinned notes on the server stay pinned until someone unpins them, so the app's 7-day pin
     * becomes "pinned until [LocalDate.MAX]". The tack isn't stored either; it's picked from the ID
     * so a note keeps its tack between refreshes.
     */
    fun note(dto: NoteDto, zone: ZoneId): Note = Note(
        id = dto.id.toString(),
        text = dto.body,
        authorId = dto.author.id.toString(),
        createdAt = dto.createdAt.toLocal(zone),
        dueDate = dto.dueOn,
        pinnedUntil = if (dto.pinned) LocalDate.MAX else null,
        color = noteColor(dto.color),
        tack = Tack.entries[Math.floorMod(dto.id, Tack.entries.size)],
    )

    /** One turn of a server chore. Every turn belongs to a chore, so it always has a [Chore.rotationId]. */
    fun chore(turn: TurnDto, houseZone: ZoneId): Chore = Chore(
        id = TURN_PREFIX + turn.id,
        title = turn.chore.title,
        assigneeIds = listOf(turn.assignee.id.toString()),
        dueDate = turn.dueOn,
        completedOn = turn.completedAt?.atZone(houseZone)?.toLocalDate(),
        rotationId = turn.chore.id.toString(),
    )

    /**
     * A server chore as the app's weekly [Rotation]: one person per turn, due on its next turn's
     * weekday. The plan starts the week of the next turn, with the members reordered so whoever has
     * that turn comes first; [Rotation.assigneesFor] then predicts the following weeks the same way
     * the server assigns them.
     */
    fun rotation(dto: ChoreDto): Rotation {
        val next = dto.comingUp.firstOrNull()
        val anchor = next?.dueOn ?: dto.startsOn
        val ids = dto.rotation.map { it.id.toString() }
        val start = ids.indexOf(next?.assignee?.id?.toString()).coerceAtLeast(0)
        return Rotation(
            id = dto.id.toString(),
            title = dto.title,
            dueDay = anchor.dayOfWeek,
            plans = listOf(RotationPlan(ChoreRules.weekStart(anchor), ids.drop(start) + ids.take(start), peoplePerWeek = 1)),
        )
    }

    fun item(dto: ShoppingItemDto, zone: ZoneId): ShoppingItem {
        val (name, quantity) = decodeItemName(dto.name)
        return ShoppingItem(
            id = dto.id.toString(),
            name = name,
            quantity = quantity,
            addedById = dto.addedBy.id.toString(),
            addedAt = dto.createdAt.toLocal(zone),
            boughtById = dto.boughtBy?.id?.toString(),
            boughtAt = dto.boughtAt?.toLocal(zone),
        )
    }

    /**
     * The backend has no quantity, so "Eggs" ×2 is stored as the name `Eggs ×2`. Roommates on other
     * clients see the same readable name. Quantities of 1 are left off.
     */
    fun encodeItemName(name: String, quantity: Int): String {
        val suffix = if (quantity > 1) " ×$quantity" else ""
        return name.trim().take(MAX_ITEM_NAME - suffix.length).trimEnd() + suffix
    }

    fun decodeItemName(raw: String): Pair<String, Int> {
        val match = Regex("^(.*\\S)\\s+×(\\d{1,4})$").find(raw.trim()) ?: return raw.trim() to 1
        val quantity = match.groupValues[2].toInt()
        return if (quantity >= 2) match.groupValues[1] to quantity else raw.trim() to 1
    }

    /**
     * The first day a new weekly chore can start: [dueDay] in [startWeek], or the same weekday a
     * week later if that's already past (the server rejects start dates before [today]).
     */
    fun firstTurn(startWeek: LocalDate, dueDay: java.time.DayOfWeek, today: LocalDate): LocalDate {
        var date = ChoreRules.weekStart(startWeek).plusDays(dueDay.value - 1L)
        while (date.isBefore(today)) date = date.plusWeeks(1)
        return date
    }

    private fun Instant.toLocal(zone: ZoneId): LocalDateTime = LocalDateTime.ofInstant(this, zone)
}
