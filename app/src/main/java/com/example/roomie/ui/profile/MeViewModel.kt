package com.example.roomie.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.Buddy
import com.example.roomie.data.House
import com.example.roomie.data.RoomieRepository
import com.example.roomie.data.RoomieState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MeUiState(val fullName: String, val netId: String, val buddy: Buddy, val meId: String, val house: House)

@HiltViewModel
class MeViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {

    /** Null while signed out or without a house. */
    val ui: StateFlow<MeUiState?> = repo.state
        .map(::meUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), meUiState(repo.state.value))

    fun renameHouse(name: String) = repo.renameHouse(name)

    fun signOut() = repo.signOut()

    fun leaveHouse() = repo.leaveHouse()
}

internal fun meUiState(state: RoomieState): MeUiState? {
    val session = state.session ?: return null
    val house = state.house ?: return null
    return MeUiState(
        fullName = listOf(session.firstName, session.lastName).filter { it.isNotBlank() }.joinToString(" "),
        netId = session.netId,
        buddy = session.buddy,
        meId = session.userId,
        house = house,
    )
}
