package com.example.roomie.data.api

import java.time.Instant
import java.time.LocalDate

// Objects the backend returns, shared across services. Money is whole cents and IDs are ints.
// These mirror the API exactly; map them to the app's models in data/Models.kt at the repository
// or ViewModel layer.

enum class MemberRole { OWNER, MEMBER }

enum class ChoreFrequency { DAILY, EVERY_OTHER_DAY, WEEKLY, BIWEEKLY, MONTHLY }

/** Computed by the server; not stored. */
enum class TurnStatus { DONE, OVERDUE, DUE_TODAY, UPCOMING }

enum class ShoppingCategory { GROCERIES, HOUSEHOLD, OTHER }

enum class ShoppingStatus { NEEDED, BOUGHT }

/** Proposed; the Note Board isn't built on the backend yet. */
enum class NoteColorDto { YELLOW, PINK, BLUE, GREEN, PURPLE }

/** The public view of a person: members, rotation, assignee, claimedBy, … */
data class UserDto(
    val id: Int,
    /** Lowercase, e.g. `mnh49`. */
    val netid: String,
    val name: String,
    val avatarUrl: String?,
)

/** Your own user, from `/auth/login` and `/users/me`. */
data class SelfDto(
    val id: Int,
    val netid: String,
    val name: String,
    val avatarUrl: String?,
    val email: String,
    val major: String?,
    val createdAt: Instant,
)

/** A [UserDto] plus what only makes sense inside a house. */
data class MemberDto(
    val id: Int,
    val netid: String,
    val name: String,
    val avatarUrl: String?,
    val role: MemberRole,
    /** If the owner leaves, the member who joined earliest becomes owner. */
    val joinedAt: Instant,
)

data class HouseDto(
    val id: Int,
    val name: String,
    /** e.g. `LOFT-4827`. Not case-sensitive. */
    val inviteCode: String,
    val unit: String?,
    val leaseEndsOn: LocalDate?,
    /** IANA name. Decides what "today" means for chores. */
    val timezone: String,
    val members: List<MemberDto>,
    val pendingInvites: List<PendingInviteDto>,
)

/** An invite the house sent that hasn't been answered. */
data class PendingInviteDto(val id: Int, val netid: String, val createdAt: Instant)

/** The short house in `GET /users/me`. [role] is yours. */
data class HouseSummaryDto(val id: Int, val name: String, val role: MemberRole)

/** An invite you received. Has enough for the "Join this house?" prompt. */
data class InviteDto(
    val id: Int,
    val createdAt: Instant,
    val house: InviteHouseDto,
    val invitedBy: UserDto,
)

data class InviteHouseDto(val id: Int, val name: String, val members: List<UserDto>)

data class ChoreDto(
    val id: Int,
    val title: String,
    val frequency: ChoreFrequency,
    /** The first turn's date. */
    val startsOn: LocalDate,
    val allowSwaps: Boolean,
    /** Remind the day before at this hour (0–23). Saved, but reminders aren't sent yet. */
    val reminderHour: Int?,
    /** In turn order. */
    val rotation: List<UserDto>,
    /** The next 3 turns. */
    val comingUp: List<TurnDto>,
)

/** One occurrence of a chore. Its ID is the `assignmentId` in URLs. */
data class TurnDto(
    val id: Int,
    val chore: TurnChoreDto,
    val dueOn: LocalDate,
    val status: TurnStatus,
    val assignee: UserDto,
    val completedAt: Instant?,
    /** Who actually did it. Can differ from [assignee]. */
    val completedBy: UserDto?,
    /** The chore allows swaps and the turn is upcoming and unfinished. */
    val canSwap: Boolean,
)

data class TurnChoreDto(val id: Int, val title: String, val frequency: ChoreFrequency)

data class ShoppingItemDto(
    val id: Int,
    val name: String,
    val category: ShoppingCategory,
    val status: ShoppingStatus,
    val priceCents: Int?,
    val addedBy: UserDto,
    /** "I'll get it". Cleared when bought. */
    val claimedBy: UserDto?,
    val boughtBy: UserDto?,
    val boughtAt: Instant?,
    /** Non-null means the item is on a bill and locked. */
    val billId: Int?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class NoteDto(
    val id: Int,
    /** 1 to 500 characters. */
    val body: String,
    val color: NoteColorDto,
    /** Compare `author.id` with your own ID to show Edit and Delete. */
    val author: UserDto,
    val pinned: Boolean,
    /** When the pin ends. Null on a pinned note means until someone unpins it. */
    val pinnedUntil: Instant?,
    val createdAt: Instant,
    /** Later than [createdAt] means it was edited. */
    val updatedAt: Instant,
)
