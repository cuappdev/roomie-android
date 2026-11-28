package com.example.roomie.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.MemberStatus
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

data class SetUpHouseUiState(
    val name: String,
    /** Blank until the server creates the house. */
    val inviteCode: String,
    val houseName: String,
    val members: List<Roommate>,
    val meId: String?,
    val hasRoommates: Boolean,
    val error: UiText? = null,
    val busy: Boolean = false,
) {
    val canFinish: Boolean get() = name.isNotBlank() && !busy
}

@HiltViewModel
class SetUpHouseViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {
    /** Null until the user types; until then it's the house's current name. */
    private val name = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<UiText?>(null)
    private val busy = MutableStateFlow(false)

    /** Null until there's a house (or draft house). */
    val ui: StateFlow<SetUpHouseUiState?> = combine(repo.state, name, error, busy) { state, name, error, busy ->
        setUpHouseUiState(state, name)?.copy(error = error, busy = busy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), setUpHouseUiState(repo.state.value, null))

    fun setName(text: String) {
        name.value = text
        repo.renameHouse(text)
    }

    fun finish(onDone: () -> Unit) {
        val current = ui.value ?: return
        error.value = null
        runOnce(busy) {
            val problem = repo.finishOnboarding(current.name)
            if (problem == null) onDone() else error.value = UiText.Raw(problem)
        }
    }
}

internal fun setUpHouseUiState(state: RoomieState, typedName: String?): SetUpHouseUiState? {
    val house = state.house ?: return null
    return SetUpHouseUiState(
        name = typedName ?: house.name,
        inviteCode = house.inviteCode,
        houseName = house.name,
        members = house.members,
        meId = state.session?.userId,
        hasRoommates = house.members.any { it.status != MemberStatus.Owner },
    )
}
