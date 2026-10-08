package com.reggiesoft.citrusremote.ui.control

import com.reggiesoft.citrusremote.data.model.RemoteCommand
import com.reggiesoft.citrusremote.data.repository.DeviceRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ControlViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DeviceRepository
    private lateinit var viewModel: ControlViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = ControlViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Ready`() {
        assertTrue(viewModel.uiState.value is ControlUiState.Ready)
    }

    @Test
    fun `sendCommand success sets Result state and resets to Ready`() = runTest(testDispatcher) {
        coEvery { repository.sendCommand("192.168.1.100", RemoteCommand.SELECT) } returns "Success: Sent select"

        viewModel.sendCommand("192.168.1.100", RemoteCommand.SELECT)

        testScheduler.advanceTimeBy(100)
        val resultState = viewModel.uiState.value
        assertTrue(resultState is ControlUiState.Result)
        assertEquals("Success: Sent select", (resultState as ControlUiState.Result).message)

        testScheduler.advanceTimeBy(1500)
        assertTrue(viewModel.uiState.value is ControlUiState.Ready)
    }

    @Test
    fun `sendCommand error sets Error state and resets to Ready`() = runTest(testDispatcher) {
        coEvery { repository.sendCommand("192.168.1.100", RemoteCommand.UP) } returns "Error: Connection timed out"

        viewModel.sendCommand("192.168.1.100", RemoteCommand.UP)

        testScheduler.advanceTimeBy(100)
        val errorState = viewModel.uiState.value
        assertTrue(errorState is ControlUiState.Error)
        assertEquals("Error: Connection timed out", (errorState as ControlUiState.Error).message)

        testScheduler.advanceTimeBy(1500)
        assertTrue(viewModel.uiState.value is ControlUiState.Ready)
    }

    @Test
    fun `forgetDevice clears repository credentials and keeps state Ready`() = runTest(testDispatcher) {
        viewModel.forgetDevice("192.168.1.100")

        verify(exactly = 1) { repository.clearCredentials("192.168.1.100") }
        assertTrue(viewModel.uiState.value is ControlUiState.Ready)
    }
}
