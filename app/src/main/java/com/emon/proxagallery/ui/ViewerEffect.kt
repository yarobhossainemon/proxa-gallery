package com.emon.proxagallery.ui

import android.content.IntentSender
import android.net.Uri

/**
 * One-time side-effects emitted by [GalleryViewModel] to the PhotoViewer UI via a SharedFlow.
 * Using a sealed class keeps the contract explicit and extensible.
 */
sealed class ViewerEffect {
    /**
     * The OS must show a system-level delete-permission dialog.
     * The UI launches it via [ActivityResultContracts.StartIntentSenderForResult].
     *
     * [retryUri] is null on Android 11+ — the OS performs the physical deletion during the dialog.
     * [retryUri] is non-null on Android 10 — the app must call [GalleryRepository.retryDelete]
     * after the user grants permission via a [RecoverableSecurityException].
     */
    data class LaunchSystemDeleteDialog(
        val intentSender: IntentSender,
        val retryUri: Uri? = null
    ) : ViewerEffect()

    /** All photos in the viewer have been removed; navigate back immediately. */
    object NavigateBack : ViewerEffect()

    /**
     * A photo was successfully deleted.
     * The gallery grid should call [LazyPagingItems.refresh] on its paging flows.
     */
    object PhotoDeleted : ViewerEffect()
}
