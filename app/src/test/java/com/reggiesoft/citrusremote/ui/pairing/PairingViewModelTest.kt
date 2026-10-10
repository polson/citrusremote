package com.reggiesoft.citrusremote.ui.pairing

import com.reggiesoft.citrusremote.data.repository.DeviceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: DeviceRepository
    private lateinit var viewModel: PairingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = PairingViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertTrue(viewModel.uiState.value is PairingUiState.Idle)
    }

    @Test
    fun `initiatePairing success sets WaitingForPin state`() = runTest {
        coEvery { repository.initiatePairing("10.0.2.2") } returns Pair(true, "")

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")

        val state = viewModel.uiState.value
        assertTrue("Expected WaitingForPin but was $state", state is PairingUiState.WaitingForPin)
    }

    @Test
    fun `initiatePairing generic failure sets error state with generic message`() = runTest {
        coEvery { repository.initiatePairing("10.0.2.2") } returns Pair(false, "Unknown pairing failure")

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")

        val state = viewModel.uiState.value
        assertTrue("Expected Error but was $state", state is PairingUiState.Error)
        assertEquals(
            "Companion pairing failed. Retry the connection and try again.",
            (state as PairingUiState.Error).message
        )
        assertTrue(state.canRetryInitiating)
    }

    @Test
    fun `initiatePairing connection failure sets error with retry`() = runTest {
        coEvery { repository.initiatePairing("10.0.2.2") } returns Pair(false, "Connection refused")

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")

        val state = viewModel.uiState.value
        assertTrue("Expected Error but was $state", state is PairingUiState.Error)
        val error = state as PairingUiState.Error
        assertTrue(error.canRetryInitiating)
        assertFalse(error.isLocked)
    }

    @Test
    fun `initiatePairing timeout sets error with retry`() = runTest {
        coEvery { repository.initiatePairing("10.0.2.2") } returns Pair(false, "Connection timed out")

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")

        val state = viewModel.uiState.value
        assertTrue("Expected Error but was $state", state is PairingUiState.Error)
        val error = state as PairingUiState.Error
        assertTrue(error.canRetryInitiating)
        assertFalse(error.isLocked)
    }

    @Test
    fun `initiatePairing lockout sets error with lockout info`() = runTest {
        coEvery { repository.initiatePairing("10.0.2.2") } returns Pair(false, "Error=BackOff BackOff=120s")

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")

        val state = viewModel.uiState.value
        assertTrue("Expected Error but was $state", state is PairingUiState.Error)
        val error = state as PairingUiState.Error
        assertTrue(error.isLocked)
        assertEquals(120L, error.lockoutSeconds)
        assertTrue(error.canRetryInitiating)
    }

    @Test
    fun `initiatePairing lockout calls setPairingLockout`() = runTest {
        coEvery { repository.initiatePairing("10.0.2.2") } returns Pair(false, "Error=BackOff BackOff=60s")

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")

        verify { repository.setPairingLockout("10.0.2.2", 60L) }
    }

    @Test
    fun `initiatePairing success clears lockout`() = runTest {
        coEvery { repository.initiatePairing("10.0.2.2") } returns Pair(true, "")

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")

        verify { repository.clearPairingLockout("10.0.2.2") }
    }

    @Test
    fun `finishPairing success sets Success state`() = runTest {
        coEvery { repository.initiatePairing("10.0.2.2") } returns Pair(true, "")
        coEvery { repository.finishPairing("10.0.2.2", "1234") } returns Pair(true, "{}")

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")
        viewModel.finishPairing("1234")

        val state = viewModel.uiState.value
        assertTrue("Expected Success but was $state", state is PairingUiState.Success)
    }

    @Test
    fun `finishPairing invalid pin sets error`() = runTest {
        coEvery { repository.initiatePairing("10.0.2.2") } returns Pair(true, "")
        coEvery { repository.finishPairing("10.0.2.2", "0000") } returns Pair(false, "invalid pin")

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")
        viewModel.finishPairing("0000")

        val state = viewModel.uiState.value
        assertTrue("Expected Error but was $state", state is PairingUiState.Error)
        assertEquals(
            "That PIN was not accepted. Restart pairing, then enter the current 4-digit code from Apple TV.",
            (state as PairingUiState.Error).message
        )
    }

    @Test
    fun `re-initiating pairing after error restores WaitingForPin state`() = runTest {
        coEvery { repository.initiatePairing("10.0.2.2") } returns Pair(true, "")
        coEvery { repository.finishPairing("10.0.2.2", "0000") } returns Pair(false, "invalid pin")

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")
        viewModel.finishPairing("0000")
        assertTrue(viewModel.uiState.value is PairingUiState.Error)

        viewModel.initiatePairing("10.0.2.2", "Mock Apple TV")
        assertTrue(viewModel.uiState.value is PairingUiState.WaitingForPin)
    }
}
