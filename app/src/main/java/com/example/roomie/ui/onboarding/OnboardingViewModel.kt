package com.example.roomie.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.Buddy
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.RoomieState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class OnboardingViewModel(private val repo: FakeRoomieRepository = FakeRoomieRepository) : ViewModel() {

    val state: StateFlow<RoomieState> = repo.state.stateIn(viewModelScope, SharingStarted.Eagerly, repo.state.value)

    /** Returns an error message, or null on success. [onSignedIn] gets whether the account already has a house. */
    fun signIn(netId: String, password: String, onSignedIn: (hasHouse: Boolean) -> Unit): String? {
        if (!repo.isValidNetId(netId)) return "Enter your Cornell NetID, like lk452."
        if (password.isBlank()) return "Enter your password."
        return when (val result = repo.signIn(netId, password)) {
            FakeRoomieRepository.SignInResult.Invalid -> "That NetID and password didn't work."
            is FakeRoomieRepository.SignInResult.Success -> {
                onSignedIn(result.hasHouse)
                null
            }
        }
    }

    fun pickBuddy(buddy: Buddy) = repo.setBuddy(buddy)

    fun startHouse() = repo.startHouse()

    fun joinHouse(code: String): String? {
        if (code.isBlank()) return "Enter the invite code from your roommate."
        if (!repo.joinHouse(code)) return "We couldn't find a house with that code."
        return null
    }

    fun invite(netId: String): String? = repo.inviteRoommate(netId)

    fun removeRoommate(id: String) = repo.removeRoommate(id)

    fun renameHouse(name: String) = repo.renameHouse(name)

    fun finish() = repo.finishOnboarding()
}
