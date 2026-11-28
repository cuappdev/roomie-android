package com.example.roomie.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.RoomieRepository
import com.example.roomie.data.RoomieState
import com.example.roomie.data.ShoppingItem
import com.example.roomie.data.ShoppingRules
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Who did something on the list. The screen turns it into "You", a first name, or "Someone". */
sealed interface Person {
    data object Me : Person

    data class Named(val name: String) : Person

    /** Someone who has left the house. */
    data object Unknown : Person
}

/** The grey line under a needed item. */
sealed interface ItemSubtitle {
    /** "Mina couldn't find it". */
    data class CouldntFind(val by: Person) : ItemSubtitle

    /** "Mina · Oat milk, the blue one". */
    data class WithNote(val by: Person, val note: String) : ItemSubtitle

    /** "Mina", or "Mina · 3 days ago" once it's been on the list a couple of days ([addedAt] set). */
    data class Added(val by: Person, val addedAt: LocalDateTime?) : ItemSubtitle
}

data class NeededItem(val item: ShoppingItem, val subtitle: ItemSubtitle)

data class BoughtItem(val item: ShoppingItem, val by: Person, val at: LocalDateTime)

/** A duplicate check result while typing a new item. */
sealed interface MatchItem {
    val item: ShoppingItem

    /** Already needed: "Make it ×3". */
    data class OnList(override val item: ShoppingItem) : MatchItem

    /** Bought lately: "Add again". */
    data class RecentlyBought(override val item: ShoppingItem, val by: Person, val at: LocalDateTime) : MatchItem
}

/** What's been changed in the Edit item sheet so far. */
data class ItemDraft(val id: String, val name: String, val quantity: Int, val note: String)

data class EditItemUiState(
    val draft: ItemDraft,
    val addedBy: Person,
    val addedAt: LocalDateTime,
    /** Null when nobody has marked it; otherwise who did. */
    val couldntFindBy: Person?,
    val showNote: Boolean,
    val showCouldntFind: Boolean,
    val canSave: Boolean,
)

data class ShoppingUiState(
    val now: LocalDateTime,
    val query: String,
    /** A roommate at the store, shown while not typing. */
    val atStoreName: String?,
    val matches: List<MatchItem>,
    val addAgain: List<String>,
    val needed: List<NeededItem>,
    val bought: List<BoughtItem>,
    /** Just bought, for the Undo snackbar. */
    val lastBought: ShoppingItem?,
    val editing: EditItemUiState?,
)

sealed interface EditItemEvent {
    data class NameChanged(val name: String) : EditItemEvent

    data class NoteChanged(val note: String) : EditItemEvent

    data object Increase : EditItemEvent

    data object Decrease : EditItemEvent
}

@HiltViewModel
class ShoppingViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val lastBought = MutableStateFlow<ShoppingItem?>(null)
    private val draft = MutableStateFlow<ItemDraft?>(null)
    private var snackbarTimer: Job? = null

    /** Null while there's no house to show. */
    val ui: StateFlow<ShoppingUiState?> = combine(repo.state, query, lastBought, draft) { state, query, last, draft ->
        shoppingUiState(state, query, last, draft, repo.now())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        shoppingUiState(repo.state.value, "", null, null, repo.now()),
    )

    fun setQuery(text: String) {
        query.value = text
    }

    /** Adds what's typed as a new item. */
    fun addQuery() {
        val name = query.value.trim()
        if (name.isEmpty()) return
        repo.addItem(name)
        query.value = ""
    }

    fun makeMore(id: String) {
        repo.incrementQuantity(id)
        query.value = ""
    }

    fun addAgain(name: String) {
        repo.addItem(name)
        query.value = ""
    }

    fun markBought(item: ShoppingItem) {
        repo.markBought(item.id)
        lastBought.value = item
        snackbarTimer?.cancel()
        snackbarTimer = viewModelScope.launch {
            delay(UNDO_FOR_MS)
            lastBought.value = null
        }
    }

    fun undoBought(id: String) {
        repo.undoBought(id)
        if (lastBought.value?.id == id) lastBought.value = null
    }

    fun openEdit(item: ShoppingItem) {
        draft.value = ItemDraft(item.id, item.name, item.quantity, item.note)
    }

    fun closeEdit() {
        draft.value = null
    }

    fun onEditEvent(event: EditItemEvent) = draft.update { it?.reduce(event) }

    fun toggleCouldntFind() {
        draft.value?.let { repo.toggleCouldntFind(it.id) }
    }

    fun deleteEditing() {
        draft.value?.let { repo.deleteItem(it.id) }
        closeEdit()
    }

    fun saveEditing() {
        draft.value?.let { repo.saveItem(it.id, it.name, it.quantity, it.note) }
        closeEdit()
    }

    private companion object {
        const val UNDO_FOR_MS = 4_000L
    }
}

internal fun ItemDraft.reduce(event: EditItemEvent): ItemDraft = when (event) {
    is EditItemEvent.NameChanged -> copy(name = event.name)
    is EditItemEvent.NoteChanged -> copy(note = event.note)
    EditItemEvent.Increase -> copy(quantity = quantity + 1)
    EditItemEvent.Decrease -> copy(quantity = (quantity - 1).coerceAtLeast(1))
}

internal fun person(state: RoomieState, id: String?): Person = when {
    id != null && id == state.session?.userId -> Person.Me
    else -> state.member(id)?.firstName?.let { Person.Named(it) } ?: Person.Unknown
}

internal fun subtitleFor(state: RoomieState, item: ShoppingItem, now: LocalDateTime): ItemSubtitle {
    val couldntFind = item.couldntFindById
    if (couldntFind != null) return ItemSubtitle.CouldntFind(person(state, couldntFind))
    val by = person(state, item.addedById)
    return when {
        item.note.isNotBlank() -> ItemSubtitle.WithNote(by, item.note)
        Duration.between(item.addedAt, now).toDays() >= 2 -> ItemSubtitle.Added(by, item.addedAt)
        else -> ItemSubtitle.Added(by, null)
    }
}

internal fun shoppingUiState(
    state: RoomieState,
    query: String,
    lastBought: ShoppingItem?,
    draft: ItemDraft?,
    now: LocalDateTime,
): ShoppingUiState? {
    val house = state.house ?: return null
    val me = state.session?.userId
    val editing = draft?.let { d ->
        state.shopping.firstOrNull { it.id == d.id }?.let { editItemUiState(state, it, d) }
    }
    return ShoppingUiState(
        now = now,
        query = query,
        atStoreName = house.atStoreId?.takeIf { it != me && query.isBlank() }?.let { state.member(it)?.firstName },
        matches = ShoppingRules.matches(query, state.shopping, now).map { match ->
            when (match) {
                is ShoppingRules.Match.OnList -> MatchItem.OnList(match.item)
                is ShoppingRules.Match.RecentlyBought ->
                    MatchItem.RecentlyBought(match.item, person(state, match.item.boughtById), match.item.boughtAt!!)
            }
        },
        addAgain = ShoppingRules.addAgain(state.shopping, now),
        needed = ShoppingRules.needed(state.shopping).map { NeededItem(it, subtitleFor(state, it, now)) },
        bought = ShoppingRules.bought(state.shopping, now).map {
            BoughtItem(it, person(state, it.boughtById), it.boughtAt!!)
        },
        lastBought = lastBought,
        editing = editing,
    )
}

internal fun editItemUiState(state: RoomieState, item: ShoppingItem, draft: ItemDraft) = EditItemUiState(
    draft = draft,
    addedBy = person(state, item.addedById),
    addedAt = item.addedAt,
    couldntFindBy = item.couldntFindById?.let { person(state, it) },
    showNote = state.capabilities.itemNotes,
    showCouldntFind = state.capabilities.couldntFind,
    canSave = draft.name.isNotBlank(),
)
