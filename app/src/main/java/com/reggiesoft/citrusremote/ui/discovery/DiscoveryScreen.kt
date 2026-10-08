package com.reggiesoft.citrusremote.ui.discovery

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reggiesoft.citrusremote.BuildConfig
import com.reggiesoft.citrusremote.data.model.AppleTvDevice
import com.reggiesoft.citrusremote.data.model.MockAppleTv
import com.reggiesoft.citrusremote.ui.theme.CitrusRemoteTheme
import kotlinx.coroutines.delay

@Composable
fun DiscoveryScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoveryViewModel = hiltViewModel(),
    showMockDevice: Boolean = BuildConfig.DEBUG,
    onDeviceSelected: (AppleTvDevice) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isMockServerDialogVisible by remember { mutableStateOf(false) }
    var mockServerAddress by remember { mutableStateOf(viewModel.getMockServerAddress()) }
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1000L)
            value = System.currentTimeMillis()
        }
    }

    DiscoveryScreenContent(
        uiState = uiState,
        modifier = modifier,
        showMockDevice = showMockDevice,
        isPairingLocked = { address -> viewModel.isPairingLocked(address, nowMillis) },
        onScan = { viewModel.scanForDevices() },
        onPrivacyPolicyClick = { context.openWebUrl(PRIVACY_POLICY_URL) },
        onDeviceSelected = onDeviceSelected,
        onMockDeviceSelected = { isMockServerDialogVisible = true }
    )

    if (isMockServerDialogVisible) {
        AlertDialog(
            onDismissRequest = { isMockServerDialogVisible = false },
            title = { Text("Mock Apple TV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Use 10.0.2.2 in the Android Emulator. For a physical phone, enter your computer's local IPv4 address and connect both devices to the same Wi-Fi."
                    )
                    OutlinedTextField(
                        value = mockServerAddress,
                        onValueChange = { mockServerAddress = it },
                        label = { Text("Mock server address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setMockServerAddress(mockServerAddress.trim())
                        isMockServerDialogVisible = false
                        onDeviceSelected(
                            AppleTvDevice(
                                name = "Mock Apple TV (Dev)",
                                address = MockAppleTv.DEVICE_ID,
                                model = "FakeAppleTV"
                            )
                        )
                    },
                    enabled = mockServerAddress.isNotBlank()
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { isMockServerDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoveryScreenContent(
    uiState: DiscoveryUiState,
    modifier: Modifier = Modifier,
    showMockDevice: Boolean = false,
    isPairingLocked: (String) -> Boolean = { false },
    onScan: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onDeviceSelected: (AppleTvDevice) -> Unit,
    onMockDeviceSelected: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                    IconButton(onClick = onPrivacyPolicyClick) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Privacy Policy"
                        )
                    }
                    if (uiState is DiscoveryUiState.Loading) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(onClick = onScan) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Scan for Devices"
                            )
                        }
                        if (showMockDevice) {
                            IconButton(onClick = onMockDeviceSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Build,
                                    contentDescription = "Add Mock Device"
                                )
                            }
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = uiState,
                label = "DiscoveryStateAnimation",
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }
            ) { state ->
                when (state) {
                    is DiscoveryUiState.Loading -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(SHIMMER_ITEM_COUNT) {
                                DeviceShimmerItem()
                            }
                            item {
                                Text(
                                    text = "Scanning for Apple TVs...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                    is DiscoveryUiState.Success -> {
                        if (state.devices.isEmpty()) {
                            EmptyDeviceList(onScanClick = onScan)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.devices, key = { it.address }) { device ->
                                    DeviceListItem(
                                        device = device,
                                        isPairingLocked = isPairingLocked(device.address),
                                        onClick = { onDeviceSelected(device) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    }
                    is DiscoveryUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Oops!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            Button(
                                onClick = onScan,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Try Again", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoveryScreenPreview() {
    CitrusRemoteTheme {
        DiscoveryScreenContent(
            uiState = DiscoveryUiState.Success(
                listOf(
                    AppleTvDevice("Living Room Apple TV", "192.168.1.100", "AppleTV6,2"),
                    AppleTvDevice("Bedroom Apple TV", "192.168.1.101", "AppleTV5,3")
                )
            ),
            onScan = {},
            onPrivacyPolicyClick = {},
            onDeviceSelected = {}
        )
    }
}

private const val PRIVACY_POLICY_URL = "https://reggiesoft.com/citrusremote/privacy/"
private const val SHIMMER_ITEM_COUNT = 3

private fun Context.openWebUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
