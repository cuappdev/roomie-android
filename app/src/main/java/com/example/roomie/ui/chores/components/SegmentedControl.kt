package com.example.roomie.ui.chores.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.roomie.ui.components.BrandGradient
import com.example.roomie.ui.components.WhiteBorderWidth
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface

/** Two- or three-way pill switch ("My Chores / All Chores", "Assign Once / Rotate Weekly"). */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    corner: Dp = 12.dp,
    innerCorner: Dp = 8.dp,
) {
    val shape = RoundedCornerShape(corner)
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                2.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.06f),
                spotColor = ShadowWarm.copy(alpha = 0.06f),
            )
            .clip(shape)
            .background(Background)
            .background(GlassFaint)
            .border(WhiteBorderWidth, Color.White.copy(alpha = 0.85f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, option ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .height(height - 8.dp)
                    .clip(RoundedCornerShape(innerCorner))
                    .then(if (selected) Modifier.background(BrandGradient) else Modifier)
                    .selectable(selected = selected, role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = RoomieType.rowMeta.copy(
                        color = if (selected) Surface else BrandBrown,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SegmentedControlPreview() = RoomiePreview(padding = 16.dp) {
    SegmentedControl(listOf("My Chores", "All Chores"), selectedIndex = 0, onSelect = {})
}
