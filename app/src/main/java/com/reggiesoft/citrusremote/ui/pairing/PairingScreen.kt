package com.reggiesoft.citrusremote.ui.pairing

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reggiesoft.citrusremote.ui.theme.CitrusRemoteTheme
import kotlinx.coroutines.launch

@Composable
fun PairingScreen(
    deviceIp: String,
    deviceName: String,
    modifier: Modifier = Modifier,
    viewModel: PairingViewModel = hiltViewModel(),
    onPairSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var pinCode by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorState = uiState as? PairingUiState.Error
    val remainingLockoutSeconds = errorState?.takeIf { it.isLocked }?.lockoutSeconds

    LaunchedEffect(deviceIp) {
        viewModel.initiatePairing(deviceIp, deviceName)
    }

    LaunchedEffect(uiState) {
        if (uiState is PairingUiState.Success) {
            onPairSuccess()
        }
    }

    PairingScreenContent(
        deviceName = deviceName,
        uiState = uiState,
        pinCode = pinCode,
        remainingLockoutSeconds = remainingLockoutSeconds,
        onPinChange = { pinCode = it },
        onSubmitPin = { pin -> viewModel.finishPairing(pin) },
        onRetryInitiate = {
            pinCode = ""
            viewModel.initiatePairing(deviceIp, deviceName)
        },
        onBack = {
            viewModel.cancelPairing()
            onBack()
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PairingScreenContent(
    deviceName: String,
    uiState: PairingUiState,
    pinCode: String,
    remainingLockoutSeconds: Long?,
    modifier: Modifier = Modifier,
    onPinChange: (String) -> Unit,
    onSubmitPin: (String) -> Unit,
    onRetryInitiate: () -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()

    val errorState = uiState as? PairingUiState.Error
    val isLocked = (remainingLockoutSeconds ?: 0L) > 0L
    val isPairingLocked = errorState?.isLocked == true && isLocked
    val lockoutDialogMessage = errorState?.takeIf { it.isLocked }?.message.orEmpty()

    LaunchedEffect(uiState) {
        if (uiState is PairingUiState.WaitingForPin) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Pairing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val statusText = when (uiState) {
                        is PairingUiState.Idle -> ""
                        is PairingUiState.Initiating -> "Connecting to device..."
                        is PairingUiState.WaitingForPin -> "Enter the PIN displayed on your TV"
                        is PairingUiState.Pairing -> "Verifying PIN..."
                        is PairingUiState.Success -> "Success!"
                        is PairingUiState.Error -> {
                            when {
                                uiState.isLocked && isLocked -> {
                                    "Pairing locked for ${formatLockoutDuration(remainingLockoutSeconds ?: 0L)}"
                                }
                                uiState.isLocked -> {
                                    "Pairing lock expired. Retry the connection."
                                }
                                uiState.canRetryInitiating -> {
                                    "Pairing needs to be restarted"
                                }
                                else -> {
                                    "Enter the PIN displayed on your TV"
                                }
                            }
                        }
                    }

                    if (isPairingLocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                tooltip = {
                                    RichTooltip(
                                        title = { Text("Pairing Temporarily Locked") },
                                        action = {
                                            TextButton(onClick = { scope.launch { tooltipState.dismiss() } }) {
                                                Text("OK")
                                            }
                                        }
                                    ) {
                                        Text(lockoutDialogMessage)
                                    }
                                },
                                state = tooltipState
                            ) {
                                IconButton(
                                    onClick = { scope.launch { tooltipState.show() } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.error,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "?",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    val isWorking = uiState is PairingUiState.Initiating || uiState is PairingUiState.Pairing
                    val hasError = uiState is PairingUiState.Error
                    val isInputEnabled = uiState is PairingUiState.WaitingForPin && !isLocked

                    PinInputBoxes(
                        pinCode = pinCode,
                        isInputEnabled = isInputEnabled,
                        hasError = hasError,
                        isLocked = errorState?.isLocked == true,
                        focusRequester = focusRequester,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.filter { it.isDigit() }
                            if (digitsOnly.length <= 4) {
                                onPinChange(digitsOnly)
                                if (digitsOnly.length == 4 && !isLocked) {
                                    onSubmitPin(digitsOnly)
                                }
                            }
                        }
                    )

                    if (errorState != null && !errorState.isLocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Crossfade(
                        targetState = isWorking,
                        label = "PairingActionState"
                    ) { working ->
                        if (working) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            val couldRetry = errorState?.canRetryInitiating == true
                            val canPair = uiState is PairingUiState.WaitingForPin

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (couldRetry) {
                                            onRetryInitiate()
                                        } else if (canPair && pinCode.isNotEmpty()) {
                                            onSubmitPin(pinCode)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = (couldRetry || (canPair && pinCode.isNotEmpty())) && !isLocked,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = if (couldRetry) { "Retry Connection" } else { "Pair Device" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinInputBoxes(
    pinCode: String,
    isInputEnabled: Boolean,
    hasError: Boolean,
    isLocked: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = pinCode,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = isInputEnabled,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isInputEnabled) {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    }
            ) {
                Box(modifier = Modifier.alpha(0f)) {
                    innerTextField()
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val char = if (index >= pinCode.length) { "" } else { pinCode[index].toString() }
                        val isFocused = index == pinCode.length && isInputEnabled
                        val showErrorBorder = hasError && (pinCode.length == 4 || isLocked)
                        val borderColor = when {
                            showErrorBorder -> MaterialTheme.colorScheme.error
                            isFocused -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(
                                    width = if (isFocused) { 2.dp } else { 1.dp },
                                    color = borderColor,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PairingScreenPreview() {
    CitrusRemoteTheme {
        PairingScreenContent(
            deviceName = "Living Room Apple TV",
            uiState = PairingUiState.WaitingForPin,
            pinCode = "12",
            remainingLockoutSeconds = null,
            onPinChange = {},
            onSubmitPin = {},
            onRetryInitiate = {},
            onBack = {}
        )
    }
}
