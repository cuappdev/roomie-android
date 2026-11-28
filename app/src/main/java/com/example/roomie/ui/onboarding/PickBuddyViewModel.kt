package com.example.roomie.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.Buddy
import com.example.roomie.data.RoomieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PickBuddyUiState(val firstName: String, val picked: Buddy)

@HiltViewModel
class PickBuddyViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {
    /** Null until the user picks; until then it's their current buddy. */
    private val picked = MutableStateFlow<Buddy?>(null)

    /** Null while signed out. */
    val ui: StateFlow<PickBuddyUiState?> = combine(repo.state, picked) { state, picked ->
        state.session?.let { PickBuddyUiState(it.firstName, picked ?: it.buddy) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        repo.state.value.session?.let {
            PickBuddyUiState(it.firstName, it.buddy)
        },
    )

    fun pick(buddy: Buddy) {
        picked.value = buddy
    }

    fun save() {
        ui.value?.let { repo.setBuddy(it.picked) }
    }

    /** Skipping assigns a random buddy; it can be changed any time from Me. */
    fun skip() = repo.setBuddy(Buddy.entries.random())
}
