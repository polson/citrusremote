package com.reggiesoft.citrusremote.ui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reggiesoft.citrusremote.data.model.RemoteCommand
import com.reggiesoft.citrusremote.ui.theme.CitrusRemoteTheme

@Composable
fun ControlScreen(
    deviceIp: String,
    deviceName: String,
    modifier: Modifier = Modifier,
    viewModel: ControlViewModel = hiltViewModel(),
    keyboardViewModel: KeyboardInputViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onDeviceForgotten: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardUiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    var isKeyboardSheetVisible by rememberSaveable { mutableStateOf(false) }

    ControlScreenContent(
        title = deviceName.ifBlank { "Remote Control" },
        uiState = uiState,
        modifier = modifier,
        onBack = onBack,
        onCommand = { command -> viewModel.sendCommand(deviceIp, command) },
        onKeyboard = {
            isKeyboardSheetVisible = true
            keyboardViewModel.loadKeyboard(deviceIp)
        },
        onForgetDevice = {
            viewModel.forgetDevice(deviceIp)
            isKeyboardSheetVisible = false
            onDeviceForgotten()
        }
    )

    if (isKeyboardSheetVisible) {
        KeyboardInputSheet(
            text = keyboardUiState.text,
            status = keyboardUiState.status,
            onTextChange = { keyboardViewModel.updateText(deviceIp, it) },
            onSubmit = { keyboardViewModel.submit(deviceIp) },
            onDismiss = { isKeyboardSheetVisible = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ControlScreenContent(
    title: String,
    uiState: ControlUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onCommand: (RemoteCommand) -> Unit,
    onKeyboard: () -> Unit,
    onForgetDevice: () -> Unit,
) {
    var isDeviceMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isForgetDialogVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { isDeviceMenuExpanded = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More actions"
                            )
                        }

                        DropdownMenu(
                            expanded = isDeviceMenuExpanded,
                            onDismissRequest = { isDeviceMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Forget Device") },
                                onClick = {
                                    isDeviceMenuExpanded = false
                                    isForgetDialogVisible = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBadge(uiState = uiState)

            Spacer(modifier = Modifier.height(20.dp))

            RemoteControl(
                uiState = uiState,
                onCommand = onCommand,
                onKeyboard = onKeyboard
            )
        }
    }

    if (isForgetDialogVisible) {
        AlertDialog(
            onDismissRequest = { isForgetDialogVisible = false },
            title = { Text("Forget this Apple TV?") },
            text = { Text("You'll need to pair again to use it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isForgetDialogVisible = false
                        onForgetDevice()
                    }
                ) {
                    Text(
                        text = "Forget Device",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isForgetDialogVisible = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ControlScreenPreview() {
    CitrusRemoteTheme {
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
