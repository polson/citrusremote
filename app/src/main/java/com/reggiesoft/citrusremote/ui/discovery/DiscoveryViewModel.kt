package com.reggiesoft.citrusremote.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reggiesoft.citrusremote.data.model.AppleTvDevice
import com.reggiesoft.citrusremote.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DiscoveryUiState {
    data object Loading : DiscoveryUiState()
    data class Success(val devices: List<AppleTvDevice>) : DiscoveryUiState()
    data class Error(val message: String) : DiscoveryUiState()
}

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscoveryUiState>(DiscoveryUiState.Loading)
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        scanForDevices()
    }

    fun scanForDevices() {
        _uiState.value = DiscoveryUiState.Loading
        viewModelScope.launch {
            try {
                val devices = repository.scanOnce()
                _uiState.value = DiscoveryUiState.Success(devices)
            } catch (e: Exception) {
                _uiState.value = DiscoveryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun hasUsableCredentials(deviceIp: String): Boolean = repository.hasUsableCredentials(deviceIp)

    fun isPairingLocked(deviceIp: String, nowMillis: Long = System.currentTimeMillis()): Boolean =
        repository.isPairingLocked(deviceIp, nowMillis)
}
