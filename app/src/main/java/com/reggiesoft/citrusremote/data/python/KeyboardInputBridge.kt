package com.reggiesoft.citrusremote.data.python

import android.util.Log
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyboardInputBridge @Inject constructor() {

    private val python by lazy { Python.getInstance() }
    private val module by lazy { python.getModule("appletv_keyboard") }

    suspend fun getKeyboardText(deviceIp: String, credsJson: String): String =
        withContext(Dispatchers.IO) { runBridgeCall("get_keyboard_text", deviceIp, credsJson) }

    suspend fun setKeyboardText(deviceIp: String, credsJson: String, text: String): String =
        withContext(Dispatchers.IO) { runBridgeCall("set_keyboard_text", deviceIp, credsJson, text) }

    suspend fun clearKeyboardText(deviceIp: String, credsJson: String): String =
        withContext(Dispatchers.IO) { runBridgeCall("clear_keyboard_text", deviceIp, credsJson) }

    suspend fun submitKeyboardText(deviceIp: String, credsJson: String, text: String): String =
        withContext(Dispatchers.IO) { runBridgeCall("submit_keyboard_text", deviceIp, credsJson, text) }

    private fun runBridgeCall(functionName: String, vararg args: Any): String {
        return try {
            val result = module.callAttr(functionName, *args).toString()
            if (result.startsWith("Error:")) {
                Log.e(TAG, result)
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception executing Python function '$functionName'", e)
            "Error: ${e.message}"
        }
    }

    private companion object {
        private const val TAG = "AppleTVKeyboard"
    }
}
