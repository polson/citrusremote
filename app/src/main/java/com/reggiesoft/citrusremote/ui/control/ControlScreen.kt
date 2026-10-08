package com.reggiesoft.citrusremote.ui.control

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reggiesoft.citrusremote.data.model.RemoteCommand
import com.reggiesoft.citrusremote.ui.theme.CITRUS_CREAM_SHADE
import com.reggiesoft.citrusremote.ui.theme.CITRUS_DEEP_TEAL
import com.reggiesoft.citrusremote.ui.theme.CITRUS_TEAL_HIGHLIGHT
import com.reggiesoft.citrusremote.ui.theme.CITRUS_TEAL_SURFACE
import com.reggiesoft.citrusremote.ui.theme.CITRUS_WARM_CREAM
import com.reggiesoft.citrusremote.ui.theme.REMOTE_INDICATOR_DARK_NEUTRAL
import com.reggiesoft.citrusremote.ui.theme.REMOTE_SHELL_DARK_BORDER
import com.reggiesoft.citrusremote.ui.theme.REMOTE_SHELL_DARK_BOTTOM
import com.reggiesoft.citrusremote.ui.theme.REMOTE_SHELL_DARK_TOP
import com.reggiesoft.citrusremote.ui.theme.CitrusRemoteTheme

@OptIn(ExperimentalMaterial3Api::class)
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

            AppleTvInspiredRemote(
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

@Composable
private fun StatusBadge(
    uiState: ControlUiState,
) {
    AnimatedContent(
        targetState = uiState,
        label = "StatusAnimation",
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        }
    ) { state ->
        val text = when (state) {
            is ControlUiState.Ready -> "Ready"
            is ControlUiState.Connecting -> "Sending..."
            is ControlUiState.Result -> "Done"
            is ControlUiState.Error -> state.message
        }

        val color = when (state) {
            is ControlUiState.Ready -> MaterialTheme.colorScheme.secondary
            is ControlUiState.Connecting -> MaterialTheme.colorScheme.primary
            is ControlUiState.Error -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.tertiary
        }

        Surface(
            color = color.copy(alpha = 0.12f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AppleTvInspiredRemote(
    uiState: ControlUiState,
    onCommand: (RemoteCommand) -> Unit,
    onKeyboard: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val compact = maxWidth < 360.dp
        val remoteWidth = if (compact) 208.dp else 228.dp
        val clickPadSize = if (compact) 164.dp else 184.dp
        val shellShape = RoundedCornerShape(if (compact) 34.dp else 40.dp)
        val controlSpacing = if (compact) 12.dp else 14.dp
        val faceButtonSize = if (compact) 48.dp else 52.dp
        val rockerHeight = faceButtonSize * 3 + controlSpacing * 2
        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val shellGradient = if (isDarkTheme) {
            listOf(REMOTE_SHELL_DARK_TOP, REMOTE_SHELL_DARK_BOTTOM)
        } else {
            listOf(CITRUS_WARM_CREAM, CITRUS_CREAM_SHADE)
        }
        val shellBorder = if (isDarkTheme) {
            REMOTE_SHELL_DARK_BORDER.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        }
        val neutralIndicatorColor = if (isDarkTheme) {
            REMOTE_INDICATOR_DARK_NEUTRAL
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        }
        val indicatorColor by animateColorAsState(
            targetValue = when (uiState) {
                is ControlUiState.Ready -> neutralIndicatorColor
                is ControlUiState.Connecting -> MaterialTheme.colorScheme.primary
                is ControlUiState.Result -> MaterialTheme.colorScheme.secondary
                is ControlUiState.Error -> MaterialTheme.colorScheme.error
            },
            animationSpec = tween(durationMillis = 250),
            label = "RemoteStatusIndicatorColor"
        )

        Surface(
            modifier = Modifier
                .width(remoteWidth)
                .shadow(24.dp, shellShape),
            shape = shellShape,
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(shellGradient))
                    .border(BorderStroke(1.dp, shellBorder), shellShape)
                    .padding(horizontal = if (compact) 16.dp else 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )

                Spacer(modifier = Modifier.height(18.dp))

                DPad(
                    padSize = clickPadSize,
                    onCommand = onCommand
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(controlSpacing),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RemoteFaceButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = { onCommand(RemoteCommand.MENU) },
                            onLongPress = { onCommand(RemoteCommand.MENU_HOLD) },
                            size = faceButtonSize
                        )
                        RemoteFaceButton(
                            icon = Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            onClick = { onCommand(RemoteCommand.PLAY_PAUSE) },
                            size = faceButtonSize
                        )
                        RemoteFaceButton(
                            icon = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Mute",
                            onClick = { onCommand(RemoteCommand.MUTE) },
                            size = faceButtonSize
                        )
                        RemoteFaceButton(
                            icon = Icons.Default.Keyboard,
                            contentDescription = "Keyboard",
                            onClick = onKeyboard,
                            size = faceButtonSize
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(controlSpacing),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RemoteFaceButton(
                            icon = Icons.Default.Home,
                            contentDescription = "Home",
                            onClick = { onCommand(RemoteCommand.HOME) },
                            size = faceButtonSize
                        )
                        VolumeRocker(
                            height = rockerHeight,
                            onVolumeUp = { onCommand(RemoteCommand.VOLUME_UP) },
                            onVolumeDown = { onCommand(RemoteCommand.VOLUME_DOWN) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
            }
        }
    }
}

@Composable
private fun DPad(
    padSize: androidx.compose.ui.unit.Dp,
    onCommand: (RemoteCommand) -> Unit,
) {
    val padGradient = listOf(CITRUS_TEAL_HIGHLIGHT, CITRUS_DEEP_TEAL)
    val dividerColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.2f)
    val selectSize = if (padSize < 175.dp) 72.dp else 80.dp
    val tapTargetSize = if (padSize < 175.dp) 58.dp else 64.dp
    val dPadInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(padSize)
            .shadow(14.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.radialGradient(padGradient))
            .border(BorderStroke(1.dp, dividerColor), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val holePath = remember { Path() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val radius = selectSize.toPx() / 2f
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    holePath.reset()
                    holePath.addOval(
                        Rect(
                            left = centerX - radius,
                            top = centerY - radius,
                            right = centerX + radius,
                            bottom = centerY + radius
                        )
                    )

                    clipPath(holePath, clipOp = ClipOp.Difference) {
                        this@drawWithContent.drawContent()
                    }
                }
                .indication(dPadInteractionSource, LocalIndication.current)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(tapTargetSize)
                .dPadDirectionTapTarget(dPadInteractionSource, { onCommand(RemoteCommand.UP) })
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(tapTargetSize)
                .dPadDirectionTapTarget(dPadInteractionSource, { onCommand(RemoteCommand.DOWN) })
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(tapTargetSize)
                .dPadDirectionTapTarget(
                    interactionSource = dPadInteractionSource,
                    onTap = { onCommand(RemoteCommand.LEFT) },
                    onLongPress = { onCommand(RemoteCommand.LEFT_HOLD) }
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(tapTargetSize)
                .dPadDirectionTapTarget(
                    interactionSource = dPadInteractionSource,
                    onTap = { onCommand(RemoteCommand.RIGHT) },
                    onLongPress = { onCommand(RemoteCommand.RIGHT_HOLD) }
                )
        )

        Box(
            modifier = Modifier
                .size(selectSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(BorderStroke(1.dp, dividerColor), CircleShape)
                .clickable(onClick = { onCommand(RemoteCommand.SELECT) })
        )
    }
}

@Composable
private fun Modifier.dPadDirectionTapTarget(
    interactionSource: MutableInteractionSource,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
): Modifier {
    val pressOriginInParent = remember { mutableStateOf(Offset.Zero) }

    return this
        .onGloballyPositioned { coordinates ->
            pressOriginInParent.value = coordinates.positionInParent()
        }
        .pointerInput(interactionSource, onTap, onLongPress) {
            detectTapGestures(
                onTap = { onTap() },
                onLongPress = if (onLongPress == null) null else { _ -> onLongPress() },
                onPress = { offset ->
                    val press = PressInteraction.Press(pressOriginInParent.value + offset)
                    interactionSource.emit(press)
                    val released = tryAwaitRelease()
                    if (released) {
                        interactionSource.emit(PressInteraction.Release(press))
                    } else {
                        interactionSource.emit(PressInteraction.Cancel(press))
                    }
                }
            )
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RemoteFaceButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    size: androidx.compose.ui.unit.Dp,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .size(size)
            .shadow(8.dp, CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = CircleShape,
        color = CITRUS_DEEP_TEAL
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = CITRUS_WARM_CREAM,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun VolumeRocker(
    height: androidx.compose.ui.unit.Dp,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(width = 52.dp, height = height)
            .shadow(10.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = CITRUS_DEEP_TEAL
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(onClick = onVolumeUp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Volume up",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.width(22.dp),
                color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.2f)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(onClick = onVolumeDown),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Volume down",
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyboardInputSheet(
    text: String,
    status: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Type on Apple TV",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { onSubmit() }
                ),
                placeholder = {
                    Text("Start typing")
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
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
