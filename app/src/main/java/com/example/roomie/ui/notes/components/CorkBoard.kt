package com.example.roomie.ui.notes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.ui.notes.components.StickyNote
import com.example.roomie.ui.notes.components.StickySize
import com.example.roomie.ui.notes.components.TackView
import com.example.roomie.ui.notes.components.color
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.CorkInner
import com.example.roomie.ui.theme.Tan

/** Tan-framed cork board. The edit button sits in the top-right corner, as in the design. */
@Composable
fun CorkBoard(
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    editing: Boolean = false,
    elevated: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            // Home refresh: 0 4 4 rgba(0,0,0,.25) under the frame.
            .then(if (elevated) Modifier.shadow(4.dp, shape, clip = false) else Modifier)
            .clip(shape)
            .background(CorkInner)
            .border(12.dp, Tan, shape)
            .padding(12.dp),
    ) {
        content()
        if (onEdit != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (editing) BrandBrown else CorkInner)
                    .border(1.dp, BrandBrown, CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (editing) {
                            stringResource(
                                R.string.cd_board_done_editing,
                            )
                        } else {
                            stringResource(R.string.cd_board_edit)
                        },
                        onClick = onEdit,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_edit),
                    if (editing) {
                        stringResource(
                            R.string.cd_board_done_editing,
                        )
                    } else {
                        stringResource(R.string.cd_board_edit)
                    },
                    Modifier.size(16.dp),
                    tint = if (editing) Color.White else Color.Unspecified,
                )
            }
        }
    }
}
