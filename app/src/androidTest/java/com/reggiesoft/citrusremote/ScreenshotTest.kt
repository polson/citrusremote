package com.reggiesoft.citrusremote

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.reggiesoft.citrusremote.data.model.AppleTvDevice
import com.reggiesoft.citrusremote.ui.control.ControlScreenContent
import com.reggiesoft.citrusremote.ui.control.ControlUiState
import com.reggiesoft.citrusremote.ui.discovery.DeviceListItem
import com.reggiesoft.citrusremote.ui.theme.CitrusRemoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun saveScreenshot(filename: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = context.getExternalFilesDir(null) ?: File("/sdcard")
        val file = File(dir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    @Composable
    private fun DiscoveryScreenMockContent() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Discovered Devices",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Privacy Policy"
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Scan for Devices"
                            )
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DeviceListItem(
                        device = AppleTvDevice(
                            name = "Living Room Apple TV",
                            address = "192.168.1.105",
                            model = "Apple TV 4K"
                        ),
                        isPairingLocked = false,
                        onClick = {}
                    )
                }
                item {
                    DeviceListItem(
                        device = AppleTvDevice(
                            name = "Bedroom Apple TV",
                            address = "192.168.1.120",
                            model = "Apple TV HD"
                        ),
                        isPairingLocked = false,
                        onClick = {}
                    )
                }
            }
        }
    }

    @Test
    fun capture01DiscoveryLight() {
        composeTestRule.setContent {
            CitrusRemoteTheme(darkTheme = false) {
                DiscoveryScreenMockContent()
            }
        }
        saveScreenshot("screenshot_01_discovery.png")
    }

    @Test
    fun capture02RemoteLight() {
        composeTestRule.setContent {
            CitrusRemoteTheme(darkTheme = false) {
                ControlScreenContent(
                    title = "Living Room Apple TV",
                    uiState = ControlUiState.Ready,
                    onBack = {},
                    onCommand = {},
                    onKeyboard = {},
                    onForgetDevice = {}
                )
            }
        }
        saveScreenshot("screenshot_02_remote.png")
    }

    @Test
    fun capture03DiscoveryDark() {
        composeTestRule.setContent {
            CitrusRemoteTheme(darkTheme = true) {
                DiscoveryScreenMockContent()
            }
        }
        saveScreenshot("screenshot_03_discovery_dark.png")
    }

    @Test
    fun capture04RemoteDark() {
        composeTestRule.setContent {
            CitrusRemoteTheme(darkTheme = true) {
                ControlScreenContent(
                    title = "Living Room Apple TV",
                    uiState = ControlUiState.Ready,
                    onBack = {},
                    onCommand = {},
                    onKeyboard = {},
                    onForgetDevice = {}
                )
            }
        }
        saveScreenshot("screenshot_04_remote_dark.png")
    }
}
