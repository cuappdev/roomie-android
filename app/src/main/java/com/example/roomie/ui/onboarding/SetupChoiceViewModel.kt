package com.example.roomie.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.R
import com.example.roomie.data.RoomieRepository
import com.example.roomie.ui.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SetupChoiceUiState(
    val firstName: String,
    val join: Boolean = false,
    val code: String = "",
    val error: UiText? = null,
    val busy: Boolean = false,
)

private data class SetupChoiceForm(val join: Boolean = false, val code: String = "", val error: UiText? = null)

@HiltViewModel
class SetupChoiceViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {
    private val form = MutableStateFlow(SetupChoiceForm())
    private val busy = MutableStateFlow(false)

    /** Null while signed out. */
    val ui: StateFlow<SetupChoiceUiState?> = combine(repo.state, form, busy) { state, form, busy ->
        state.session?.let { SetupChoiceUiState(it.firstName, form.join, form.code, form.error, busy) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        repo.state.value.session?.let {
            SetupChoiceUiState(it.firstName)
        },
    )

    fun chooseStart() = form.update { it.copy(join = false, error = null) }

    fun chooseJoin() = form.update { it.copy(join = true) }

    /** Typing a code picks "Join" too. */
    fun setCode(code: String) = form.update { it.copy(code = code.uppercase(), join = true, error = null) }

    fun next(onStartHouse: () -> Unit, onJoined: () -> Unit) {
        val current = form.value
        if (!current.join) {
            repo.startHouse()
            return onStartHouse()
        }
        if (current.code.isBlank()) return form.update { it.copy(error = UiText.Res(R.string.setup_no_code)) }
        form.update { it.copy(error = null) }
        runOnce(busy) {
            val problem = repo.joinHouse(current.code)
            if (problem == null) onJoined() else form.update { it.copy(error = UiText.Raw(problem)) }
        }
    }
}
