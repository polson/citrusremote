package com.reggiesoft.citrusremote.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.reggiesoft.citrusremote.data.model.AppleTvDevice
import com.reggiesoft.citrusremote.data.model.RemoteCommand
import com.reggiesoft.citrusremote.data.model.StoredCredentials
import com.reggiesoft.citrusremote.data.python.AppleTvRemoteService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val remoteService: AppleTvRemoteService,
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AppleTVPrefs", Context.MODE_PRIVATE)
    companion object {
        private const val LOCKOUT_UNTIL_PREFIX = "pairing_lockout_until_"
        private const val CREDENTIALS_PREFIX = "creds_"
        const val INVALID_CREDENTIALS_MESSAGE =
            "Error: Saved credentials are missing or invalid for this Apple TV. Pair again."
    }

    suspend fun scanOnce(): List<AppleTvDevice> {
        return remoteService.scanForDevices()
    }

    suspend fun initiatePairing(deviceIp: String): Pair<Boolean, String> {
        return remoteService.initiatePairing(deviceIp)
    }

    suspend fun finishPairing(deviceIp: String, pin: String): Pair<Boolean, String> {
        val result = remoteService.finishPairing(deviceIp, pin)
        if (!result.first) {
            Log.e("DeviceRepository", "finishPairing failed for device $deviceIp: ${result.second}")
            return result
        }

        val validation = remoteService.validateCredentials(deviceIp, result.second)
        if (!validation.first) {
            Log.e(
                "DeviceRepository",
                "Credential validation failed after pairing device $deviceIp: ${validation.second}"
            )
            clearCredentials(deviceIp)
            return Pair(false, validation.second.ifBlank { INVALID_CREDENTIALS_MESSAGE })
        }
        saveCredentials(deviceIp, result.second)
        return result
    }

    suspend fun cancelPairing() {
        remoteService.cancelPairing()
    }

    suspend fun sendCommand(deviceIp: String, command: RemoteCommand): String {
        val creds = getCredentials(deviceIp) ?: return INVALID_CREDENTIALS_MESSAGE
        val result = remoteService.sendCommand(deviceIp, creds, command)
        return clearCredentialsIfInvalid(deviceIp, result)
    }

    fun hasUsableCredentials(deviceIp: String): Boolean {
        return getCredentials(deviceIp) != null
    }

    fun clearCredentials(deviceIp: String) {
        prefs.edit().remove(credentialsKey(deviceIp)).apply()
    }

    fun setPairingLockout(deviceIp: String, backOffSeconds: Long) {
        val lockoutUntilMillis = System.currentTimeMillis() + (backOffSeconds * 1000L)
        prefs.edit().putLong("$LOCKOUT_UNTIL_PREFIX$deviceIp", lockoutUntilMillis).apply()
    }

    fun clearPairingLockout(deviceIp: String) {
        prefs.edit().remove("$LOCKOUT_UNTIL_PREFIX$deviceIp").apply()
    }

    fun getRemainingLockoutSeconds(deviceIp: String, nowMillis: Long = System.currentTimeMillis()): Long {
        val lockoutUntilMillis = prefs.getLong("$LOCKOUT_UNTIL_PREFIX$deviceIp", 0L)
        val remainingMillis = lockoutUntilMillis - nowMillis
        if (remainingMillis <= 0L) {
            if (lockoutUntilMillis != 0L) {
                clearPairingLockout(deviceIp)
            }
            return 0L
        }
        return (remainingMillis + 999L) / 1000L
    }

    fun isPairingLocked(deviceIp: String, nowMillis: Long = System.currentTimeMillis()): Boolean =
        getRemainingLockoutSeconds(deviceIp, nowMillis) > 0L

    private fun saveCredentials(deviceIp: String, credsJson: String) {
        val normalized = StoredCredentials.normalize(credsJson)
        prefs.edit().putString(credentialsKey(deviceIp), normalized).apply()
    }

    fun getCredentials(deviceIp: String): String? {
        val stored = prefs.getString(credentialsKey(deviceIp), null) ?: return null
        val normalized = StoredCredentials.getUsableCredentials(deviceIp, stored)
        if (normalized == null) {
            clearCredentials(deviceIp)
            return null
        }
        if (normalized != stored) {
            prefs.edit().putString(credentialsKey(deviceIp), normalized).apply()
        }
        return normalized
    }

    fun clearCredentialsIfInvalid(deviceIp: String, result: String): String {
        if (result.contains("invalid credentials", ignoreCase = true)) {
            clearCredentials(deviceIp)
        }
        return result
    }

    private fun credentialsKey(deviceIp: String): String {
        return "$CREDENTIALS_PREFIX$deviceIp"
    }
}
