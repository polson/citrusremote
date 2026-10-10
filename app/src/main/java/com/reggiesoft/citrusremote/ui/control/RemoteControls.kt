package com.reggiesoft.citrusremote.ui.control

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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reggiesoft.citrusremote.data.model.RemoteCommand
import com.reggiesoft.citrusremote.ui.theme.CITRUS_DEEP_TEAL
import com.reggiesoft.citrusremote.ui.theme.CITRUS_TEAL_HIGHLIGHT
import com.reggiesoft.citrusremote.ui.theme.CITRUS_WARM_CREAM

@Composable
internal fun DPad(
    padSize: Dp,
    onCommand: (RemoteCommand) -> Unit,
) {
    val padGradient = listOf(CITRUS_TEAL_HIGHLIGHT, CITRUS_DEEP_TEAL)
    val dividerColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.2f)
    val selectSize = if (padSize < 175.dp) { 72.dp } else { 80.dp }
    val tapTargetSize = if (padSize < 175.dp) { 58.dp } else { 64.dp }
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
internal fun RemoteFaceButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    size: Dp,
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
internal fun VolumeRocker(
    height: Dp,
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
