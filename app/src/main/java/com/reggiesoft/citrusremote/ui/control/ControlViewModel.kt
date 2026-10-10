package com.reggiesoft.citrusremote.ui.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reggiesoft.citrusremote.data.model.RemoteCommand
import com.reggiesoft.citrusremote.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ControlUiState {
    data object Ready : ControlUiState
    data object Connecting : ControlUiState
    data class Result(val message: String) : ControlUiState
    data class Error(val message: String) : ControlUiState
}

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ControlUiState>(ControlUiState.Ready)
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()
    private var resetJob: Job? = null

    fun forgetDevice(deviceIp: String) {
        resetJob?.cancel()
        repository.clearCredentials(deviceIp)
        _uiState.value = ControlUiState.Ready
    }

    fun sendCommand(deviceIp: String, command: RemoteCommand) {
        resetJob?.cancel()
        _uiState.value = ControlUiState.Connecting
        viewModelScope.launch {
            val result = repository.sendCommand(deviceIp, command)
            if (result.startsWith("Error:")) {
                _uiState.value = ControlUiState.Error(result)
            } else {
                _uiState.value = ControlUiState.Result(result)
            }

            resetJob?.cancel()
            resetJob = launch {
                delay(1_500)
                _uiState.value = ControlUiState.Ready
            }
        }
    }
}
