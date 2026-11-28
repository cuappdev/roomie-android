package com.example.roomie.ui.chores.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.roomie.R

/** "1 person", "3 people". */
@Composable
internal fun peopleLabel(count: Int): String = pluralStringResource(R.plurals.rotation_people_count, count, count)

/** "Mina", "Mina and Leah", "Mina, Leah and Stef"; empty for nobody. */
@Composable
internal fun joinNames(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names.single()
    else -> stringResource(
        R.string.names_and,
        names.dropLast(1).joinToString(stringResource(R.string.names_separator)),
        names.last(),
    )
}
