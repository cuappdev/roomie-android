package com.example.roomie.ui.chores.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.TanButtonEnd
import com.example.roomie.ui.theme.TanButtonStart
import com.example.roomie.ui.theme.TanButtonText

/** Light tan gradient button ("View Rotation", "View & Edit Rotation"). */
@Composable
fun TanButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(
                2.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.12f),
                spotColor = ShadowWarm.copy(alpha = 0.12f),
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(TanButtonStart, TanButtonEnd)))
            .border(1.dp, BrandBrown.copy(alpha = 0.6f), shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = RoomieType.glassButton.copy(color = TanButtonText))
    }
}

@Preview
@Composable
private fun TanButtonPreview() = RoomiePreview(padding = 16.dp) {
    TanButton("View Rotation", {})
}
