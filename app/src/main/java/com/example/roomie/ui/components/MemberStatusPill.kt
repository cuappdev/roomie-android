package com.example.roomie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.data.MemberStatus
import com.example.roomie.ui.notes.components.color
import com.example.roomie.ui.theme.NoteGreen
import com.example.roomie.ui.theme.PeachPill
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.SuccessText
import com.example.roomie.ui.theme.TextSecondary

@Composable
fun StatusPill(text: String, background: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = RoomieType.pill.copy(color = contentColor))
    }
}

@Composable
fun MemberStatusPill(
    status: MemberStatus,
    ownerStyle: Boolean = true,
    pendingLabel: String = stringResource(R.string.status_pending),
) {
    when (status) {
        MemberStatus.Owner -> if (ownerStyle) {
            StatusPill(
                stringResource(R.string.status_owner),
                NoteGreen,
                SuccessText,
            )
        } else {
            StatusPill(stringResource(R.string.status_owner), PeachPill, TextSecondary)
        }
        MemberStatus.Joined -> StatusPill(stringResource(R.string.status_joined), NoteGreen, SuccessText)
        MemberStatus.InviteSent -> StatusPill(pendingLabel, PeachPill, TextSecondary)
    }
}
