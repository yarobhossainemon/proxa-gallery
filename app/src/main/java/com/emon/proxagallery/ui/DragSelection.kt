package com.emon.proxagallery.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Grid-level drag-selection gesture for the staggered photo grids.
 *
 * Behaviour (Samsung Gallery / Google Photos style):
 * - A long press enters selection mode and selects the row under the pressed
 *   photo (anchor). The whole row is selected, not just the single thumbnail.
 * - While the finger moves, EVERY photo whose vertical extent lies between the
 *   anchor row and the current pointer row is selected — across the FULL width
 *   of the grid. This means dragging through a row selects the entire row, and
 *   dragging across several rows selects every row in between (no skipped items,
 *   no gaps), exactly like Samsung Gallery.
 * - Selection is geometry-based: the pointer position is mapped into the grid's
 *   layout coordinate space (via [LazyStaggeredGridState.layoutInfo]
 *   viewportStartOffset) and a full-width vertical band is built from the anchor
 *   to the current pointer. Every laid-out item whose rectangle overlaps that
 *   band is selected. This is correct for the staggered, variable-height layout.
 * - When the pointer lingers near the top/bottom edge the grid auto-scrolls; the
 *   band is recomputed on every auto-scroll tick (using the live viewport offset)
 *   so newly revealed items are swept into the selection automatically.
 * - Works in both directions (up/down). Releasing the finger ends the gesture;
 *   the selection is preserved. Pre-existing selections are kept.
 *
 * Implementation notes — only APIs verified to exist in the project's Compose
 * version are used:
 * - [detectDragGesturesAfterLongPress] (from androidx.compose.foundation.gestures)
 *   handles the long-press → drag sequence and auto-consumes pointer input, so
 *   ordinary scrolling (and per-item tap handling) keeps working when no drag
 *   is active.
 * - [LazyStaggeredGridState.layoutInfo] exposes [androidx.compose.foundation.lazy.LazyLayoutInfo.viewportStartOffset]
 *   and [visibleItemsInfo] (rectangles + absolute indices). The pointer's
 *   viewport-relative Y is converted into the same layout coordinate space using
 *   viewportStartOffset, which updates as the grid scrolls — keeping the band
 *   correct during auto-scroll without any repository queries.
 * - Selecting iterates only the currently visible items (O(visible)), so work
 *   per drag update / auto-scroll tick is bounded regardless of list size.
 * - [LazyStaggeredGridState.scrollBy] (via ScrollableState) drives auto-scroll.
 * - The composable-captured [androidx.compose.runtime.rememberCoroutineScope]
 *   provides the proper [kotlinx.coroutines.CoroutineScope] for the auto-scroll
 *   job (PointerInputScope is a Density, not a CoroutineScope, in Compose 1.10.4).
 *
 * Gesture logic is kept entirely inside this file so HomeScreen stays free of
 * pointer-handling code.
 */
@Composable
fun Modifier.dragSelection(
    gridState: LazyStaggeredGridState,
    density: Density,
    resolveId: (Int) -> Long?,
    onSelectPhoto: (Long) -> Unit
): Modifier {
    // Proper CoroutineScope tied to this composable's lifecycle. PointerInputScope
    // is a Density, NOT a CoroutineScope in Compose 1.10.4, so launching directly
    // inside pointerInput would have no valid scope.
    val scope = rememberCoroutineScope()
    return this.pointerInput(gridState, density) {
        val autoScrollSpeedPx = with(density) { 18.dp.toPx() }
        val autoScrollZonePx = with(density) { 90.dp.toPx() }

        var autoScrollJob: kotlinx.coroutines.Job? = null
        var autoScrollDir = 0f

        // Pointer Y in viewport space (relative to the grid element). Converted
        // to layout coordinates via viewportStartOffset when building the band.
        var anchorVY = 0f
        var currentVY = 0f

        // Maps a viewport-relative pointer Y into the grid's layout coordinate
        // space, matching the space of LazyStaggeredGridItemInfo.offset. Using
        // viewportStartOffset means this stays correct as the grid scrolls.
        // Declared BEFORE updateAutoScroll/stopAutoScroll so it is a backward
        // (legal) reference for selectBand and those callers.
        fun contentY(viewportY: Float): Float =
            viewportY + gridState.layoutInfo.viewportStartOffset

        // Selects every currently-visible item whose vertical span overlaps the
        // full-width band between the anchor and the current pointer. O(visible
        // items) work; runs each time the band changes. Items not yet visible
        // (still loading via Paging) are covered automatically later as they
        // scroll into view and re-trigger selectBand during auto-scroll.
        // Declared BEFORE updateAutoScroll so the call at line ~108 is a
        // backward reference (Kotlin forbids forward references to a local
        // function that captures local variables).
        fun selectBand() {
            val anchorY = contentY(anchorVY)
            val currentY = contentY(currentVY)
            val top = minOf(anchorY, currentY)
            val bottom = maxOf(anchorY, currentY)
            for (item in gridState.layoutInfo.visibleItemsInfo) {
                val itemTop = item.offset.y.toFloat()
                val itemBottom = itemTop + item.size.height
                // Full-width band: only the vertical overlap matters, so entire
                // rows the finger crossed are selected (staggered-safe).
                if (itemBottom >= top && itemTop <= bottom) {
                    resolveId(item.index)?.let { onSelectPhoto(it) }
                }
            }
        }

        fun updateAutoScroll(pointerY: Float) {
            val viewportH = gridState.layoutInfo.viewportSize.height.toFloat()
            autoScrollDir = when {
                pointerY < autoScrollZonePx -> -1f
                pointerY > viewportH - autoScrollZonePx -> 1f
                else -> 0f
            }
            if (autoScrollDir == 0f) {
                autoScrollJob?.cancel()
                autoScrollJob = null
                return
            }
            if (autoScrollJob?.isActive != true) {
                autoScrollJob = scope.launch {
                    while (scope.isActive && autoScrollDir != 0f) {
                        gridState.scrollBy(autoScrollDir * autoScrollSpeedPx)
                        // Re-apply the band after each scroll so items that just
                        // became visible are swept into the selection.
                        selectBand()
                        delay(16)
                    }
                }
            }
        }

        fun stopAutoScroll() {
            autoScrollJob?.cancel()
            autoScrollJob = null
            autoScrollDir = 0f
        }

        detectDragGesturesAfterLongPress(
            onDragStart = { startPos ->
                anchorVY = startPos.y
                currentVY = startPos.y
                // Select the whole row under the long-pressed point immediately.
                selectBand()
                updateAutoScroll(startPos.y)
            },
            onDrag = { change, _ ->
                currentVY = change.position.y
                selectBand()
                updateAutoScroll(change.position.y)
            },
            onDragEnd = { stopAutoScroll() },
            onDragCancel = { stopAutoScroll() }
        )
    }
}
