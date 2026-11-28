package com.example.roomie.ui.components

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.ui.theme.GlassButtonBorder
import com.example.roomie.ui.theme.PeachPill
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import kotlinx.coroutines.launch

/** Peach invite code card with a Copy button. [glass] uses the onboarding glass Copy button. */
@Composable
fun InviteCodeCard(
    code: String,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.invite_code),
    glass: Boolean = false,
    corner: Dp = 10.dp,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val clipLabel = stringResource(R.string.invite_clip_label)
    val copied = stringResource(R.string.invite_copied)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(PeachPill)
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = RoomieType.rowMeta.copy(fontWeight = FontWeight.Medium))
            Text(code, style = RoomieType.inviteCode.copy(fontSize = 20.sp, lineHeight = 24.sp))
        }
        Row(
            Modifier
                .height(44.dp)
                .then(
                    if (glass) {
                        Modifier.glassSurface(
                            RoundedCornerShape(12.dp),
                            borderWidth = 1.dp,
                            borderColor = GlassButtonBorder,
                        )
                    } else {
                        Modifier.clip(RoundedCornerShape(10.dp)).background(Surface)
                    },
                )
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_invite_copy)) {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, code)))
                        Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_copy), null, Modifier.size(16.dp), tint = Color.Unspecified)
            Text(stringResource(R.string.invite_copy), style = RoomieType.glassButton)
        }
    }
}

@Composable
fun ShareInviteButton(
    houseName: String,
    code: String,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.invite_share),
    glass: Boolean = false,
    height: Dp = 46.dp,
) {
    val context = LocalContext.current
    val message = stringResource(
        R.string.invite_share_text,
        houseName.ifBlank { stringResource(R.string.invite_share_our_house) },
        code,
    )
    val chooserTitle = stringResource(R.string.invite_share_chooser)
    SecondaryButton(
        text = text,
        icon = R.drawable.ic_link,
        modifier = modifier,
        glass = glass,
        height = height,
        textStyle = RoomieType.glassButton,
        onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(send, chooserTitle))
        },
    )
}
