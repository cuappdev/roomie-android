package com.example.roomie.ui.notes.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.data.Note
import com.example.roomie.data.NoteRules
import com.example.roomie.ui.components.BrandGradient
import com.example.roomie.ui.components.RoomieInlineCalendar
import com.example.roomie.ui.notes.components.StickyNote
import com.example.roomie.ui.notes.components.StickySize
import com.example.roomie.ui.notes.components.TackView
import com.example.roomie.ui.notes.components.color
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Poppins
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.Tan
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.util.shortLabel
import java.time.LocalDate

private val EditButtonFill = Color(0xFFFFFAF2)

/**
 * "Inline Edit": the note opens large over the dimmed board with the keyboard up. Cancel and Save
 * sit above it; the button below adds, changes or removes the due date.
 */
@Composable
fun InlineNoteEditor(
    note: Note,
    authorName: String,
    today: LocalDate,
    onCancel: () -> Unit,
    onSave: (String, LocalDate?) -> Unit,
) {
    // Cursor starts at the end so the user can keep typing, as in the Figma keyboard frame.
    var text by rememberSaveable(note.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(note.text, TextRange(note.text.length)))
    }
    var due by rememberSaveable(note.id) { mutableStateOf(note.dueDate) }
    var calendarOpen by rememberSaveable(note.id) { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(note.id) {
        focus.requestFocus()
        keyboard?.show()
    }
    BackHandler(onBack = onCancel)

    Box(
        Modifier
            .fillMaxSize()
            .background(HoldScrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                SmallEditButton(
                    stringResource(R.string.cancel),
                    onCancel,
                    fill = SolidColor(EditButtonFill),
                    textColor = BrandBrown,
                    border = Tan,
                )
                Spacer(Modifier.weight(1f))
                SmallEditButton(
                    stringResource(R.string.save),
                    {
                        onSave(text.text, due)
                    },
                    fill = BrandGradient,
                    textColor = Surface,
                    border = BrandBrown.copy(
                        alpha = 0.85f,
                    ),
                    enabled = text.text.isNotBlank(),
                )
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 21.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .shadow(
                            10.dp,
                            RectangleShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.25f),
                            spotColor = Color.Black.copy(alpha = 0.25f),
                        )
                        .background(note.color.color())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { if (it.text.length <= NoteRules.MAX_LENGTH) text = it },
                        modifier = Modifier.fillMaxWidth().weight(1f).focusRequester(focus),
                        textStyle = TextStyle(
                            fontFamily = Poppins,
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            color = TextPrimary,
                        ),
                        cursorBrush = SolidColor(BrandBrown),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                    Text(
                        stringResource(R.string.note_author, authorName),
                        style = TextStyle(fontFamily = Poppins, fontSize = 14.sp, color = TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                }
                TackView(note.tack, Modifier.align(Alignment.TopCenter).offset(y = (-8).dp))
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 21.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val shape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(shape)
                        .background(EditButtonFill)
                        .border(1.dp, Tan, shape)
                        .clickable(role = Role.Button) { calendarOpen = !calendarOpen },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        due?.let { stringResource(R.string.note_change_date, it.shortLabel()) }
                            ?: stringResource(R.string.note_add_due_date),
                        style = RoomieType.pillButton.copy(fontWeight = FontWeight.Medium),
                    )
                }
                if (calendarOpen) {
                    RoomieInlineCalendar(selected = due, today = today, onPick = {
                        due = it
                        calendarOpen = false
                    })
                }
                if (due != null) {
                    Text(
                        stringResource(R.string.note_remove_date),
                        style = RoomieType.pillButton.copy(color = Surface.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) {
                                due = null
                                calendarOpen = false
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallEditButton(
    text: String,
    onClick: () -> Unit,
    fill: Brush,
    textColor: Color,
    border: Color,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .width(96.dp)
            .height(36.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = RoomieType.pillButton.copy(color = textColor, fontWeight = FontWeight.Medium))
    }
}
