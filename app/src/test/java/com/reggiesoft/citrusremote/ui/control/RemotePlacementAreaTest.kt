package com.reggiesoft.citrusremote.ui.control

import com.reggiesoft.citrusremote.data.model.RemotePlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePlacementAreaTest {
    private val area = RemotePlacementArea(width = 340f, height = 700f, baseWidth = 208f, baseHeight = 483f)

    @Test
    fun `dragging stops at every edge`() {
        val start = RemotePlacement()
        val topLeft = area.bounds(area.move(placement = start, dx = -10000f, dy = -10000f))
        val bottomRight = area.bounds(area.move(placement = start, dx = 10000f, dy = 10000f))

        assertEquals(0f, topLeft.x, TOLERANCE)
        assertEquals(0f, topLeft.y, TOLERANCE)
        assertEquals(area.width, bottomRight.x + bottomRight.width, TOLERANCE)
        assertEquals(area.height, bottomRight.y + bottomRight.height, TOLERANCE)
    }

    @Test
    fun `resizing preserves proportions and the top left corner`() {
        val start = RemotePlacement(horizontalPosition = 0.2f, verticalPosition = 0.1f)
        val before = area.bounds(start)
        val after = area.bounds(area.resize(placement = start, dx = 30f, dy = 70f))

        assertTrue(after.width > before.width)
        assertEquals(before.x, after.x, TOLERANCE)
        assertEquals(before.y, after.y, TOLERANCE)
        assertEquals(area.baseWidth / area.baseHeight, after.width / after.height, TOLERANCE)
    }

    @Test
    fun `all placements stay in bounds when resized in either direction`() {
        for (horizontalPosition in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            for (verticalPosition in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                for (width in listOf(208f, 240f, 400f)) {
                    for (delta in listOf(-10000f, 10000f)) {
                        val start = RemotePlacement(
                            horizontalPosition = horizontalPosition,
                            verticalPosition = verticalPosition,
                            width = width
                        )
                        val bounds = area.bounds(area.resize(placement = start, dx = delta, dy = delta))
                        assertTrue(bounds.width >= area.baseWidth - TOLERANCE)
                        assertTrue(bounds.x >= -TOLERANCE)
                        assertTrue(bounds.y >= -TOLERANCE)
                        assertTrue(bounds.x + bounds.width <= area.width + TOLERANCE)
                        assertTrue(bounds.y + bounds.height <= area.height + TOLERANCE)
                    }
                }
            }
        }
    }

    @Test
    fun `saved placement adapts to a different available area`() {
        val saved = RemotePlacement(horizontalPosition = 1f, verticalPosition = 0.5f, width = 250f)
        val narrowerArea = area.copy(width = 220f, height = 483f)
        val bounds = narrowerArea.bounds(saved)

        assertEquals(narrowerArea.width, bounds.x + bounds.width, TOLERANCE)
        assertEquals(narrowerArea.height, bounds.height, TOLERANCE)
        assertEquals(250f, saved.width, TOLERANCE)
    }

    @Test
    fun `dragging with no available travel preserves a valid position`() {
        val exactArea = area.copy(width = 208f, height = 483f)
        val start = RemotePlacement(horizontalPosition = 0.75f, verticalPosition = 0.5f, width = 208f)
        val moved = exactArea.move(placement = start, dx = 100f, dy = -100f)

        assertEquals(start, moved)
        assertEquals(0f, exactArea.bounds(moved).x, TOLERANCE)
        assertEquals(0f, exactArea.bounds(moved).y, TOLERANCE)
    }

    @Test
    fun `fractional screen densities keep resizing valid at the bottom edge`() {
        for (height in listOf(483.0476f, 524.381f, 700.5f)) {
            val fractionalArea = area.copy(width = 328.7619f, height = height, baseHeight = 483.0476f)
            for (width in listOf(208f, 228f, 400f)) {
                val start = RemotePlacement(horizontalPosition = 1f, verticalPosition = 1f, width = width)
                val resized = fractionalArea.resize(placement = start, dx = -10000f, dy = -10000f)
                val bounds = fractionalArea.bounds(resized)
                assertEquals(208f, bounds.width, TOLERANCE)
                assertTrue(bounds.x + bounds.width <= fractionalArea.width + TOLERANCE)
                assertTrue(bounds.y + bounds.height <= fractionalArea.height + TOLERANCE)
            }
        }
    }
}

private const val TOLERANCE = 0.001f
