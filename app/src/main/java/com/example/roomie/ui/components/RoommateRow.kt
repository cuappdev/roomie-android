package com.example.roomie.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.data.Roommate
import com.example.roomie.ui.theme.RoomieType

/** Roommate row with avatar, name and an optional status pill. */
@Composable
fun RoommateRow(
    roommate: Roommate,
    isMe: Boolean,
    modifier: Modifier = Modifier,
    nameStyle: TextStyle = RoomieType.listTitle,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuddyAvatar(roommate.buddy, 32.dp)
        Text(
            if (isMe) stringResource(R.string.roommate_is_you, roommate.firstName) else roommate.firstName,
            style = nameStyle,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}
