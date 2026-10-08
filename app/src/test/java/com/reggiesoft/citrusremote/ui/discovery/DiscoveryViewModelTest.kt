package com.reggiesoft.citrusremote.ui.discovery

import com.reggiesoft.citrusremote.data.model.AppleTvDevice
import com.reggiesoft.citrusremote.data.repository.DeviceRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
class DiscoveryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: DeviceRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init triggers scan and sets Success state`() = runTest {
        val testDevices = listOf(
            AppleTvDevice("Living Room", "192.168.1.100", "Apple TV 4K"),
            AppleTvDevice("Bedroom", "192.168.1.101", "Apple TV HD")
        )
        coEvery { repository.scanOnce() } returns testDevices

        val viewModel = DiscoveryViewModel(repository)

        val state = viewModel.uiState.value
        assertTrue(state is DiscoveryUiState.Success)
        assertEquals(testDevices, (state as DiscoveryUiState.Success).devices)
    }

    @Test
    fun `scan error sets Error state`() = runTest {
        coEvery { repository.scanOnce() } throws RuntimeException("Network scan failed")

        val viewModel = DiscoveryViewModel(repository)

        val state = viewModel.uiState.value
        assertTrue(state is DiscoveryUiState.Error)
        assertEquals("Network scan failed", (state as DiscoveryUiState.Error).message)
    }

    @Test
    fun `hasUsableCredentials delegates to repository`() {
        val viewModel = DiscoveryViewModel(repository)
        every { repository.hasUsableCredentials("192.168.1.100") } returns true
        every { repository.hasUsableCredentials("192.168.1.101") } returns false

        assertTrue(viewModel.hasUsableCredentials("192.168.1.100"))
        assertFalse(viewModel.hasUsableCredentials("192.168.1.101"))
    }

    @Test
    fun `isPairingLocked delegates to repository`() {
        val viewModel = DiscoveryViewModel(repository)
        every { repository.isPairingLocked("192.168.1.100", any()) } returns true
        every { repository.isPairingLocked("192.168.1.101", any()) } returns false

        assertTrue(viewModel.isPairingLocked("192.168.1.100"))
        assertFalse(viewModel.isPairingLocked("192.168.1.101"))
    }
}
