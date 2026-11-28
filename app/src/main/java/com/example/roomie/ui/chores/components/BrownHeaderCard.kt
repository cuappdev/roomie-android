package com.example.roomie.ui.chores.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.ui.components.BrandGradient
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.HeaderCardBorder
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface

/** Brown gradient card that heads the rotation pages. */
@Composable
fun BrownHeaderCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(
                8.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.15f),
                spotColor = ShadowWarm.copy(alpha = 0.15f),
            )
            .clip(shape)
            .background(BrandGradient)
            .border(0.6.dp, HeaderCardBorder, shape)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            title,
            style = RoomieType.sheetTitle.copy(
                fontSize = 24.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = Surface,
            ),
        )
        Text(subtitle, style = RoomieType.choreMeta.copy(color = Background))
    }
}

@Preview
@Composable
private fun BrownHeaderCardPreview() = RoomiePreview(padding = 16.dp) {
    BrownHeaderCard("Manage Rotations", "Automatic weekly assignments")
}
