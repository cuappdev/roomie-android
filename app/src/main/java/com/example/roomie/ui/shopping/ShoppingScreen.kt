package com.example.roomie.ui.shopping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.ShoppingItem
import com.example.roomie.data.ShoppingRules
import com.example.roomie.ui.components.InlineAddButton
import com.example.roomie.ui.components.PillButton
import com.example.roomie.ui.components.RoomieCard
import com.example.roomie.ui.components.RoomieCheckbox
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.SettingsAction
import com.example.roomie.ui.shopping.components.EditItemSheet
import com.example.roomie.ui.shopping.components.MatchPanel
import com.example.roomie.ui.shopping.components.label
import com.example.roomie.ui.shopping.components.text
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.DangerPill
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.NoteGreen
import com.example.roomie.ui.theme.PeachPill
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.SnackbarAction
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.agoLabel
import java.time.LocalDateTime

@Composable
fun ShoppingScreen(onOpenSettings: () -> Unit, vm: ShoppingViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val current = ui ?: return
    ShoppingContent(
        ui = current,
        onOpenSettings = onOpenSettings,
        onQueryChange = vm::setQuery,
        onAddQuery = vm::addQuery,
        onMakeMore = vm::makeMore,
        onAddAgain = vm::addAgain,
        onBought = vm::markBought,
        onUndoBought = vm::undoBought,
        onEdit = vm::openEdit,
    )
    current.editing?.let { editing ->
        EditItemSheet(
            ui = editing,
            now = current.now,
            onEvent = vm::onEditEvent,
            onToggleCouldntFind = vm::toggleCouldntFind,
            onDelete = vm::deleteEditing,
            onSave = vm::saveEditing,
            onDismiss = vm::closeEdit,
        )
    }
}

@Composable
private fun ShoppingContent(
    ui: ShoppingUiState,
    onOpenSettings: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onAddQuery: () -> Unit = {},
    onMakeMore: (String) -> Unit = {},
    onAddAgain: (String) -> Unit = {},
    onBought: (ShoppingItem) -> Unit = {},
    onUndoBought: (String) -> Unit = {},
    onEdit: (ShoppingItem) -> Unit = {},
) {
    val focus = LocalFocusManager.current
    val inputFocus = remember { FocusRequester() }
    var showBought by rememberSaveable { mutableStateOf(false) }

    // Each of these clears the box, so the keyboard can go too.
    fun thenHideKeyboard(action: () -> Unit) {
        action()
        focus.clearFocus()
    }

    Box(Modifier.fillMaxSize().background(Background)) {
        Column(Modifier.fillMaxSize()) {
            RoomieHeader(stringResource(R.string.shopping_title)) { SettingsAction(onOpenSettings) }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ui.atStoreName?.let { AtStoreBanner(it) }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoomieTextField(
                        value = ui.query,
                        onValueChange = onQueryChange,
                        placeholder = stringResource(R.string.shopping_add_placeholder),
                        height = 46.dp,
                        corner = 8.dp,
                        modifier = Modifier.weight(1f).focusRequester(inputFocus),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { thenHideKeyboard(onAddQuery) }),
                        contentDescription = stringResource(R.string.shopping_add_placeholder),
                    )
                    InlineAddButton({ thenHideKeyboard(onAddQuery) }, enabled = ui.query.isNotBlank())
                }

                if (ui.query.isNotBlank()) {
                    MatchPanel(
                        query = ui.query.trim(),
                        matches = ui.matches,
                        now = ui.now,
                        onMakeMore = { thenHideKeyboard { onMakeMore(it) } },
                        onAddAgain = { thenHideKeyboard { onAddAgain(it) } },
                        onAddNew = { thenHideKeyboard(onAddQuery) },
                    )
                    Text(
                        stringResource(R.string.shopping_matches_hint, ShoppingRules.MATCH_LOOKBACK_DAYS),
                        style = RoomieType.caption.copy(lineHeight = 17.5.sp),
                    )
                    return@Column
                }

                if (ui.addAgain.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.shopping_add_again),
                            style = RoomieType.caption.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ui.addAgain.forEach { name -> AddAgainChip(name) { onAddAgain(name) } }
                        }
                    }
                }

                if (ui.needed.isEmpty()) {
                    EmptyList(onAddFirst = { inputFocus.requestFocus() })
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.shopping_needed, ui.needed.size),
                            style = RoomieType.sectionTitle,
                            modifier = Modifier.semantics { heading() },
                        )
                        RoomieCard {
                            ui.needed.forEachIndexed { i, needed ->
                                if (i > 0) HorizontalDivider(color = Divider, thickness = 1.dp)
                                NeededRow(
                                    item = needed.item,
                                    subtitle = needed.subtitle.text(ui.now),
                                    onBought = { onBought(needed.item) },
                                    onEdit = { onEdit(needed.item) },
                                )
                            }
                        }
                    }
                }

                if (ui.bought.isNotEmpty()) {
                    BoughtSection(ui.bought, ui.now, showBought, { showBought = !showBought }, onUndoBought)
                }
            }
        }

        AnimatedVisibility(
            visible = ui.lastBought != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        ) {
            val item = ui.lastBought
            if (item !=
                null
            ) {
                UndoSnackbar(stringResource(R.string.shopping_bought_item, item.name)) { onUndoBought(item.id) }
            }
        }
    }
}

@Composable
private fun BoughtSection(
    bought: List<BoughtItem>,
    now: LocalDateTime,
    expanded: Boolean,
    onToggle: () -> Unit,
    onUndo: (String) -> Unit,
) {
    RoomieCard {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(
                        if (expanded) R.string.cd_shopping_hide_bought else R.string.cd_shopping_show_bought,
                    ),
                    onClick = onToggle,
                )
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.shopping_bought, bought.size),
                style = RoomieType.listTitle.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.shopping_clears_in, ShoppingRules.CLEAR_BOUGHT_AFTER_DAYS),
                style = RoomieType.pillButton,
            )
            Icon(
                painterResource(R.drawable.ic_chevron_down),
                null,
                Modifier.size(18.dp).rotate(if (expanded) 180f else 0f),
                tint = Color.Unspecified,
            )
        }
        if (expanded) {
            bought.forEach { entry ->
                HorizontalDivider(color = Divider, thickness = 1.dp)
                BoughtRow(
                    entry.item,
                    stringResource(R.string.shopping_by_detail, entry.by.label(), entry.at.agoLabel(now)),
                ) {
                    onUndo(entry.item.id)
                }
            }
        }
    }
}

@Composable
private fun AtStoreBanner(name: String) {
    val atStore = stringResource(R.string.shopping_at_store, name)
    val stillPickedUp = stringResource(R.string.shopping_at_store_detail)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(NoteGreen)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_store), null, Modifier.size(20.dp), tint = Color.Unspecified)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(atStore) }
                append(stillPickedUp)
            },
            style = RoomieType.body.copy(fontSize = 13.5.sp),
        )
    }
}

@Composable
private fun AddAgainChip(name: String, onClick: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PeachPill)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.cd_shopping_add_again, name),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_plus_chip), null, Modifier.size(13.dp), tint = Color.Unspecified)
        Text(name, style = RoomieType.rowTitle)
    }
}

@Composable
private fun NeededRow(item: ShoppingItem, subtitle: String, onBought: () -> Unit, onEdit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.cd_shopping_edit, item.name),
                onClick = onEdit,
            )
            .padding(start = 7.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoomieCheckbox(false, { onBought() }, stringResource(R.string.cd_shopping_mark_bought, item.name))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(item.name, style = RoomieType.listTitle, maxLines = 2)
            Text(subtitle, style = RoomieType.caption, maxLines = 2)
        }
        when {
            item.couldntFindById != null -> TagPill(
                stringResource(R.string.shopping_couldnt_find_tag),
                DangerPill,
                Danger,
            )
            item.quantity > 1 -> QuantityPill(item.quantity)
        }
    }
}

@Composable
private fun BoughtRow(item: ShoppingItem, subtitle: String, onUndo: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 7.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoomieCheckbox(true, { onUndo() }, stringResource(R.string.cd_shopping_put_back, item.name))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = RoomieType.listTitle.copy(color = TextSecondary, textDecoration = TextDecoration.LineThrough),
            )
            Text(subtitle, style = RoomieType.caption)
        }
        if (item.quantity > 1) QuantityPill(item.quantity)
    }
}

@Composable
private fun QuantityPill(quantity: Int) =
    TagPill(stringResource(R.string.shopping_quantity, quantity), PeachPill, BrandBrown, fontSize = 12f)

@Composable
private fun TagPill(text: String, background: Color, color: Color, fontSize: Float = 11.5f) {
    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(background).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, style = RoomieType.pill.copy(color = color, fontWeight = FontWeight.Bold, fontSize = fontSize.sp))
    }
}

@Composable
private fun EmptyList(onAddFirst: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(painterResource(R.drawable.illus_empty_cart), null, Modifier.size(120.dp), tint = Color.Unspecified)
        Text(
            stringResource(R.string.shopping_empty_title),
            style = RoomieType.titleEmpty,
            modifier = Modifier.semantics {
                heading()
            },
        )
        Text(
            stringResource(R.string.shopping_empty_body),
            style = RoomieType.body.copy(color = TextSecondary, lineHeight = 20.3.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp),
        )
        PillButton(stringResource(R.string.shopping_add_first), onAddFirst, height = 44.dp)
    }
}

@Composable
private fun UndoSnackbar(message: String, onUndo: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(7.dp, RoundedCornerShape(12.dp))
            .background(TextPrimary, RoundedCornerShape(12.dp))
            .padding(start = 16.dp, end = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = RoomieType.body.copy(color = Surface), modifier = Modifier.weight(1f), maxLines = 1)
        Row(
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onUndo)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_undo), null, Modifier.size(16.dp), tint = Color.Unspecified)
            Text(
                stringResource(R.string.shopping_undo),
                style = RoomieType.body.copy(color = SnackbarAction, fontWeight = FontWeight.Bold),
            )
        }
    }
}

private fun previewUi(query: String = "", snackbar: Boolean = false): ShoppingUiState {
    val state = FakeRoomieRepository.demoState()
    val last = if (snackbar) state.shopping.firstOrNull() else null
    return shoppingUiState(state, query, last, null, LocalDateTime.now())!!
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun ShoppingContentPreview() = RoomiePreview {
    ShoppingContent(previewUi(snackbar = true))
}

@Preview(widthDp = 390, heightDp = 600)
@Composable
private fun ShoppingContentTypingPreview() = RoomiePreview {
    ShoppingContent(previewUi(query = "mi"))
}

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun ShoppingContentEmptyPreview() = RoomiePreview {
    ShoppingContent(
        ShoppingUiState(LocalDateTime.now(), "", null, emptyList(), emptyList(), emptyList(), emptyList(), null, null),
    )
}
