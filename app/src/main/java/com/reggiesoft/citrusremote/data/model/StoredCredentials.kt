package com.reggiesoft.citrusremote.data.model

import org.json.JSONObject

object StoredCredentials {
    private const val CURRENT_VERSION = 2
    private const val MOCK_DEVICE_IP = "10.0.2.2"
    private const val MOCK_CREDENTIAL_PREFIX = "TEST_MOCK_CREDENTIALS_"

    fun normalize(rawJson: String): String {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return rawJson
        }

        return try {
            normalizeObject(JSONObject(trimmed)).toString()
        } catch (_: Exception) {
            rawJson
        }
    }

    fun getUsableCredentials(deviceIp: String, rawJson: String?): String? {
        val normalized = parseNormalizedObject(rawJson) ?: return null
        if (!isMockDevice(deviceIp) && containsMockCredentials(normalized)) {
            return null
        }
        val hasValidEntry = anyProtocolCredential(normalized) { entry ->
            entry.optString("credential_string").isNotBlank()
        }
        return if (hasValidEntry) normalized.toString() else null
    }

    fun isUsableForDevice(deviceIp: String, rawJson: String?): Boolean =
        getUsableCredentials(deviceIp, rawJson) != null

    private fun parseNormalizedObject(rawJson: String?): JSONObject? {
        val trimmed = rawJson?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return null
        }

        return try {
            normalizeObject(JSONObject(trimmed))
        } catch (_: Exception) {
            null
        }
    }

    private fun containsMockCredentials(normalized: JSONObject): Boolean =
        anyProtocolCredential(normalized) { entry ->
            entry.optString("credential_string").startsWith(MOCK_CREDENTIAL_PREFIX)
        }

    private fun anyProtocolCredential(
        normalized: JSONObject,
        predicate: (JSONObject) -> Boolean
    ): Boolean {
        val protocolCredentials = normalized.optJSONObject("protocol_credentials") ?: return false
        val keys = protocolCredentials.keys()
        while (keys.hasNext()) {
            val entry = protocolCredentials.optJSONObject(keys.next()) ?: continue
            if (predicate(entry)) {
                return true
            }
        }
        return false
    }

    private fun isMockDevice(deviceIp: String): Boolean = deviceIp == MOCK_DEVICE_IP

    private fun normalizeObject(input: JSONObject): JSONObject {
        val protocolCredentials = input.optJSONObject("protocol_credentials")
        if (protocolCredentials != null) {
            if (!input.has("version")) {
                input.put("version", CURRENT_VERSION)
            }
            if (!input.has("primary_protocol")) {
                val keys = protocolCredentials.keys()
                if (keys.hasNext()) {
                    input.put("primary_protocol", keys.next())
                }
            }
            return input
        }

        if (!input.has("credential_string")) {
            return input
        }

        val protocolIdentifier = when {
            input.has("protocol_name") -> input.get("protocol_name").toString()
            input.has("protocol") -> input.get("protocol").toString()
            else -> return input
        }

        val entry = JSONObject().put(
            "credential_string",
            input.optString("credential_string")
        )
        if (input.has("protocol")) {
            entry.put("protocol", input.get("protocol"))
        }
        if (input.has("protocol_name")) {
            entry.put("protocol_name", input.get("protocol_name"))
        }

        return JSONObject()
            .put("version", CURRENT_VERSION)
            .put("primary_protocol", protocolIdentifier)
            .put(
                "protocol_credentials",
                JSONObject().put(protocolIdentifier, entry)
            )
    }
}
