package com.example.roomie.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.data.Buddy
import com.example.roomie.ui.theme.RoomiePreview

@Composable
fun BuddyAvatar(buddy: Buddy, size: Dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Image(painterResource(buddy.drawable), contentDescription, modifier.size(size))
}

/** The buddy's name, in the phone's language. */
@Composable
fun Buddy.displayName(): String = stringResource(
    when (this) {
        Buddy.Mug -> R.string.buddy_mug
        Buddy.Sock -> R.string.buddy_sock
        Buddy.Plant -> R.string.buddy_plant
        Buddy.Toaster -> R.string.buddy_toaster
        Buddy.Teapot -> R.string.buddy_teapot
    },
)

@Preview
@Composable
private fun BuddyAvatarPreview() = RoomiePreview(padding = 16.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Buddy.entries.forEach { BuddyAvatar(it, 48.dp) }
    }
}
