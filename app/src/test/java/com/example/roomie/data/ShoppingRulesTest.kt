package com.example.roomie.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ShoppingRulesTest {
    private val now = LocalDateTime.of(2026, 11, 11, 12, 0)

    private fun item(id: String, name: String, boughtDaysAgo: Long? = null, qty: Int = 1) = ShoppingItem(
        id = id,
        name = name,
        quantity = qty,
        addedById = "lk452",
        addedAt = now.minusDays(40),
        boughtById = boughtDaysAgo?.let { "mk482" },
        boughtAt = boughtDaysAgo?.let { now.minusDays(it) },
    )

    @Test
    fun boughtItemsClearAfterSevenDays() {
        val items = listOf(item("a", "Rice", boughtDaysAgo = 6), item("b", "Eggs", boughtDaysAgo = 8), item("c", "Milk"))
        assertEquals(listOf("a"), ShoppingRules.bought(items, now).map { it.id })
        assertEquals(listOf("c"), ShoppingRules.needed(items).map { it.id })
    }

    @Test
    fun duplicateCheck_offersMakeItMoreForItemsOnTheList() {
        val items = listOf(item("a", "Oat milk", qty = 1), item("b", "Bread"))
        val matches = ShoppingRules.matches("milk", items, now)
        assertEquals(1, matches.size)
        assertTrue(matches[0] is ShoppingRules.Match.OnList)
        assertEquals("a", matches[0].item.id)
    }

    @Test
    fun duplicateCheck_looksBack30DaysAndSkipsNamesAlreadyNeeded() {
        val items = listOf(
            item("needed", "Oat milk"),
            item("dupe", "Oat milk", boughtDaysAgo = 3),
            item("recent", "Whole milk", boughtDaysAgo = 2),
            item("month", "Almond milk", boughtDaysAgo = 29),
            item("tooOld", "Goat milk", boughtDaysAgo = 31),
        )
        val matches = ShoppingRules.matches("MILK", items, now)
        assertEquals(listOf("needed", "recent", "month"), matches.map { it.item.id })
        assertTrue(matches.drop(1).all { it is ShoppingRules.Match.RecentlyBought })
    }

    @Test
    fun addAgainChips_areRecentDistinctAndNotAlreadyNeeded() {
        val items = listOf(
            item("1", "Bananas"),
            item("2", "Bananas", boughtDaysAgo = 1),
            item("3", "Rice", boughtDaysAgo = 2),
            item("4", "rice", boughtDaysAgo = 4),
            item("5", "Paper towels", boughtDaysAgo = 40),
        )
        assertEquals(listOf("Rice"), ShoppingRules.addAgain(items, now))
    }
}
