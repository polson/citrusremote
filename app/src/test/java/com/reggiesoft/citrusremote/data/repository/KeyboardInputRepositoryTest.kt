package com.reggiesoft.citrusremote.data.repository

import com.reggiesoft.citrusremote.data.python.KeyboardInputBridge
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class KeyboardInputRepositoryTest {

    private lateinit var keyboardBridge: KeyboardInputBridge
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var repository: KeyboardInputRepository

    @Before
    fun setUp() {
        keyboardBridge = mockk()
        deviceRepository = mockk()
        repository = KeyboardInputRepository(keyboardBridge, deviceRepository)
    }

    @Test
    fun getKeyboardText_whenCredentialsMissing_returnsInvalidCredentialsMessage() = runTest {
        every { deviceRepository.getCredentials("10.0.2.2") } returns null

        val result = repository.getKeyboardText("10.0.2.2")

        assertEquals(DeviceRepository.INVALID_CREDENTIALS_MESSAGE, result)
        coVerify(exactly = 0) { keyboardBridge.getKeyboardText(any(), any()) }
    }

    @Test
    fun getKeyboardText_whenCredentialsPresent_invokesBridgeAndClearsIfInvalid() = runTest {
        every { deviceRepository.getCredentials("10.0.2.2") } returns "mock_creds"
        coEvery { keyboardBridge.getKeyboardText("10.0.2.2", "mock_creds") } returns "live text"
        every { deviceRepository.clearCredentialsIfInvalid("10.0.2.2", "live text") } returns "live text"

        val result = repository.getKeyboardText("10.0.2.2")

        assertEquals("live text", result)
        coVerify(exactly = 1) { keyboardBridge.getKeyboardText("10.0.2.2", "mock_creds") }
        verify(exactly = 1) { deviceRepository.clearCredentialsIfInvalid("10.0.2.2", "live text") }
    }

    @Test
    fun setKeyboardText_whenCredentialsPresent_delegatesToBridge() = runTest {
        every { deviceRepository.getCredentials("10.0.2.2") } returns "mock_creds"
        coEvery { keyboardBridge.setKeyboardText("10.0.2.2", "mock_creds", "hello") } returns "Success"
        every { deviceRepository.clearCredentialsIfInvalid("10.0.2.2", "Success") } returns "Success"

        val result = repository.setKeyboardText("10.0.2.2", "hello")

        assertEquals("Success", result)
        coVerify(exactly = 1) { keyboardBridge.setKeyboardText("10.0.2.2", "mock_creds", "hello") }
    }

    @Test
    fun clearKeyboardText_whenCredentialsPresent_delegatesToBridge() = runTest {
        every { deviceRepository.getCredentials("10.0.2.2") } returns "mock_creds"
        coEvery { keyboardBridge.clearKeyboardText("10.0.2.2", "mock_creds") } returns "Success"
        every { deviceRepository.clearCredentialsIfInvalid("10.0.2.2", "Success") } returns "Success"

        val result = repository.clearKeyboardText("10.0.2.2")

        assertEquals("Success", result)
        coVerify(exactly = 1) { keyboardBridge.clearKeyboardText("10.0.2.2", "mock_creds") }
    }

    @Test
    fun submitKeyboardText_whenCredentialsPresent_delegatesToBridge() = runTest {
        every { deviceRepository.getCredentials("10.0.2.2") } returns "mock_creds"
        coEvery { keyboardBridge.submitKeyboardText("10.0.2.2", "mock_creds", "done") } returns "Success"
        every { deviceRepository.clearCredentialsIfInvalid("10.0.2.2", "Success") } returns "Success"

        val result = repository.submitKeyboardText("10.0.2.2", "done")

        assertEquals("Success", result)
        coVerify(exactly = 1) { keyboardBridge.submitKeyboardText("10.0.2.2", "mock_creds", "done") }
    }
}
