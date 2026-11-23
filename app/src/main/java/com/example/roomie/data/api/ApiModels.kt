package com.example.roomie.data.api

import java.time.Instant
import java.time.LocalDate

// Objects the backend returns, shared across services. Money is whole cents and IDs are ints.
// These mirror the API exactly; map them to the app's models in data/Models.kt at the repository
// or ViewModel layer.

enum class MemberRole { OWNER, MEMBER }

/** The "I'm home / away" toggle. */
enum class Presence { HOME, AWAY }

/** The public view of a person: members, rotation, assignee, claimedBy, … */
data class UserDto(
    val id: Int,
    /** Lowercase, e.g. `mnh49`. */
    val netid: String,
    val name: String,
    val avatarUrl: String?,
    val presence: Presence,
)

/** Your own user, from `/auth/login` and `/users/me`. */
data class SelfDto(
    val id: Int,
    val netid: String,
    val name: String,
    val avatarUrl: String?,
    val presence: Presence,
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
    val presence: Presence,
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
