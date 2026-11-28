package com.example.roomie.ui.chores.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.ui.components.WhiteBorderWidth
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.GlassFaintEnd
import com.example.roomie.ui.theme.GlassFaintStart
import com.example.roomie.ui.theme.PeachPill
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.theme.TextSecondary

/** Faint tan glass fill shared by the chores controls. */
internal val GlassFaint = Brush.horizontalGradient(listOf(GlassFaintStart, GlassFaintEnd))

/** Faint tan dropdown trigger used throughout the chores forms. */
@Composable
fun GlassDropdownTrigger(
    expanded: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    startPadding: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(42.dp)
            .shadow(
                2.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.05f),
                spotColor = ShadowWarm.copy(alpha = 0.05f),
            )
            .clip(shape)
            .background(Background)
            .background(GlassFaint)
            .border(WhiteBorderWidth, Color.White.copy(alpha = 0.82f), shape)
            .clickable(role = Role.DropdownList, onClickLabel = contentDescription, onClick = onClick)
            .padding(start = startPadding, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { content() }
        Icon(
            painterResource(if (expanded) R.drawable.ic_chevron_up_small else R.drawable.ic_chevron_down_small),
            null,
            Modifier.size(16.dp),
            tint = Color.Unspecified,
        )
    }
}

/** White options panel that opens under a trigger. */
@Composable
fun DropdownOptions(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(
                5.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.12f),
                spotColor = ShadowWarm.copy(alpha = 0.12f),
            )
            .clip(shape)
            .background(Surface)
            .border(0.65.dp, Border, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** Single-choice dropdown ("1 person", "Week of Nov 16, 2026"). Collapses once something is picked. */
@Composable
fun <T> ChoiceDropdown(
    value: T?,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    placeholder: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassDropdownTrigger(open, { open = !open }, contentDescription) {
            Text(
                if (value != null) label(value) else placeholder,
                style = RoomieType.choreMeta.copy(color = if (value == null) TextSecondary else TextPrimary),
            )
        }
        if (open) {
            DropdownOptions {
                options.forEach { option ->
                    val selected = option == value
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) PeachPill else Color.Transparent)
                            .selectable(selected = selected, role = Role.RadioButton) {
                                onSelect(option)
                                open = false
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label(option),
                            style = RoomieType.choreMeta.copy(
                                color = TextPrimary,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChoiceDropdownPreview() = RoomiePreview(padding = 16.dp) {
    ChoiceDropdown(value = 2, options = listOf(1, 2, 3), label = {
        "$it people"
    }, onSelect = {}, placeholder = "Choose", contentDescription = "People per week")
}
