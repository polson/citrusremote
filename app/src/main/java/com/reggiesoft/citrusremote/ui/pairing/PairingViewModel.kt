package com.reggiesoft.citrusremote.ui.pairing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reggiesoft.citrusremote.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PairingUiState {
    data object Idle : PairingUiState
    data object Initiating : PairingUiState
    data object WaitingForPin : PairingUiState
    data object Pairing : PairingUiState
    data object Success : PairingUiState
    data class Error(
        val message: String,
        val canRetryInitiating: Boolean = false,
        val isLocked: Boolean = false,
        val lockoutSeconds: Long? = null
    ) : PairingUiState
}

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private var currentDeviceIp: String = ""
    private var currentDeviceName: String = ""
    private var lockoutJob: Job? = null

    fun initiatePairing(deviceIp: String, deviceName: String) {
        lockoutJob?.cancel()
        currentDeviceIp = deviceIp
        currentDeviceName = deviceName

        val remainingLockout = repository.getRemainingLockoutSeconds(deviceIp)
        if (remainingLockout > 0L) {
            val error = PairingUiState.Error(
                message = buildLockoutMessage(remainingLockout),
                canRetryInitiating = true,
                isLocked = true,
                lockoutSeconds = remainingLockout
            )
            _uiState.value = error
            startLockoutCountdown(remainingLockout)
            return
        }

        _uiState.value = PairingUiState.Initiating
        viewModelScope.launch {
            val result = repository.initiatePairing(deviceIp)
            if (result.first) {
                repository.clearPairingLockout(deviceIp)
                _uiState.value = PairingUiState.WaitingForPin
            } else {
                handlePairingFailure(deviceIp, result.second)
            }
        }
    }

    fun finishPairing(pin: String) {
        if (_uiState.value !is PairingUiState.WaitingForPin) {
            return
        }

        lockoutJob?.cancel()
        _uiState.value = PairingUiState.Pairing
        viewModelScope.launch {
            val result = repository.finishPairing(currentDeviceIp, pin)
            if (result.first) {
                repository.clearPairingLockout(currentDeviceIp)
                _uiState.value = PairingUiState.Success
            } else {
                handlePairingFailure(currentDeviceIp, result.second)
            }
        }
    }

    fun cancelPairing() {
        lockoutJob?.cancel()
        viewModelScope.launch {
            repository.cancelPairing()
        }
    }

    private fun handlePairingFailure(deviceIp: String, rawError: String) {
        Log.e(TAG, rawError)
        val error = mapPairingError(rawError)
        if (error.isLocked) {
            error.lockoutSeconds?.let {
                repository.setPairingLockout(deviceIp, it)
                startLockoutCountdown(it)
            }
        } else {
            repository.clearPairingLockout(deviceIp)
        }
        _uiState.value = error
    }

    private fun startLockoutCountdown(initialSeconds: Long) {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            var seconds = initialSeconds
            while (seconds > 0L) {
                delay(1000L)
                seconds--
                val currentState = _uiState.value
                if (currentState is PairingUiState.Error && currentState.isLocked) {
                    _uiState.value = currentState.copy(lockoutSeconds = seconds)
                } else {
                    break
                }
            }
        }
    }

    private fun mapPairingError(rawError: String): PairingUiState.Error {
        val normalized = rawError.trim()

        val backOffMatch = BACK_OFF_REGEX.find(normalized)
        if (normalized.contains("Error=BackOff", ignoreCase = true) || backOffMatch != null) {
            val seconds = backOffMatch?.groupValues?.getOrNull(1)?.toLongOrNull()
            return PairingUiState.Error(
                message = buildLockoutMessage(seconds),
                canRetryInitiating = true,
                isLocked = true,
                lockoutSeconds = seconds
            )
        }

        if (normalized.contains("no active pairing session", ignoreCase = true)) {
            return PairingUiState.Error(
                message = "The pairing session is no longer active. Retry the connection, then enter the current PIN from Apple TV.",
                canRetryInitiating = true
            )
        }

        if (
            normalized.contains("timed out", ignoreCase = true) ||
            normalized.contains("not found", ignoreCase = true) ||
            normalized.contains("refused", ignoreCase = true) ||
            normalized.contains("connect call failed", ignoreCase = true) ||
            normalized.contains("unreachable", ignoreCase = true)
        ) {
            return PairingUiState.Error(
                message = "Could not reach Apple TV. Check that both devices are on the same network, then retry.",
                canRetryInitiating = true
            )
        }

        if (normalized.contains("invalid", ignoreCase = true) && normalized.contains("pin", ignoreCase = true)) {
            return PairingUiState.Error(
                message = "That PIN was not accepted. Restart pairing, then enter the current 4-digit code from Apple TV.",
                canRetryInitiating = true
            )
        }

        if (normalized.contains("invalid credentials", ignoreCase = true)) {
            return PairingUiState.Error(
                message = "Pairing did not produce usable credentials for this Apple TV. Restart pairing and try again.",
                canRetryInitiating = true
            )
        }

        if (normalized.contains("companion service is not available", ignoreCase = true)) {
            return PairingUiState.Error(
                message = "This Apple TV did not expose the Companion service CitrusRemote needs for buttons and keyboard input. Make sure Apple TV is awake, on the same network, and then retry pairing.",
                canRetryInitiating = true
            )
        }

        return PairingUiState.Error(
            message = "Companion pairing failed. Retry the connection and try again.",
            canRetryInitiating = true
        )
    }

    private fun buildLockoutMessage(seconds: Long?): String {
        val waitTime = seconds?.let { formatLockoutDuration(it) } ?: "a while"
        return "$currentDeviceName Apple TV has temporarily locked pairing for $waitTime, due to too many attempts. This is an Apple TV Security restriction. Wait for the lockout to expire before trying again."
    }
}

private const val TAG = "PairingViewModel"

private val BACK_OFF_REGEX = Regex("""BackOff=(\d+)s""")

fun formatLockoutDuration(seconds: Long): String {
    if (seconds <= 0L) {
        return "0m 00s"
    }

    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    val remainingSeconds = seconds % 60L
    if (hours > 0L) {
        return String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, remainingSeconds)
    }
    return String.format(Locale.US, "%dm %02ds", minutes, remainingSeconds)
}
