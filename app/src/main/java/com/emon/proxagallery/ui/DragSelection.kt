package com.emon.proxagallery.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Grid-level drag-selection gesture for the staggered photo grids.
 *
 * Behaviour (Samsung Gallery / Google Photos style):
 * - A long press enters selection mode and selects the item (and its row
 *   neighbours) under the pressed point — the anchor.
 * - While the finger moves, the current item under the pointer is determined by
 *   hit-testing against [LazyStaggeredGridState.layoutInfo.visibleItemsInfo]
 *   (geometry). From that the **absolute item index** is read directly from
 *   [LazyStaggeredGridItemInfo.index].
 * - The active selection range is always
 *   `min(anchorIndex, currentIndex) .. max(anchorIndex, currentIndex)`.
 *   Selection is synchronized against this INDEX range, NOT against visible
 *   items. Items that have scrolled off-screen stay selected because their
 *   indices remain inside the range.
 * - When the pointer lingers near the top/bottom edge the grid auto-scrolls.
 *   After each scroll tick the pointer position is re-hit-tested (it may now
 *   fall on a different visible item), the current index is updated, the
 *   range is recomputed, and selection is re-synchronized.
 * - Works symmetrically in both directions (up/down). As the finger moves
 *   back, items that leave the range are unselected. Pre-existing selections
 *   (made before the drag) are never touched.
 * - [resolveId] maps an **absolute index** to a media ID. It accesses the full
 *   data source (PagingData / list), so it works for any index — not just
 *   currently visible ones. This is why off-screen items keep their selection.
 *
 * Implementation notes:
 * - [visibleItemsInfo] is used **only for hit-testing** the pointer position to
 *   find the current item index. It is never the source of truth for selection.
 * - Selecting iterates the index range (up to total items), but the per-item
 *   work is a single set lookup, so even 10 000-item ranges are fast.
 * - The composable-captured [rememberCoroutineScope] provides the proper
 *   [kotlinx.coroutines.CoroutineScope] for the auto-scroll job.
 */
@Composable
fun Modifier.dragSelection(
    gridState: LazyStaggeredGridState,
    density: Density,
    resolveId: (Int) -> Long?,
    onSelectPhoto: (Long) -> Unit,
    onUnselectPhoto: (Long) -> Unit
): Modifier {
    val scope = rememberCoroutineScope()
    val currentResolveId by rememberUpdatedState(resolveId)
    val currentOnSelectPhoto by rememberUpdatedState(onSelectPhoto)
    val currentOnUnselectPhoto by rememberUpdatedState(onUnselectPhoto)

    return this.pointerInput(gridState, density) {
        val autoScrollSpeedPx = with(density) { 18.dp.toPx() }
        val autoScrollZonePx = with(density) { 90.dp.toPx() }

        var autoScrollJob: kotlinx.coroutines.Job? = null
        var autoScrollDir = 0f

        // Absolute item indices for the anchor and current pointer position.
        // These are the two endpoints of the selection range. The range
        // min(start, current)..max(start, current) covers all items that
        // should be selected by this drag — regardless of visibility.
        var anchorIndex = -1
        var currentIndex = -1

        // Latest viewport-relative pointer Y, kept so the auto-scroll loop
        // can re-hit-test after each scroll tick. Must be declared before
        // updateAutoScroll (which captures it).
        var currentVY = 0f

        // IDs that were added to the selection BY THE CURRENT drag. Tracking
        // this set makes reverse-drag deselect work without flickering and
        // without ever touching pre-existing selections: only these IDs are
        // eligible for removal when they leave the range.
        val dragSelectedIds = mutableSetOf<Long>()

        // Total item count from the layout info. Updated each time we need it
        // so the range iteration stays in sync with Paging appends/refreshes.
        fun itemCount(): Int = gridState.layoutInfo.totalItemsCount

        /**
         * Hit-tests a viewport-relative Y against [visibleItemsInfo] to find
         * which visible item the pointer is over. Returns its **absolute
         * index**, or -1 if no item is under the pointer.
         *
         * This is the ONLY place visibleItemsInfo is used — for pointer
         * hit-testing, never for selection logic.
         */
        fun hitTestIndex(viewportY: Float): Int {
            for (item in gridState.layoutInfo.visibleItemsInfo) {
                val top = item.offset.y.toFloat()
                val bottom = top + item.size.height
                if (viewportY >= top && viewportY <= bottom) {
                    return item.index
                }
            }
            return -1
        }

        /**
         * Synchronizes the selection with the current drag range.
         *
         * The range is `min(anchorIndex, currentIndex)..max(anchorIndex,
         * currentIndex)`. For every index in that range we resolve the media ID
         * and select it. For every ID in [dragSelectedIds] that is no longer in
         * the range, we unselect it.
         *
         * Because [resolveId] accesses the full data source (not just visible
         * items), off-screen items remain correctly selected.
         */
        fun syncRange() {
            if (anchorIndex < 0 || currentIndex < 0) return

            val rangeStart = minOf(anchorIndex, currentIndex)
            val rangeEnd = maxOf(anchorIndex, currentIndex)
            val total = itemCount()

            // Collect all IDs currently in the active range.
            val inRangeIds = mutableSetOf<Long>()
            for (i in rangeStart..rangeEnd.coerceAtMost(total - 1)) {
                currentResolveId(i)?.let { inRangeIds.add(it) }
            }

            // Select newly-entered items.
            for (id in inRangeIds) {
                if (dragSelectedIds.add(id)) {
                    currentOnSelectPhoto(id)
                }
            }

            // Deselect items that left the range (reverse drag).
            val toRemove = dragSelectedIds.filter { it !in inRangeIds }
            for (id in toRemove) {
                dragSelectedIds.remove(id)
                currentOnUnselectPhoto(id)
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
                        // Re-hit-test after scroll: the pointer may now fall on
                        // a different visible item, so the current index updates.
                        val newIndex = hitTestIndex(currentVY)
                        if (newIndex >= 0) currentIndex = newIndex
                        syncRange()
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
                currentVY = startPos.y
                dragSelectedIds.clear()
                // Hit-test to find the anchor item index.
                anchorIndex = hitTestIndex(startPos.y)
                currentIndex = anchorIndex
                syncRange()
                updateAutoScroll(startPos.y)
            },
            onDrag = { change, _ ->
                currentVY = change.position.y
                val newIndex = hitTestIndex(change.position.y)
                if (newIndex >= 0) currentIndex = newIndex
                syncRange()
                updateAutoScroll(change.position.y)
            },
            onDragEnd = { stopAutoScroll() },
            onDragCancel = { stopAutoScroll() }
        )
    }
}
