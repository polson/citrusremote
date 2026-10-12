package com.reggiesoft.citrusremote.ui.control

import com.reggiesoft.citrusremote.data.model.RemotePlacement
import kotlin.math.min

internal data class RemoteBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

internal data class RemotePlacementArea(
    val width: Float,
    val height: Float,
    val baseWidth: Float,
    val baseHeight: Float
) {
    private val aspectRatio: Float get() = baseWidth / baseHeight
    private val maximumWidth: Float get() = min(width, height * aspectRatio)
    private val minimumWidth: Float get() = min(baseWidth, maximumWidth)

    fun bounds(placement: RemotePlacement): RemoteBounds {
        val remoteWidth = placement.width.coerceIn(minimumWidth, maximumWidth)
        val remoteHeight = remoteWidth / aspectRatio
        return RemoteBounds(
            x = (width - remoteWidth) * placement.horizontalPosition,
            y = (height - remoteHeight) * placement.verticalPosition,
            width = remoteWidth,
            height = remoteHeight
        )
    }

    fun move(placement: RemotePlacement, dx: Float, dy: Float): RemotePlacement {
        val bounds = bounds(placement)
        return placement.copy(
            horizontalPosition = position(
                offset = bounds.x + dx,
                travel = width - bounds.width,
                current = placement.horizontalPosition
            ),
            verticalPosition = position(
                offset = bounds.y + dy,
                travel = height - bounds.height,
                current = placement.verticalPosition
            ),
            width = bounds.width
        )
    }

    fun resize(placement: RemotePlacement, dx: Float, dy: Float): RemotePlacement {
        val bounds = bounds(placement)
        val widthChange = (dx + dy / aspectRatio) / (1f + 1f / (aspectRatio * aspectRatio))
        val availableWidth = min(width - bounds.x, (height - bounds.y) * aspectRatio)
        val remoteWidth = (bounds.width + widthChange).coerceIn(minimumWidth, availableWidth)
        return placement.copy(
            horizontalPosition = position(
                offset = bounds.x,
                travel = width - remoteWidth,
                current = placement.horizontalPosition
            ),
            verticalPosition = position(
                offset = bounds.y,
                travel = height - remoteWidth / aspectRatio,
                current = placement.verticalPosition
            ),
            width = remoteWidth
        )
    }

    private fun position(offset: Float, travel: Float, current: Float): Float {
        return if (travel > 0f) {
            (offset / travel).coerceIn(0f, 1f)
        } else {
            current
        }
    }
}
