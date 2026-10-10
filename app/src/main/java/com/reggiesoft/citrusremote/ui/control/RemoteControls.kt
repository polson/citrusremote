package com.reggiesoft.citrusremote.ui.control

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reggiesoft.citrusremote.data.model.RemoteCommand
import com.reggiesoft.citrusremote.ui.theme.CITRUS_CREAM_SHADE
import com.reggiesoft.citrusremote.ui.theme.CITRUS_DEEP_TEAL
import com.reggiesoft.citrusremote.ui.theme.CITRUS_TEAL_HIGHLIGHT
import com.reggiesoft.citrusremote.ui.theme.CITRUS_WARM_CREAM
import com.reggiesoft.citrusremote.ui.theme.REMOTE_INDICATOR_DARK_NEUTRAL
import com.reggiesoft.citrusremote.ui.theme.REMOTE_SHELL_DARK_BORDER
import com.reggiesoft.citrusremote.ui.theme.REMOTE_SHELL_DARK_BOTTOM
import com.reggiesoft.citrusremote.ui.theme.REMOTE_SHELL_DARK_TOP

@Composable
internal fun StatusBadge(
    uiState: ControlUiState,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState,
        label = "StatusAnimation",
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        modifier = modifier
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
            is ControlUiState.Result -> MaterialTheme.colorScheme.tertiary
            is ControlUiState.Error -> MaterialTheme.colorScheme.error
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
internal fun RemoteControl(
    uiState: ControlUiState,
    onCommand: (RemoteCommand) -> Unit,
    onKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val compact = maxWidth < 360.dp
        val remoteWidth = if (compact) { 208.dp } else { 228.dp }
        val clickPadSize = if (compact) { 164.dp } else { 184.dp }
        val shellShape = RoundedCornerShape(if (compact) { 34.dp } else { 40.dp })
        val controlSpacing = if (compact) { 12.dp } else { 14.dp }
        val faceButtonSize = if (compact) { 48.dp } else { 52.dp }
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
                    .padding(horizontal = if (compact) { 16.dp } else { 18.dp }, vertical = 18.dp),
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
                        RemoteButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = { onCommand(RemoteCommand.MENU) },
                            onLongPress = { onCommand(RemoteCommand.MENU_HOLD) },
                            size = faceButtonSize
                        )
                        RemoteButton(
                            icon = Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            onClick = { onCommand(RemoteCommand.PLAY_PAUSE) },
                            size = faceButtonSize
                        )
                        RemoteButton(
                            icon = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Mute",
                            onClick = { onCommand(RemoteCommand.MUTE) },
                            size = faceButtonSize
                        )
                        RemoteButton(
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
                        RemoteButton(
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

                Spacer(modifier = Modifier.height(if (compact) { 8.dp } else { 12.dp }))
            }
        }
    }
}

@Composable
internal fun DPad(
    padSize: Dp,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val padGradient = listOf(CITRUS_TEAL_HIGHLIGHT, CITRUS_DEEP_TEAL)
    val dividerColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.2f)
    val selectSize = if (padSize < 175.dp) { 72.dp } else { 80.dp }
    val tapTargetSize = if (padSize < 175.dp) { 58.dp } else { 64.dp }
    val dPadInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
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
                .dPadTarget(dPadInteractionSource, { onCommand(RemoteCommand.UP) })
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(tapTargetSize)
                .dPadTarget(dPadInteractionSource, { onCommand(RemoteCommand.DOWN) })
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(tapTargetSize)
                .dPadTarget(
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
                .dPadTarget(
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
private fun Modifier.dPadTarget(
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
                onLongPress = onLongPress?.let { callback -> { _ -> callback() } },
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
internal fun RemoteButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
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
internal fun VolumeRocker(
    height: Dp,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
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
