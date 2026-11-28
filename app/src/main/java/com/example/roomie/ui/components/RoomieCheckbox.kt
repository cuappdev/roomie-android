package com.example.roomie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Surface

/** 24dp rounded checkbox with a 44dp touch target. */
@Composable
fun RoomieCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier
            .size(44.dp)
            .semantics { this.contentDescription = contentDescription }
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(shape)
                .background(if (checked) BrandBrown else Color.Transparent)
                .border(2.dp, BrandBrown, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(painterResource(R.drawable.ic_check_small), null, Modifier.size(14.dp), tint = Surface)
        }
    }
}
