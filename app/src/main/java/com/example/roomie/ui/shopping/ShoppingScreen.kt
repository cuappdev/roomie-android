package com.example.roomie.ui.shopping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.RoomieState
import com.example.roomie.data.ShoppingItem
import com.example.roomie.data.ShoppingRules
import com.example.roomie.ui.components.DangerButton
import com.example.roomie.ui.components.InlineAddButton
import com.example.roomie.ui.components.LabeledField
import com.example.roomie.ui.components.PillButton
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieBottomSheet
import com.example.roomie.ui.components.RoomieCard
import com.example.roomie.ui.components.RoomieCheckbox
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.SecondaryButton
import com.example.roomie.ui.components.SettingsAction
import com.example.roomie.ui.components.StatusPill
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.DangerPill
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.NoteGreen
import com.example.roomie.ui.theme.PeachPill
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.SnackbarAction
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.agoLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDateTime

class ShoppingViewModel(private val repo: FakeRoomieRepository = FakeRoomieRepository) : ViewModel() {
    val state: StateFlow<RoomieState> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)
    fun now(): LocalDateTime = repo.now()
    fun add(name: String) = repo.addItem(name)
    fun increment(id: String) = repo.incrementQuantity(id)
    fun bought(id: String) = repo.markBought(id)
    fun undoBought(id: String) = repo.undoBought(id)
    fun toggleCouldntFind(id: String) = repo.toggleCouldntFind(id)
    fun save(id: String, name: String, qty: Int, note: String) = repo.saveItem(id, name, qty, note)
    fun delete(id: String) = repo.deleteItem(id)
}

@Composable
fun ShoppingScreen(onOpenSettings: () -> Unit, vm: ShoppingViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val house = state.house ?: return
    val me = state.session?.userId
    val now = vm.now()
    val focus = LocalFocusManager.current
    val inputFocus = remember { FocusRequester() }
    var query by rememberSaveable { mutableStateOf("") }
    var showBought by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastBought by remember { mutableStateOf<ShoppingItem?>(null) }
    fun name(id: String?) = if (id == me) "You" else state.member(id)?.firstName ?: "Someone"
    fun addQuery() {
        if (query.isBlank()) return
        vm.add(query)
        query = ""
        focus.clearFocus()
    }

    LaunchedEffect(lastBought) {
        if (lastBought != null) {
            delay(4_000)
            lastBought = null
        }
    }

    val needed = ShoppingRules.needed(state.shopping)
    val bought = ShoppingRules.bought(state.shopping, now)
    val matches = ShoppingRules.matches(query, state.shopping, now)
    val chips = ShoppingRules.addAgain(state.shopping, now)

    Box(Modifier.fillMaxSize().background(Background)) {
        Column(Modifier.fillMaxSize()) {
            RoomieHeader("Shopping list") { SettingsAction(onOpenSettings) }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val shopper = house.atStoreId?.takeIf { it != me }?.let { state.member(it) }
                if (shopper != null && query.isBlank()) AtStoreBanner(shopper.firstName)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoomieTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Add an item",
                        height = 46.dp,
                        corner = 8.dp,
                        modifier = Modifier.weight(1f).focusRequester(inputFocus),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addQuery() }),
                        contentDescription = "Add an item",
                    )
                    InlineAddButton(::addQuery, enabled = query.isNotBlank())
                }

                if (query.isNotBlank()) {
                    MatchPanel(
                        query = query.trim(),
                        matches = matches,
                        now = now,
                        name = ::name,
                        onMakeMore = { vm.increment(it.id); query = ""; focus.clearFocus() },
                        onAddAgain = { vm.add(it.name); query = ""; focus.clearFocus() },
                        onAddNew = ::addQuery,
                    )
                    Text(
                        "Matches look at the current list and the last 30 days, so nobody buys the same thing twice.",
                        style = RoomieType.caption.copy(lineHeight = 17.5.sp),
                    )
                    return@Column
                }

                if (chips.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Add again", style = RoomieType.caption.copy(fontWeight = FontWeight.SemiBold))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            chips.forEach { chip -> AddAgainChip(chip) { vm.add(chip) } }
                        }
                    }
                }

                if (needed.isEmpty()) {
                    EmptyList(onAddFirst = { inputFocus.requestFocus() })
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Needed · ${needed.size}", style = RoomieType.sectionTitle, modifier = Modifier.semantics { heading() })
                        RoomieCard {
                            needed.forEachIndexed { i, item ->
                                if (i > 0) HorizontalDivider(color = Divider, thickness = 1.dp)
                                NeededRow(
                                    item = item,
                                    subtitle = subtitleFor(item, now, ::name),
                                    onBought = { vm.bought(item.id); lastBought = item },
                                    onEdit = { editingId = item.id },
                                )
                            }
                        }
                    }
                }

                if (bought.isNotEmpty()) {
                    RoomieCard {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable(role = Role.Button, onClickLabel = if (showBought) "Hide bought items" else "Show bought items") { showBought = !showBought }
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Bought · ${bought.size}", style = RoomieType.listTitle.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                            Text("Clears in ${ShoppingRules.CLEAR_BOUGHT_AFTER_DAYS} days", style = RoomieType.pillButton)
                            Icon(
                                painterResource(R.drawable.ic_chevron_down),
                                null,
                                Modifier.size(18.dp).rotate(if (showBought) 180f else 0f),
                                tint = Color.Unspecified,
                            )
                        }
                        if (showBought) {
                            bought.forEach { item ->
                                HorizontalDivider(color = Divider, thickness = 1.dp)
                                BoughtRow(item, "${name(item.boughtById)} · ${item.boughtAt!!.agoLabel(now)}") { vm.undoBought(item.id) }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = lastBought != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        ) {
            val item = lastBought
            if (item != null) UndoSnackbar("Bought ${item.name}") { vm.undoBought(item.id); lastBought = null }
        }
    }

    val editing = state.shopping.firstOrNull { it.id == editingId }
    if (editing != null) {
        EditItemSheet(
            item = editing,
            addedLine = "Added by ${name(editing.addedById).let { if (it == "You") "you" else it }} · ${editing.addedAt.agoLabel(now)}",
            couldntFindMine = editing.couldntFindById == me,
            onToggleCouldntFind = { vm.toggleCouldntFind(editing.id) },
            onDelete = { vm.delete(editing.id); editingId = null },
            onSave = { n, q, note -> vm.save(editing.id, n, q, note); editingId = null },
            onDismiss = { editingId = null },
        )
    }
}

private fun subtitleFor(item: ShoppingItem, now: LocalDateTime, name: (String?) -> String): String {
    if (item.couldntFindById != null) {
        val who = name(item.couldntFindById)
        return if (who == "You") "You couldn't find it" else "$who couldn't find it"
    }
    val who = name(item.addedById)
    return when {
        item.note.isNotBlank() -> "$who · ${item.note}"
        Duration.between(item.addedAt, now).toDays() >= 2 -> "$who · ${item.addedAt.agoLabel(now)}"
        else -> who
    }
}

@Composable
private fun AtStoreBanner(name: String) {
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
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("$name is at the store.") }
                append(" New items still get picked up.")
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
            .clickable(role = Role.Button, onClickLabel = "Add $name again", onClick = onClick)
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
            .clickable(role = Role.Button, onClickLabel = "Edit ${item.name}", onClick = onEdit)
            .padding(start = 7.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoomieCheckbox(false, { onBought() }, "Mark ${item.name} bought")
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(item.name, style = RoomieType.listTitle, maxLines = 2)
            Text(subtitle, style = RoomieType.caption, maxLines = 2)
        }
        when {
            item.couldntFindById != null -> TagPill("Couldn't find", DangerPill, Danger)
            item.quantity > 1 -> TagPill("×${item.quantity}", PeachPill, BrandBrown)
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
        RoomieCheckbox(true, { onUndo() }, "${item.name} bought, tap to put back on the list")
        Column(Modifier.weight(1f)) {
            Text(item.name, style = RoomieType.listTitle.copy(color = TextSecondary, textDecoration = TextDecoration.LineThrough))
            Text(subtitle, style = RoomieType.caption)
        }
        if (item.quantity > 1) TagPill("×${item.quantity}", PeachPill, BrandBrown)
    }
}

@Composable
private fun TagPill(text: String, background: Color, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(background).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, style = RoomieType.pill.copy(color = color, fontWeight = FontWeight.Bold, fontSize = if (text.startsWith("×")) 12.sp else 11.5.sp))
    }
}

@Composable
private fun MatchPanel(
    query: String,
    matches: List<ShoppingRules.Match>,
    now: LocalDateTime,
    name: (String?) -> String,
    onMakeMore: (ShoppingItem) -> Unit,
    onAddAgain: (ShoppingItem) -> Unit,
    onAddNew: () -> Unit,
) {
    RoomieCard {
        matches.forEach { match ->
            val item = match.item
            when (match) {
                is ShoppingRules.Match.OnList -> Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp).background(PeachPill).padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = RoomieType.listTitle.copy(fontWeight = FontWeight.SemiBold))
                        Text("Already on the list · ×${item.quantity}", style = RoomieType.caption.copy(color = Danger, fontWeight = FontWeight.SemiBold))
                    }
                    Box(
                        Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Surface)
                            .border(1.dp, BrandBrown, RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { onMakeMore(item) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Make it ×${item.quantity + 1}", style = RoomieType.pillButton.copy(fontWeight = FontWeight.Bold))
                    }
                }
                is ShoppingRules.Match.RecentlyBought -> Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = RoomieType.listTitle)
                        val by = name(item.boughtById).let { if (it == "You") "you" else it }
                        Text("Bought ${item.boughtAt!!.agoLabel(now)} by $by", style = RoomieType.caption)
                    }
                    Box(
                        Modifier.height(44.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { onAddAgain(item) }.padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Add again", style = RoomieType.pillButton.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
            HorizontalDivider(color = Divider, thickness = 1.dp)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.Button, onClick = onAddNew)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_plus), null, Modifier.size(18.dp), tint = Color.Unspecified)
            Text("Add “$query” as a new item", style = RoomieType.listTitle.copy(fontWeight = FontWeight.Normal))
        }
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
        Text("Nothing on the list", style = RoomieType.titleEmpty, modifier = Modifier.semantics { heading() })
        Text(
            "Add things as you run out. Everyone sees them right away, so no one buys it twice.",
            style = RoomieType.body.copy(color = TextSecondary, lineHeight = 20.3.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp),
        )
        PillButton("Add first item", onAddFirst, height = 44.dp)
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
            Modifier.height(44.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onUndo).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_undo), null, Modifier.size(16.dp), tint = Color.Unspecified)
            Text("Undo", style = RoomieType.body.copy(color = SnackbarAction, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun EditItemSheet(
    item: ShoppingItem,
    addedLine: String,
    couldntFindMine: Boolean,
    onToggleCouldntFind: () -> Unit,
    onDelete: () -> Unit,
    onSave: (String, Int, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var qty by rememberSaveable(item.id) { mutableStateOf(item.quantity) }
    var note by rememberSaveable(item.id) { mutableStateOf(item.note) }

    RoomieBottomSheet(title = "Edit item", actionLabel = "Close", onDismiss = onDismiss) {
        LabeledField("Item") {
            RoomieTextField(name, { name = it }, placeholder = "Item name", contentDescription = "Item")
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Quantity", style = RoomieType.sectionTitle, modifier = Modifier.weight(1f))
            StepperButton(R.drawable.ic_minus, "Decrease quantity", enabled = qty > 1) { qty-- }
            Text("$qty", style = RoomieType.input.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), modifier = Modifier.widthIn(min = 28.dp), textAlign = TextAlign.Center)
            StepperButton(R.drawable.ic_plus_stepper, "Increase quantity", enabled = true) { qty++ }
        }
        LabeledField("Note · optional") {
            RoomieTextField(note, { note = it }, placeholder = "Brand, size, where to find it", contentDescription = "Note")
        }
        Text(addedLine, style = RoomieType.caption)
        SecondaryButton(
            if (item.couldntFindById == null) "Couldn't find it" else if (couldntFindMine) "Found it after all" else "Clear “couldn't find”",
            onToggleCouldntFind,
            icon = R.drawable.ic_store_18,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DangerButton("Delete", onDelete, Modifier.weight(1f), icon = R.drawable.ic_trash)
            PrimaryButton("Save", { onSave(name, qty, note) }, Modifier.weight(1f), enabled = name.isNotBlank(), height = 46.dp)
        }
    }
}

@Composable
private fun StepperButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .background(Surface)
            .border(1.dp, Border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, Modifier.size(18.dp), tint = if (enabled) Color.Unspecified else Border)
    }
}
