package com.example.roomie.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.RoomieRepository
import com.example.roomie.data.RoomieState
import com.example.roomie.data.Roommate
import com.example.roomie.ui.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class AddRoommatesUiState(
    /** Everyone but you, with their invite status. */
    val roommates: List<Roommate>,
    val netId: String = "",
    val error: UiText? = null,
    val busy: Boolean = false,
)

private data class InviteForm(val netId: String = "", val error: UiText? = null)

@HiltViewModel
class AddRoommatesViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {
    private val form = MutableStateFlow(InviteForm())
    private val busy = MutableStateFlow(false)

    /** Null until there's a house (or draft house) to add people to. */
    val ui: StateFlow<AddRoommatesUiState?> = combine(repo.state, form, busy) { state, form, busy ->
        roommatesOf(state)?.let { AddRoommatesUiState(it, form.netId, form.error, busy) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        roommatesOf(repo.state.value)?.let {
            AddRoommatesUiState(it)
        },
    )

    fun setNetId(netId: String) = form.update { InviteForm(netId.trim()) }

    fun invite() {
        val sent = form.value.netId
        if (sent.isBlank()) return
        runOnce(busy) {
            val problem = repo.inviteRoommate(sent)
            form.update {
                when {
                    problem != null -> it.copy(error = UiText.Raw(problem))
                    // Clear the box unless they've already started typing the next one.
                    it.netId == sent -> InviteForm()
                    else -> it
                }
            }
        }
    }

    fun remove(id: String) = repo.removeRoommate(id)
}

internal fun roommatesOf(state: RoomieState): List<Roommate>? {
    val house = state.house ?: return null
    return house.members.filter { it.id != state.session?.userId }
}
