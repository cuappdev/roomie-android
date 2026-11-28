package com.example.roomie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.ui.notes.components.color
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.Placeholder
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextPrimary

/** Dropdown trigger ("Select a due date  v"); the chevron flips up while [expanded]. */
@Composable
fun DropdownField(
    text: String?,
    placeholder: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(
                2.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.05f),
                spotColor = ShadowWarm.copy(alpha = 0.05f),
            )
            .clip(shape)
            .background(Surface)
            .border(1.dp, Border, shape)
            .clickable(role = Role.DropdownList, onClickLabel = contentDescription, onClick = onClick)
            .padding(start = 15.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text ?: placeholder,
            style = RoomieType.input.copy(
                lineHeight = 20.sp,
                color = if (text ==
                    null
                ) {
                    Placeholder
                } else {
                    TextPrimary
                },
            ),
            modifier = Modifier.weight(1f),
        )
        Icon(
            painterResource(R.drawable.ic_dropdown_chevron),
            null,
            Modifier.size(width = 12.dp, height = 6.dp).rotate(if (expanded) 0f else 180f),
            tint = Color.Unspecified,
        )
    }
}
