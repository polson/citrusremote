package com.reggiesoft.citrusremote.data.repository

import com.reggiesoft.citrusremote.data.python.KeyboardInputBridge
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyboardInputRepository @Inject constructor(
    private val keyboardBridge: KeyboardInputBridge,
    private val deviceRepository: DeviceRepository
) {
    suspend fun getKeyboardText(deviceIp: String): String = runWithCredentials(deviceIp) { creds ->
        keyboardBridge.getKeyboardText(deviceIp, creds)
    }

    suspend fun setKeyboardText(deviceIp: String, text: String): String = runWithCredentials(deviceIp) { creds ->
        keyboardBridge.setKeyboardText(deviceIp, creds, text)
    }

    suspend fun clearKeyboardText(deviceIp: String): String = runWithCredentials(deviceIp) { creds ->
        keyboardBridge.clearKeyboardText(deviceIp, creds)
    }

    suspend fun submitKeyboardText(deviceIp: String, text: String): String = runWithCredentials(deviceIp) { creds ->
        keyboardBridge.submitKeyboardText(deviceIp, creds, text)
    }

    private suspend fun runWithCredentials(
        deviceIp: String,
        action: suspend (String) -> String
    ): String {
        val creds = deviceRepository.getCredentials(deviceIp)
        if (creds == null) {
            return DeviceRepository.INVALID_CREDENTIALS_MESSAGE
        }
        val result = action(creds)
        return deviceRepository.clearCredentialsIfInvalid(deviceIp, result)
    }
}
