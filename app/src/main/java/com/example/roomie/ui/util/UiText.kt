package com.example.roomie.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Text a ViewModel wants shown, without needing a Context: a string resource, or text that already
 * arrived as words (the repositories' error messages, until they report errors as types).
 */
sealed interface UiText {
    data class Res(@StringRes val id: Int) : UiText

    data class Raw(val text: String) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> stringResource(id)
    is UiText.Raw -> text
}
