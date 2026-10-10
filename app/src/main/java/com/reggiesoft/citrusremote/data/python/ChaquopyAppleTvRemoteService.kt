package com.reggiesoft.citrusremote.data.python

import android.util.Log
import com.chaquo.python.Python
import com.reggiesoft.citrusremote.data.model.AppleTvDevice
import com.reggiesoft.citrusremote.data.model.RemoteCommand
import com.reggiesoft.citrusremote.data.model.StoredCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChaquopyAppleTvRemoteService @Inject constructor() : AppleTvRemoteService {

    private val python by lazy { Python.getInstance() }
    private val module by lazy { python.getModule("appletv_remote") }

    private fun normalizePythonResult(result: String): String =
        if (result == "Error:" || result == "Error: ") {
            "Error: Apple TV command failed without a detailed message."
        } else {
            result
        }

    override suspend fun scanForDevices(): List<AppleTvDevice> = withContext(Dispatchers.IO) {
        try {
            val resultJson = module.callAttr("scan_for_devices").toString()
            JSONArray(resultJson).toDeviceList()
        } catch (e: Exception) {
            Log.e(TAG, "Exception scanning for devices", e)
            emptyList()
        }
    }

    override suspend fun initiatePairing(deviceIp: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val resultStr = module.callAttr("initiate_pairing", deviceIp).toString()
            JSONObject(resultStr).toStatusPair()
        } catch (e: Exception) {
            Pair(false, e.message ?: "Unknown error")
        }
    }

    override suspend fun finishPairing(deviceIp: String, pin: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val resultStr = module.callAttr("finish_pairing", deviceIp, pin).toString()
            val json = JSONObject(resultStr)
            val success = json.optString("status") == "success"
            if (success) {
                Pair(
                    true,
                    StoredCredentials.normalize(json.getJSONObject("credentials").toString())
                )
            } else {
                val errorMsg = json.optString("message", "")
                Log.e(TAG, "finishPairing failed: $errorMsg")
                Pair(false, errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in finishPairing for '$deviceIp'", e)
            Pair(false, e.message ?: "Unknown error")
        }
    }

    override suspend fun validateCredentials(deviceIp: String, credsJson: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val resultStr = module.callAttr("validate_credentials", deviceIp, credsJson).toString()
            JSONObject(resultStr).toStatusPair()
        } catch (e: Exception) {
            Log.e(TAG, "Exception validating credentials for '$deviceIp'", e)
            Pair(false, e.message ?: "Unknown error")
        }
    }

    override suspend fun cancelPairing(): Unit = withContext(Dispatchers.IO) {
        try {
            module.callAttr("cancel_pairing")
        } catch (e: Exception) {
            Log.e(TAG, "Exception on cancel", e)
        }
    }

    override suspend fun sendCommand(deviceIp: String, credsJson: String, command: RemoteCommand): String = withContext(Dispatchers.IO) {
        try {
            val result = module.callAttr("send_command", deviceIp, credsJson, command.command)
            val resultString = normalizePythonResult(result.toString())
            if (resultString.startsWith("Error:")) {
                Log.e(TAG, resultString)
            }
            resultString
        } catch (e: Exception) {
            Log.e(TAG, "Exception executing Python command '${command.command}'", e)
            "Error: ${e.message}"
        }
    }
}

private const val TAG = "AppleTVRemote"

private fun JSONArray.toDeviceList(): List<AppleTvDevice> = buildList {
    for (i in 0 until length()) {
        val jsonObject = getJSONObject(i)
        if (jsonObject.has("error")) {
            Log.e(TAG, "Scan error: ${jsonObject.getString("error")}")
            continue
        }
        add(
            AppleTvDevice(
                name = jsonObject.getString("name"),
                address = jsonObject.getString("address"),
                model = jsonObject.optString("model", "Unknown")
            )
        )
    }
}

private fun JSONObject.toStatusPair(): Pair<Boolean, String> =
    Pair(optString("status") == "success", optString("message", ""))
