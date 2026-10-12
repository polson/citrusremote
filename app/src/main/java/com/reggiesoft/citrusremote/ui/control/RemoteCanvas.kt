package com.reggiesoft.citrusremote.ui.control

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.reggiesoft.citrusremote.data.model.MINIMUM_REMOTE_WIDTH
import com.reggiesoft.citrusremote.data.model.RemoteCommand
import com.reggiesoft.citrusremote.data.model.RemotePlacement
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
internal fun RemoteCanvas(
    uiState: ControlUiState,
    placement: RemotePlacement,
    isEditing: Boolean,
    onPlacementChange: (RemotePlacement) -> Unit,
    onCommand: (RemoteCommand) -> Unit,
    onKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val viewportHeight = maxHeight
        SubcomposeLayout(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) { constraints ->
            val margin = CANVAS_MARGIN.dp.roundToPx()
            val remote = subcompose("remote") {
                Box(
                    modifier = if (isEditing) {
                        Modifier.clearAndSetSemantics { }
                    } else {
                        Modifier.testTag("remote")
                    }
                ) {
                    RemoteControl(
                        uiState = uiState,
                        onCommand = { command ->
                            if (!isEditing) {
                                onCommand(command)
                            }
                        },
                        onKeyboard = {
                            if (!isEditing) {
                                onKeyboard()
                            }
                        }
                    )
                }
            }.single().measure(Constraints.fixedWidth(MINIMUM_REMOTE_WIDTH.dp.roundToPx()))

            val canvasHeight = max(viewportHeight.roundToPx(), remote.height + margin * 2)
            val area = RemotePlacementArea(
                width = (constraints.maxWidth - margin * 2).coerceAtLeast(1) / density,
                height = (canvasHeight - margin * 2) / density,
                baseWidth = remote.width / density,
                baseHeight = remote.height / density
            )
            val bounds = area.bounds(placement)
            val remoteWidth = (bounds.width * density).roundToInt()
            val remoteHeight = (bounds.height * density).roundToInt()
            val scale = bounds.width / area.baseWidth
            val outline = subcompose("outline") {
                if (isEditing) {
                    RemotePlacementOutline(
                        scale = scale,
                        onMove = { delta ->
                            onPlacementChange(
                                area.move(placement = placement, dx = delta.x / density, dy = delta.y / density)
                            )
                        },
                        onResize = { delta ->
                            onPlacementChange(
                                area.resize(placement = placement, dx = delta.x / density, dy = delta.y / density)
                            )
                        }
                    )
                }
            }.map { measurable ->
                measurable.measure(
                    Constraints.fixed(width = remoteWidth + margin * 2, height = remoteHeight + margin * 2)
                )
            }

            layout(width = constraints.maxWidth, height = canvasHeight) {
                val x = margin + (bounds.x * density).roundToInt()
                val y = margin + (bounds.y * density).roundToInt()
                remote.placeWithLayer(x = x, y = y) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0f)
                }
                for (overlay in outline) {
                    overlay.place(x = x - margin, y = y - margin)
                }
            }
        }
    }
}

@Composable
private fun RemotePlacementOutline(
    scale: Float,
    onMove: (Offset) -> Unit,
    onResize: (Offset) -> Unit
) {
    val move by rememberUpdatedState(onMove)
    val resize by rememberUpdatedState(onResize)
    val moveStep = with(LocalDensity.current) { MOVE_STEP.dp.toPx() }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(CANVAS_MARGIN.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("remote-move")
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(34.dp * scale)
                    )
                    .semantics {
                        contentDescription = "Move remote"
                        customActions = listOf(
                            CustomAccessibilityAction(label = "Move left") {
                                move(Offset(x = -moveStep, y = 0f))
                                true
                            },
                            CustomAccessibilityAction(label = "Move right") {
                                move(Offset(x = moveStep, y = 0f))
                                true
                            },
                            CustomAccessibilityAction(label = "Move up") {
                                move(Offset(x = 0f, y = -moveStep))
                                true
                            },
                            CustomAccessibilityAction(label = "Move down") {
                                move(Offset(x = 0f, y = moveStep))
                                true
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, delta ->
                            change.consume()
                            move(delta)
                        }
                    }
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(48.dp)
                .testTag("remote-resize")
                .semantics {
                    contentDescription = "Resize remote"
                    customActions = listOf(
                        CustomAccessibilityAction(label = "Make larger") {
                            resize(Offset(x = moveStep, y = moveStep))
                            true
                        },
                        CustomAccessibilityAction(label = "Make smaller") {
                            resize(Offset(x = -moveStep, y = -moveStep))
                            true
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, delta ->
                        change.consume()
                        resize(delta)
                    }
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.OpenInFull, contentDescription = null)
            }
        }
    }
}

private const val CANVAS_MARGIN = 24
private const val MOVE_STEP = 24
