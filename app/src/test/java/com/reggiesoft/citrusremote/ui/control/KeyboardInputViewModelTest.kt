package com.reggiesoft.citrusremote.ui.control

import com.reggiesoft.citrusremote.data.repository.KeyboardInputRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeyboardInputViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: KeyboardInputRepository
    private lateinit var viewModel: KeyboardInputViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = KeyboardInputViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default message and empty text`() {
        assertEquals("", viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Open a text field on Apple TV to start typing.", viewModel.uiState.value.status)
    }

    @Test
    fun `loadKeyboard success updates text and status`() = runTest(testDispatcher) {
        coEvery { repository.getKeyboardText("10.0.2.2") } returns "Search term"

        viewModel.loadKeyboard("10.0.2.2")

        testScheduler.advanceUntilIdle()
        assertEquals("Search term", viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Editing live text on Apple TV.", viewModel.uiState.value.status)
    }

    @Test
    fun `loadKeyboard with empty text sets helper message`() = runTest(testDispatcher) {
        coEvery { repository.getKeyboardText("10.0.2.2") } returns ""

        viewModel.loadKeyboard("10.0.2.2")

        testScheduler.advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Type here and it will sync to Apple TV.", viewModel.uiState.value.status)
    }

    @Test
    fun `loadKeyboard with error sets cleaned error status`() = runTest(testDispatcher) {
        coEvery { repository.getKeyboardText("10.0.2.2") } returns "Error: Keyboard not active"

        viewModel.loadKeyboard("10.0.2.2")

        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Keyboard not active", viewModel.uiState.value.status)
    }

    @Test
    fun `updateText debounces and calls setKeyboardText`() = runTest(testDispatcher) {
        coEvery { repository.setKeyboardText("10.0.2.2", "New Query") } returns "Success: Updated text"

        viewModel.updateText("10.0.2.2", "New Query")

        assertEquals("New Query", viewModel.uiState.value.text)
        assertEquals("Syncing to Apple TV...", viewModel.uiState.value.status)

        testScheduler.advanceTimeBy(300)
        testScheduler.advanceUntilIdle()
        assertEquals("Typing on Apple TV.", viewModel.uiState.value.status)
    }

    @Test
    fun `clearText clears text and invokes clearKeyboardText`() = runTest(testDispatcher) {
        coEvery { repository.clearKeyboardText("10.0.2.2") } returns "Success: Cleared text"

        viewModel.clearText("10.0.2.2")

        testScheduler.advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Apple TV text cleared.", viewModel.uiState.value.status)
    }

    @Test
    fun `submit sends text and invokes submitKeyboardText`() = runTest(testDispatcher) {
        coEvery { repository.getKeyboardText("10.0.2.2") } returns "Query"
        viewModel.loadKeyboard("10.0.2.2")
        testScheduler.advanceUntilIdle()

        coEvery { repository.submitKeyboardText("10.0.2.2", "Query") } returns "Success: Submitted"
        viewModel.submit("10.0.2.2")

        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Moved to the next Apple TV field.", viewModel.uiState.value.status)
    }
}
