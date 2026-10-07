package com.example.roomie.data

import java.time.LocalDateTime

/** Behaviour rules for the shopping list, from the "Behaviour notes" frame in the Figma file. */
object ShoppingRules {
    const val CLEAR_BOUGHT_AFTER_DAYS = 7L
    const val MATCH_LOOKBACK_DAYS = 30L
    const val ADD_AGAIN_CHIPS = 6

    sealed interface Match {
        val item: ShoppingItem

        /** Already needed: offer "Make it ×(n+1)". */
        data class OnList(override val item: ShoppingItem) : Match

        /** Bought in the last 30 days: offer "Add again". */
        data class RecentlyBought(override val item: ShoppingItem) : Match
    }

    /** Bought items clear after 7 days. */
    fun visible(items: List<ShoppingItem>, now: LocalDateTime): List<ShoppingItem> =
        items.filter { it.boughtAt == null || it.boughtAt.isAfter(now.minusDays(CLEAR_BOUGHT_AFTER_DAYS)) }

    fun needed(items: List<ShoppingItem>): List<ShoppingItem> =
        items.filter { !it.isBought }.sortedBy { it.addedAt }

    fun bought(items: List<ShoppingItem>, now: LocalDateTime): List<ShoppingItem> =
        visible(items, now).filter { it.isBought }.sortedByDescending { it.boughtAt }

    /** Duplicate check: the current list plus anything bought in the last 30 days. */
    fun matches(query: String, items: List<ShoppingItem>, now: LocalDateTime): List<Match> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val onList = needed(items).filter { it.name.contains(q, ignoreCase = true) }
        val neededNames = needed(items).map { it.name.lowercase() }.toSet()
        val recent = items
            .filter { it.isBought && it.boughtAt!!.isAfter(now.minusDays(MATCH_LOOKBACK_DAYS)) }
            .filter { it.name.contains(q, ignoreCase = true) && it.name.lowercase() !in neededNames }
            .sortedByDescending { it.boughtAt }
            .distinctBy { it.name.lowercase() }
        return onList.map { Match.OnList(it) } + recent.map { Match.RecentlyBought(it) }
    }

    /** "Add again" chips: recently bought names that aren't already needed. */
    fun addAgain(items: List<ShoppingItem>, now: LocalDateTime): List<String> {
        val neededNames = needed(items).map { it.name.lowercase() }.toSet()
        return items
            .filter { it.isBought && it.boughtAt!!.isAfter(now.minusDays(MATCH_LOOKBACK_DAYS)) }
            .sortedByDescending { it.boughtAt }
            .map { it.name }
            .distinctBy { it.lowercase() }
            .filter { it.lowercase() !in neededNames }
            .take(ADD_AGAIN_CHIPS)
    }
}
