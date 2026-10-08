package com.reggiesoft.citrusremote.ui.pairing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
@OptIn(ExperimentalMaterial3Api::class)
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorState = uiState as? PairingUiState.Error
    val remainingBackOffSeconds = errorState?.takeIf { it.isBackOff }?.backOffSeconds
    val backOffDialogMessage = errorState?.takeIf { it.isBackOff }?.message.orEmpty()

    LaunchedEffect(deviceIp) {
        viewModel.initiatePairing(deviceIp, deviceName)
    }

    LaunchedEffect(uiState) {
        if (uiState is PairingUiState.Success) {
            onPairSuccess()
        }

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
                    IconButton(onClick = {
                        viewModel.cancelPairing()
                        onBack()
                    }) {
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
                        is PairingUiState.Initiating -> "Connecting to device..."
                        is PairingUiState.WaitingForPin -> "Enter the PIN displayed on your TV"
                        is PairingUiState.Error -> {
                            val errorState = uiState as PairingUiState.Error
                            if (errorState.isBackOff) {
                                if ((remainingBackOffSeconds ?: 0L) > 0L) {
                                    "Pairing locked for ${PairingViewModel.formatBackOffDuration(remainingBackOffSeconds ?: 0L)}"
                                } else {
                                    "Pairing lock expired. Retry the connection."
                                }
                            } else if (errorState.canRetryInitiating) {
                                "Pairing needs to be restarted"
                            } else {
                                "Enter the PIN displayed on your TV"
                            }
                        }
                        is PairingUiState.Pairing -> "Verifying PIN..."
                        is PairingUiState.Success -> "Success!"
                        else -> ""
                    }
                    
                    val isPairingLocked = statusText.contains("Pairing locked")
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
                                        Text(backOffDialogMessage)
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
                    val isLocked = (remainingBackOffSeconds ?: 0L) > 0L
                    val isInputEnabled = uiState is PairingUiState.WaitingForPin && !isLocked
                    
                    BasicTextField(
                        value = pinCode,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.filter { it.isDigit() }
                            if (digitsOnly.length <= 4) {
                                pinCode = digitsOnly
                                if (digitsOnly.length == 4 && !isLocked) {
                                    viewModel.finishPairing(digitsOnly)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = isInputEnabled,
                        modifier = Modifier
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
                                        val char = when {
                                            index >= pinCode.length -> ""
                                            else -> pinCode[index].toString()
                                        }
                                        val isFocused = index == pinCode.length && isInputEnabled
                                        val showErrorBorder = hasError && (pinCode.length == 4 || (uiState as PairingUiState.Error).isBackOff)
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .border(
                                                    width = if (isFocused) 2.dp else 1.dp,
                                                    color = if (showErrorBorder) MaterialTheme.colorScheme.error 
                                                           else if (isFocused) MaterialTheme.colorScheme.primary 
                                                           else MaterialTheme.colorScheme.outline,
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
                    
                    if (hasError) {
                        val errorState = uiState as PairingUiState.Error
                        if (!errorState.isBackOff) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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
                            val couldRetry = hasError && (uiState as PairingUiState.Error).canRetryInitiating
                            val isLocked = (remainingBackOffSeconds ?: 0L) > 0L
                            val canPair = uiState is PairingUiState.WaitingForPin
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (couldRetry) {
                                            pinCode = ""
                                            viewModel.initiatePairing(deviceIp, deviceName)
                                        } else if (canPair && pinCode.isNotEmpty()) {
                                            viewModel.finishPairing(pinCode)
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
                                        text = if (couldRetry) "Retry Connection" else "Pair Device",
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
