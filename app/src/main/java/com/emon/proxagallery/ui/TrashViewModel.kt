package com.emon.proxagallery.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emon.proxagallery.data.DeleteResult
import com.emon.proxagallery.data.GalleryRepository
import com.emon.proxagallery.data.TrashItem
import com.emon.proxagallery.data.TrashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrashViewModel(
    private val trashRepository: TrashRepository,
    private val galleryRepository: GalleryRepository
) : ViewModel() {

    val trashItems: StateFlow<List<TrashItem>> = trashRepository.getTrashItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _effects = MutableSharedFlow<TrashEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<TrashEffect> = _effects.asSharedFlow()

    private var pendingDeleteAll: PendingDeleteAll? = null

    private data class PendingDeleteAll(
        val items: List<TrashItem>,
        val retryUri: Uri? = null
    )

    fun deleteAll() {
        viewModelScope.launch {
            val items = trashItems.value
            if (items.isEmpty()) return@launch

            val ids = items.map { it.id }
            withContext(Dispatchers.IO) {
                trashRepository.deleteTrashRecords(ids)
            }
        }
    }

    /**
     * Restore [item] back into MediaStore.
     *
     * [onRestored] is invoked only when the restore succeeds, so callers (e.g. the
     * gallery) can refresh their own in-memory state immediately. It defaults to a
     * no-op so existing callers are unaffected.
     */
    fun restoreItem(
        item: TrashItem,
        onRestored: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                trashRepository.restoreItem(item)
            }
            if (success) {
                onRestored()
                _effects.tryEmit(TrashEffect.RestoreSuccess)
            } else {
                _effects.tryEmit(
                    TrashEffect.ShowError("Failed to restore item. The file may be corrupted or storage is full.")
                )
            }
        }
    }

    /**
     * Restore every item currently in Recently Deleted back to MediaStore.
     *
     * Reuses the existing [TrashRepository.restoreItem] for each item so the
     * copy-back-to-MediaStore logic is not duplicated. On completion, invokes
     * [onRestored] so callers can refresh the main gallery immediately.
     *
     * A partial failure (some items restored, some failed) still invokes
     * [onRestored] because the gallery should refresh for any successful
     * restores; individual failures are reported via snackbar.
     */
    fun restoreAll(onRestored: () -> Unit = {}) {
        viewModelScope.launch {
            val items = trashItems.value
            if (items.isEmpty()) return@launch

            var failCount = 0
            for (item in items) {
                val success = withContext(Dispatchers.IO) {
                    trashRepository.restoreItem(item)
                }
                if (!success) failCount++
            }

            if (failCount == 0) {
                onRestored()
                _effects.tryEmit(TrashEffect.RestoreAllSuccess)
            } else {
                onRestored()
                _effects.tryEmit(
                    TrashEffect.ShowError("$failCount item(s) could not be restored.")
                )
            }
        }
    }

    /**
     * Permanently delete a single trash item. Used by the unified viewer's
     * "Delete Forever" action after the user confirms via an in-viewer dialog.
     */
    fun deleteForever(itemId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                trashRepository.deleteTrashRecords(listOf(itemId))
            }
        }
    }

    fun confirmDeleteAllAfterPermission() {
        viewModelScope.launch {
            val pending = pendingDeleteAll ?: return@launch
            pendingDeleteAll = null

            if (pending.items.isEmpty()) return@launch

            pending.retryUri?.let { retryUri ->
                withContext(Dispatchers.IO) {
                    galleryRepository.retryDelete(retryUri)
                }
            }

            val ids = pending.items.map { it.id }
            withContext(Dispatchers.IO) {
                trashRepository.deleteTrashRecords(ids)
            }
        }
    }

    fun cancelDeleteAll() {
        pendingDeleteAll = null
    }
}
