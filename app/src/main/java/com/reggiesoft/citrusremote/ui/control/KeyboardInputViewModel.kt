package com.reggiesoft.citrusremote.ui.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reggiesoft.citrusremote.data.repository.KeyboardInputRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KeyboardInputUiState(
    val text: String = "",
    val isLoading: Boolean = false,
    val status: String = "Open a text field on Apple TV to start typing."
)

@HiltViewModel
class KeyboardInputViewModel @Inject constructor(
    private val repository: KeyboardInputRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeyboardInputUiState())
    val uiState: StateFlow<KeyboardInputUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null
    private var hasLocalEdits = false
    private var activeDeviceIp: String? = null

    fun loadKeyboard(deviceIp: String) {
        syncJob?.cancel()
        hasLocalEdits = false
        activeDeviceIp = deviceIp
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            status = "Checking Apple TV text input..."
        )

        viewModelScope.launch {
            val result = repository.getKeyboardText(deviceIp)
            if (result.startsWith("Error:")) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    status = result.cleanErrorMessage().ifBlank {
                        "Open a text field on Apple TV to start typing."
                    }
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    text = if (hasLocalEdits) {
                        _uiState.value.text
                    } else {
                        result
                    },
                    isLoading = false,
                    status = if (result.isBlank()) {
                        "Type here and it will sync to Apple TV."
                    } else {
                        "Editing live text on Apple TV."
                    }
                )
            }
        }
    }

    fun updateText(deviceIp: String, text: String) {
        hasLocalEdits = true
        activeDeviceIp = deviceIp
        _uiState.value = _uiState.value.copy(
            text = text,
            isLoading = false,
            status = "Syncing to Apple TV..."
        )

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(KEYBOARD_SYNC_DEBOUNCE_MILLIS)
            val result = repository.setKeyboardText(deviceIp, text)
            val statusMessage = when {
                result.startsWith("Error:") -> result.cleanErrorMessage()
                text.isBlank() -> "Apple TV text cleared."
                else -> "Typing on Apple TV."
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                status = statusMessage
            )
        }
    }

    fun clearText(deviceIp: String) {
        syncJob?.cancel()
        hasLocalEdits = true
        activeDeviceIp = deviceIp
        _uiState.value = _uiState.value.copy(
            text = "",
            isLoading = true,
            status = "Clearing Apple TV text..."
        )

        viewModelScope.launch {
            val result = repository.clearKeyboardText(deviceIp)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                status = if (result.startsWith("Error:")) {
                    result.cleanErrorMessage()
                } else {
                    "Apple TV text cleared."
                }
            )
        }
    }

    fun submit(deviceIp: String) {
        syncJob?.cancel()
        val currentText = _uiState.value.text
        hasLocalEdits = true
        activeDeviceIp = deviceIp
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            status = "Advancing on Apple TV..."
        )

        viewModelScope.launch {
            val result = repository.submitKeyboardText(deviceIp, currentText)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                status = if (result.startsWith("Error:")) {
                    result.cleanErrorMessage()
                } else {
                    "Moved to the next Apple TV field."
                }
            )
        }
    }

    fun closeKeyboard(deviceIp: String) {
        syncJob?.cancel()
        val targetIp = deviceIp.ifBlank { activeDeviceIp.orEmpty() }
        if (targetIp.isNotBlank()) {
            viewModelScope.launch {
                repository.closeKeyboardSession(targetIp)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
        activeDeviceIp?.let { ip ->
            closeKeyboard(ip)
        }
    }
}

private const val KEYBOARD_SYNC_DEBOUNCE_MILLIS = 50L

private fun String.cleanErrorMessage(): String = removePrefix("Error: ").trim()
