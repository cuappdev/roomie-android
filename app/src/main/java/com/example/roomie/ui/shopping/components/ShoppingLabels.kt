package com.example.roomie.ui.shopping.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.roomie.R
import com.example.roomie.ui.shopping.ItemSubtitle
import com.example.roomie.ui.shopping.Person
import com.example.roomie.ui.util.agoLabel
import java.time.LocalDateTime

/** "You" / "you" for the signed-in roommate, their first name, or "Someone". */
@Composable
internal fun Person.label(capitalized: Boolean = true): String = when (this) {
    Person.Me -> stringResource(if (capitalized) R.string.shopping_you else R.string.you)
    is Person.Named -> name
    Person.Unknown -> stringResource(R.string.someone)
}

@Composable
internal fun ItemSubtitle.text(now: LocalDateTime): String = when (this) {
    is ItemSubtitle.CouldntFind -> when (by) {
        Person.Me -> stringResource(R.string.shopping_you_couldnt_find)
        else -> stringResource(R.string.shopping_couldnt_find_by, by.label())
    }
    is ItemSubtitle.WithNote -> stringResource(R.string.shopping_by_detail, by.label(), note)
    is ItemSubtitle.Added -> when (addedAt) {
        null -> by.label()
        else -> stringResource(R.string.shopping_by_detail, by.label(), addedAt.agoLabel(now))
    }
}
