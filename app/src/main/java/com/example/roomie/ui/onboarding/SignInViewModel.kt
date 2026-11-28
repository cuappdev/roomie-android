package com.example.roomie.ui.onboarding

import androidx.lifecycle.ViewModel
import com.example.roomie.R
import com.example.roomie.data.RoomieRepository
import com.example.roomie.ui.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SignInUiState(
    val netId: String = "",
    val password: String = "",
    val error: UiText? = null,
    val busy: Boolean = false,
)

@HiltViewModel
class SignInViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {
    private val _ui = MutableStateFlow(SignInUiState())
    val ui: StateFlow<SignInUiState> = _ui.asStateFlow()
    private val busy = MutableStateFlow(false)

    fun setNetId(netId: String) = _ui.update { it.copy(netId = netId.trim(), error = null) }

    fun setPassword(password: String) = _ui.update { it.copy(password = password, error = null) }

    /** [onSignedIn] gets whether the account already has a house. */
    fun signIn(onSignedIn: (hasHouse: Boolean) -> Unit) {
        val form = _ui.value
        val problem = when {
            !repo.isValidNetId(form.netId) -> R.string.sign_in_bad_net_id
            form.password.isBlank() -> R.string.sign_in_no_password
            else -> null
        }
        if (problem != null) return _ui.update { it.copy(error = UiText.Res(problem)) }
        if (busy.value) return
        _ui.update { it.copy(error = null, busy = true) }
        runOnce(busy) {
            when (val result = repo.signIn(form.netId, form.password)) {
                is RoomieRepository.SignInResult.Invalid -> _ui.update { it.copy(error = UiText.Raw(result.message)) }
                is RoomieRepository.SignInResult.Success -> onSignedIn(result.hasHouse)
            }
            _ui.update { it.copy(busy = false) }
        }
    }
}
