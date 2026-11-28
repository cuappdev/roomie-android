package com.example.roomie.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Wraps a `@Preview` in the app theme on the app's cream background. */
@Composable
fun RoomiePreview(padding: Dp = 0.dp, background: Color = Background, content: @Composable () -> Unit) {
    RoomieTheme {
        Box(Modifier.background(background).padding(padding)) {
            content()
        }
    }
}
