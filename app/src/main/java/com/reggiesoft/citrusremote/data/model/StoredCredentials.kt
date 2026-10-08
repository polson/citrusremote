package com.reggiesoft.citrusremote.data.model

import org.json.JSONObject

object StoredCredentials {
    private const val CURRENT_VERSION = 2
    private const val MOCK_DEVICE_IP = "10.0.2.2"
    private const val MOCK_CREDENTIAL_PREFIX = "TEST_MOCK_CREDENTIALS_"
    private const val KEY_VERSION = "version"
    private const val KEY_PRIMARY_PROTOCOL = "primary_protocol"
    private const val KEY_PROTOCOL_CREDENTIALS = "protocol_credentials"
    private const val KEY_CREDENTIAL_STRING = "credential_string"
    private const val KEY_PROTOCOL = "protocol"
    private const val KEY_PROTOCOL_NAME = "protocol_name"

    fun normalize(rawJson: String): String {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return rawJson
        }

        return try {
            JSONObject(trimmed).normalize().toString()
        } catch (_: Exception) {
            rawJson
        }
    }

    fun getUsableCredentials(deviceIp: String, rawJson: String?): String? {
        val normalized = parseNormalizedObject(rawJson) ?: return null
        if (!deviceIp.isMockDevice() && normalized.containsMockCredentials()) {
            return null
        }
        val hasValidEntry = normalized.anyProtocolCredential { entry ->
            entry.optString(KEY_CREDENTIAL_STRING).isNotBlank()
        }
        return if (hasValidEntry) {
            normalized.toString()
        } else {
            null
        }
    }

    fun isUsableForDevice(deviceIp: String, rawJson: String?): Boolean =
        getUsableCredentials(deviceIp, rawJson) != null

    private fun parseNormalizedObject(rawJson: String?): JSONObject? {
        val trimmed = rawJson?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return null
        }

        return try {
            JSONObject(trimmed).normalize()
        } catch (_: Exception) {
            null
        }
    }

    private fun JSONObject.containsMockCredentials(): Boolean =
        anyProtocolCredential { entry ->
            entry.optString(KEY_CREDENTIAL_STRING).startsWith(MOCK_CREDENTIAL_PREFIX)
        }

    private fun JSONObject.anyProtocolCredential(
        predicate: (JSONObject) -> Boolean
    ): Boolean {
        val protocolCredentials = optJSONObject(KEY_PROTOCOL_CREDENTIALS) ?: return false
        return protocolCredentials.keys().asSequence().any { key ->
            val entry = protocolCredentials.optJSONObject(key) ?: return@any false
            predicate(entry)
        }
    }

    private fun String.isMockDevice(): Boolean = this == MOCK_DEVICE_IP

    private fun JSONObject.normalize(): JSONObject {
        val protocolCredentials = optJSONObject(KEY_PROTOCOL_CREDENTIALS)
        if (protocolCredentials != null) {
            if (!has(KEY_VERSION)) {
                put(KEY_VERSION, CURRENT_VERSION)
            }
            if (!has(KEY_PRIMARY_PROTOCOL)) {
                val keys = protocolCredentials.keys()
                if (keys.hasNext()) {
                    put(KEY_PRIMARY_PROTOCOL, keys.next())
                }
            }
            return this
        }

        if (!has(KEY_CREDENTIAL_STRING)) {
            return this
        }

        val protocolIdentifier = when {
            has(KEY_PROTOCOL_NAME) -> get(KEY_PROTOCOL_NAME).toString()
            has(KEY_PROTOCOL) -> get(KEY_PROTOCOL).toString()
            else -> return this
        }

        val entry = JSONObject().put(
            KEY_CREDENTIAL_STRING,
            optString(KEY_CREDENTIAL_STRING)
        )
        if (has(KEY_PROTOCOL)) {
            entry.put(KEY_PROTOCOL, get(KEY_PROTOCOL))
        }
        if (has(KEY_PROTOCOL_NAME)) {
            entry.put(KEY_PROTOCOL_NAME, get(KEY_PROTOCOL_NAME))
        }

        return JSONObject()
            .put(KEY_VERSION, CURRENT_VERSION)
            .put(KEY_PRIMARY_PROTOCOL, protocolIdentifier)
            .put(
                KEY_PROTOCOL_CREDENTIALS,
                JSONObject().put(protocolIdentifier, entry)
            )
    }
}
