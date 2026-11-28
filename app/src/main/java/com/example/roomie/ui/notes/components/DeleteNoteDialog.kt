package com.example.roomie.ui.notes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roomie.R
import com.example.roomie.data.Note
import com.example.roomie.ui.components.WhiteBorderWidth
import com.example.roomie.ui.notes.components.StickyNote
import com.example.roomie.ui.notes.components.StickySize
import com.example.roomie.ui.notes.components.TackView
import com.example.roomie.ui.notes.components.color
import com.example.roomie.ui.theme.CorkInner
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.GlassFaintEnd
import com.example.roomie.ui.theme.GlassFaintStart
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.Tan
import com.example.roomie.ui.theme.TextPrimary

/** "Delete Note / Confirm". */
@Composable
fun DeleteNoteDialog(note: Note, onCancel: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(
                    12.dp,
                    shape,
                    ambientColor = ShadowWarm.copy(alpha = 0.22f),
                    spotColor = ShadowWarm.copy(alpha = 0.22f),
                )
                .clip(shape)
                .background(CorkInner)
                .border(1.dp, Tan.copy(alpha = 0.45f), shape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.note_delete_title),
                style = RoomieType.titleEmpty.copy(fontSize = 24.sp, lineHeight = 28.sp, color = TextPrimary),
            )
            Text(
                stringResource(R.string.note_delete_body),
                style = RoomieType.input.copy(lineHeight = 20.sp, color = TextPrimary),
            )
            Text(
                stringResource(R.string.note_quoted, note.text),
                style = RoomieType.choreMeta.copy(color = TextPrimary),
                maxLines = 3,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val buttonShape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(buttonShape)
                        .background(Brush.horizontalGradient(listOf(GlassFaintStart, GlassFaintEnd)))
                        .border(WhiteBorderWidth, Color.White.copy(alpha = 0.85f), buttonShape)
                        .clickable(role = Role.Button, onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        style = RoomieType.pillButton.copy(fontWeight = FontWeight.Medium),
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(buttonShape)
                        .background(Danger)
                        .clickable(role = Role.Button, onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.delete),
                        style = RoomieType.pillButton.copy(color = Surface, fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
    }
}
