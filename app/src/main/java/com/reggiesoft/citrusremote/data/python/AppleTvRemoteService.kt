package com.reggiesoft.citrusremote.data.python

import com.reggiesoft.citrusremote.data.model.AppleTvDevice
import com.reggiesoft.citrusremote.data.model.RemoteCommand

/**
 * Interface defining the interactions with the Python bridge (Chaquopy).
 */
interface AppleTvRemoteService {
    suspend fun scanForDevices(): List<AppleTvDevice>
    suspend fun initiatePairing(deviceIp: String): Pair<Boolean, String>
    suspend fun finishPairing(deviceIp: String, pin: String): Pair<Boolean, String>
    suspend fun validateCredentials(deviceIp: String, credsJson: String): Pair<Boolean, String>
    suspend fun cancelPairing()
    suspend fun sendCommand(deviceIp: String, credsJson: String, command: RemoteCommand): String
}
