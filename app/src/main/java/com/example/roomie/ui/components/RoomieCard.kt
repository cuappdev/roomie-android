package com.example.roomie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.roomie.ui.theme.Surface

/** White bordered card container used on Me, Chores and Shopping. */
@Composable
fun RoomieCard(
    modifier: Modifier = Modifier,
    corner: Dp = 8.dp,
    borderColor: Color = com.example.roomie.ui.theme.Divider,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(1.dp, borderColor, shape),
        content = content,
    )
}
