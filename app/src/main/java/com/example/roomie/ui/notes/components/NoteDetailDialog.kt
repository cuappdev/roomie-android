package com.example.roomie.ui.notes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.data.Note
import com.example.roomie.data.NoteRules
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.notes.components.StickyNote
import com.example.roomie.ui.notes.components.StickySize
import com.example.roomie.ui.notes.components.TackView
import com.example.roomie.ui.notes.components.color
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.Poppins
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.StickyText
import com.example.roomie.ui.util.shortLabel

/** Full note, opened by tapping a sticky (stickies cut off after 3 lines). */
@Composable
fun NoteDetailDialog(
    note: Note,
    authorName: String,
    pinned: Boolean,
    canPin: Boolean,
    isMine: Boolean,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = note.color.color(),
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    note.text,
                    style = TextStyle(fontFamily = Poppins, fontSize = 18.sp, lineHeight = 24.sp, color = StickyText),
                )
                note.dueDate?.let { DueChip(it.shortLabel()) }
                Text(
                    stringResource(R.string.note_author, authorName),
                    style = TextStyle(fontFamily = Poppins, fontSize = 15.sp, color = StickyText),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        },
        confirmButton = { TextAction(stringResource(R.string.close), onDismiss) },
        dismissButton = {
            Row {
                if (pinned || canPin) {
                    TextAction(
                        if (pinned) {
                            stringResource(R.string.note_unpin)
                        } else {
                            pluralStringResource(
                                R.plurals.note_pin_for_days,
                                NoteRules.PIN_DAYS.toInt(),
                                NoteRules.PIN_DAYS,
                            )
                        },
                        onTogglePin,
                    )
                }
                if (isMine) {
                    TextAction(
                        stringResource(R.string.delete),
                        onDelete,
                        style = RoomieType.linkButton.copy(color = com.example.roomie.ui.theme.Danger),
                    )
                }
            }
        },
    )
}
