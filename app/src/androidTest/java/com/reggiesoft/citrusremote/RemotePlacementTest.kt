package com.reggiesoft.citrusremote

import android.graphics.Bitmap
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.reggiesoft.citrusremote.data.model.MINIMUM_REMOTE_WIDTH
import com.reggiesoft.citrusremote.data.model.RemoteCommand
import com.reggiesoft.citrusremote.data.model.RemotePlacement
import com.reggiesoft.citrusremote.data.preferences.RemotePlacementPreferences
import com.reggiesoft.citrusremote.ui.control.ControlScreenContent
import com.reggiesoft.citrusremote.ui.control.ControlUiState
import com.reggiesoft.citrusremote.ui.theme.CitrusRemoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class RemotePlacementTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun dragResizeAndSaveKeepsCommandsWorking() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = RemotePlacementPreferences(context)
        val previousPlacement = preferences.load()
        val placement = mutableStateOf(RemotePlacement())
        val commands = mutableListOf<RemoteCommand>()
        try {
            compose.setContent {
                CitrusRemoteTheme {
                    ControlScreenContent(
                        title = "Test Apple TV",
                        uiState = ControlUiState.Ready,
                        modifier = Modifier.requiredSize(width = 400.dp, height = 800.dp),
                        onBack = {},
                        onCommand = { commands.add(it) },
                        onKeyboard = {},
                        onForgetDevice = {},
                        placement = placement.value,
                        onPlacementChange = {
                            preferences.save(it)
                            placement.value = it
                        }
                    )
                }
            }
            compose.onNodeWithText("Reposition").performClick()
            val originalWidth = compose.onNodeWithTag("remote-move").fetchSemanticsNode().boundsInRoot.width
            compose.onNodeWithTag("remote-resize").performTouchInput {
                swipe(start = center, end = center + Offset(x = -100f, y = -200f), durationMillis = 300)
            }
            val smallerWidth = compose.onNodeWithTag("remote-move").fetchSemanticsNode().boundsInRoot.width
            assertTrue(smallerWidth < originalWidth)
            compose.onNodeWithTag("remote-move").performTouchInput {
                click(Offset(x = center.x * 0.5f, y = center.y * 1.3f))
                swipe(start = center, end = center + Offset(x = -60f, y = 30f), durationMillis = 300)
            }
            compose.onNodeWithTag("remote-resize").performTouchInput {
                swipe(start = center, end = center + Offset(x = 100f, y = 200f), durationMillis = 300)
            }
            compose.runOnIdle { assertTrue(commands.isEmpty()) }
            val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
            File(context.cacheDir, "remote_reposition.png").outputStream().use { output ->
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            val editingBounds = compose.onNodeWithTag("remote-move").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithText("Done").performClick()
            val savedBounds = compose.onNodeWithTag("remote").fetchSemanticsNode().boundsInRoot
            assertEquals(editingBounds.left, savedBounds.left, 1f)
            assertEquals(editingBounds.top, savedBounds.top, 1f)
            assertEquals(editingBounds.width, savedBounds.width, 1f)
            assertEquals(editingBounds.height, savedBounds.height, 1f)
            compose.runOnIdle {
                val saved = RemotePlacementPreferences(context).load()
                assertEquals(placement.value, saved)
                assertTrue(saved.horizontalPosition < 0.5f)
                assertTrue(saved.verticalPosition > 0f)
                assertTrue(saved.width > MINIMUM_REMOTE_WIDTH)
            }
            compose.onNodeWithContentDescription("Volume up").performTouchInput { click() }
            compose.runOnIdle { assertEquals(listOf(RemoteCommand.VOLUME_UP), commands) }
            val enlargedWidth = placement.value.width
            compose.onNodeWithText("Reposition").performClick()
            compose.onNodeWithTag("remote-resize").performTouchInput {
                swipe(start = center, end = center + Offset(x = -100f, y = -200f), durationMillis = 300)
            }
            compose.onNodeWithText("Done").performClick()
            compose.runOnIdle {
                assertTrue(placement.value.width < enlargedWidth)
                assertTrue(placement.value.width >= MINIMUM_REMOTE_WIDTH)
                assertEquals(placement.value, RemotePlacementPreferences(context).load())
            }
            compose.onNodeWithContentDescription("Play/Pause").performTouchInput { click() }
            compose.onNodeWithContentDescription("Volume up").performTouchInput { click() }
            compose.runOnIdle {
                assertEquals(
                    listOf(RemoteCommand.VOLUME_UP, RemoteCommand.PLAY_PAUSE, RemoteCommand.VOLUME_UP),
                    commands
                )
            }
        } finally {
            preferences.save(previousPlacement)
        }
    }

    @Test
    fun draftSurvivesRecreationAndCancelAndResetWork() {
        val initialPlacement = RemotePlacement(horizontalPosition = 0.3f, verticalPosition = 0.2f, width = 220f)
        val placement = mutableStateOf(initialPlacement)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CitrusRemoteTheme {
                ControlScreenContent(
                    title = "Test Apple TV",
                    uiState = ControlUiState.Ready,
                    modifier = Modifier.requiredSize(width = 400.dp, height = 800.dp),
                    onBack = {},
                    onCommand = {},
                    onKeyboard = {},
                    onForgetDevice = {},
                    placement = placement.value,
                    onPlacementChange = { placement.value = it }
                )
            }
        }
        compose.onNodeWithText("Reposition").performClick()
        compose.onNodeWithTag("remote-move").performTouchInput {
            swipe(start = center, end = center + Offset(x = 50f, y = 50f), durationMillis = 300)
        }
        val draftBounds = compose.onNodeWithTag("remote-move").fetchSemanticsNode().boundsInRoot
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(draftBounds, compose.onNodeWithTag("remote-move").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithContentDescription("Cancel repositioning").performClick()
        compose.runOnIdle { assertEquals(initialPlacement, placement.value) }
        compose.onNodeWithText("Reposition").performClick()
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { assertEquals(RemotePlacement(), placement.value) }
    }

    @Test
    fun shortScreensKeepControlsReachableByScrolling() {
        var keyboardOpened = false
        compose.setContent {
            CitrusRemoteTheme {
                ControlScreenContent(
                    title = "Test Apple TV",
                    uiState = ControlUiState.Ready,
                    modifier = Modifier.requiredSize(width = 400.dp, height = 400.dp),
                    onBack = {},
                    onCommand = {},
                    onKeyboard = { keyboardOpened = true },
                    onForgetDevice = {}
                )
            }
        }
        compose.onNodeWithContentDescription("Keyboard").performScrollTo().performTouchInput { click() }
        compose.runOnIdle { assertTrue(keyboardOpened) }
        compose.onNodeWithText("Reposition").assertIsDisplayed().performClick()
        compose.onNodeWithTag("remote-resize").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Done").assertIsDisplayed()
    }
}
