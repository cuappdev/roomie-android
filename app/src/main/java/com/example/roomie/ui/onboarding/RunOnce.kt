package com.example.roomie.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Runs [block] in viewModelScope unless [busy] says something is already running.
 *
 * Sign-in, joining, inviting and creating the house wait on the server; screens disable their main
 * button while [busy] so a second tap doesn't send the request twice.
 */
internal fun ViewModel.runOnce(busy: MutableStateFlow<Boolean>, block: suspend () -> Unit) {
    if (busy.value) return
    busy.value = true
    viewModelScope.launch {
        try {
            block()
        } finally {
            busy.value = false
        }
    }
}
