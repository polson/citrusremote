package com.reggiesoft.citrusremote.data.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredCredentialsTest {

    @Test
    fun `normalize returns raw string for invalid JSON`() {
        val invalid = "not a json"
        assertEquals(invalid, StoredCredentials.normalize(invalid))
    }

    @Test
    fun `normalize upgrades legacy credential format to version 2`() {
        val legacy = """
            {
                "protocol": 2,
                "protocol_name": "Companion",
                "credential_string": "test_cred_123"
            }
        """.trimIndent()

        val normalized = StoredCredentials.normalize(legacy)
        val json = JSONObject(normalized)

        assertEquals(2, json.getInt("version"))
        assertEquals("Companion", json.getString("primary_protocol"))
        val protocolCreds = json.getJSONObject("protocol_credentials")
        val companion = protocolCreds.getJSONObject("Companion")
        assertEquals("test_cred_123", companion.getString("credential_string"))
    }

    @Test
    fun `normalize preserves existing version 2 credential format`() {
        val v2 = """
            {
                "version": 2,
                "primary_protocol": "Companion",
                "protocol_credentials": {
                    "Companion": {
                        "protocol": 2,
                        "protocol_name": "Companion",
                        "credential_string": "valid_token"
                    }
                }
            }
        """.trimIndent()

        val normalized = StoredCredentials.normalize(v2)
        val json = JSONObject(normalized)

        assertEquals(2, json.getInt("version"))
        assertEquals("Companion", json.getString("primary_protocol"))
        assertTrue(json.has("protocol_credentials"))
    }

    @Test
    fun `getUsableCredentials returns null for empty or invalid input`() {
        assertNull(StoredCredentials.getUsableCredentials("192.168.1.100", null))
        assertNull(StoredCredentials.getUsableCredentials("192.168.1.100", ""))
        assertNull(StoredCredentials.getUsableCredentials("192.168.1.100", "invalid json"))
    }

    @Test
    fun `getUsableCredentials rejects mock credentials on real device`() {
        val mockCreds = """
            {
                "version": 2,
                "primary_protocol": "Companion",
                "protocol_credentials": {
                    "Companion": {
                        "credential_string": "TEST_MOCK_CREDENTIALS_ABC"
                    }
                }
            }
        """.trimIndent()

        assertNull(StoredCredentials.getUsableCredentials("192.168.1.100", mockCreds))
        assertFalse(StoredCredentials.isUsableForDevice("192.168.1.100", mockCreds))
    }

    @Test
    fun `getUsableCredentials accepts mock credentials on mock device`() {
        val mockCreds = """
            {
                "version": 2,
                "primary_protocol": "Companion",
                "protocol_credentials": {
                    "Companion": {
                        "credential_string": "TEST_MOCK_CREDENTIALS_ABC"
                    }
                }
            }
        """.trimIndent()

        assertNotNull(StoredCredentials.getUsableCredentials("10.0.2.2", mockCreds))
        assertTrue(StoredCredentials.isUsableForDevice("10.0.2.2", mockCreds))
    }

    @Test
    fun `getUsableCredentials accepts valid credentials on real device`() {
        val realCreds = """
            {
                "version": 2,
                "primary_protocol": "Companion",
                "protocol_credentials": {
                    "Companion": {
                        "credential_string": "actual_live_token"
                    }
                }
            }
        """.trimIndent()

        val result = StoredCredentials.getUsableCredentials("192.168.1.100", realCreds)
        assertNotNull(result)
        assertTrue(StoredCredentials.isUsableForDevice("192.168.1.100", realCreds))
    }
}
