package com.example.roomie.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.RoomieRepository
import com.example.roomie.data.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** App-wide wiring: the saved sign-in check, error toasts, and background refreshes. */
@HiltViewModel
class AppViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {
    val restoring: StateFlow<Boolean> = repo.state.map { it.restoring }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, repo.state.value.restoring)
    val messages: Flow<String> = repo.messages
    val session: Session? get() = repo.state.value.session
    fun refresh() = repo.refresh()
}
