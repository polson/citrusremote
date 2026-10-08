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

    private fun normalizePythonResult(result: String): String {
        return if (result == "Error:" || result == "Error: ") {
            "Error: Apple TV command failed without a detailed message."
        } else {
            result
        }
    }

    override suspend fun scanForDevices(): List<AppleTvDevice> = withContext(Dispatchers.IO) {
        try {
            val resultJson = module.callAttr("scan_for_devices").toString()
            val jsonArray = JSONArray(resultJson)
            val deviceList = mutableListOf<AppleTvDevice>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                if (jsonObject.has("error")) {
                    Log.e("AppleTVRemote", "Scan error: ${jsonObject.getString("error")}")
                    continue
                }
                val name = jsonObject.getString("name")
                val address = jsonObject.getString("address")
                val model = jsonObject.optString("model", "Unknown")
                deviceList.add(AppleTvDevice(name, address, model))
            }
            deviceList
        } catch (e: Exception) {
            Log.e("AppleTVRemote", "Exception scanning for devices", e)
            emptyList()
        }
    }

    override suspend fun initiatePairing(deviceIp: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val resultStr = module.callAttr("initiate_pairing", deviceIp).toString()
            val json = JSONObject(resultStr)
            val success = json.optString("status") == "success"
            Pair(success, json.optString("message", ""))
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
                Log.e("AppleTVRemote", "finishPairing failed: $errorMsg")
                Pair(false, errorMsg)
            }
        } catch (e: Exception) {
            Log.e("AppleTVRemote", "Exception in finishPairing for '$deviceIp'", e)
            Pair(false, e.message ?: "Unknown error")
        }
    }

    override suspend fun validateCredentials(deviceIp: String, credsJson: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val resultStr = module.callAttr("validate_credentials", deviceIp, credsJson).toString()
            val json = JSONObject(resultStr)
            val success = json.optString("status") == "success"
            Pair(success, json.optString("message", ""))
        } catch (e: Exception) {
            Log.e("AppleTVRemote", "Exception validating credentials for '$deviceIp'", e)
            Pair(false, e.message ?: "Unknown error")
        }
    }

    override suspend fun cancelPairing() {
        withContext(Dispatchers.IO) {
            try {
                module.callAttr("cancel_pairing")
            } catch (e: Exception) {
                Log.e("AppleTVRemote", "Exception on cancel", e)
            }
        }
    }

    override suspend fun sendCommand(deviceIp: String, credsJson: String, command: RemoteCommand): String = withContext(Dispatchers.IO) {
        try {
            val result = module.callAttr("send_command", deviceIp, credsJson, command.command)
            val resultString = normalizePythonResult(result.toString())
            if (resultString.startsWith("Error:")) {
                Log.e("AppleTVRemote", resultString)
            }
            resultString
        } catch (e: Exception) {
            Log.e("AppleTVRemote", "Exception executing Python command '${command.command}'", e)
            "Error: ${e.message}"
        }
    }
}
