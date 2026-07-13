package com.emon.proxagallery.data

import android.content.IntentSender
import android.net.Uri

/**
 * Represents the outcome of a MediaStore delete operation.
 *
 * - [Success]              File was deleted immediately (Android 10, own files).
 * - [RequiresPermission]   System must present a confirmation dialog.
 *                          [retryUri] is null on Android 11+ — the OS handles deletion
 *                          after the user grants access.  Non-null on Android 10, where
 *                          the app must retry [GalleryRepository.retryDelete] afterwards.
 * - [Error]                Deletion failed; contains a human-readable reason.
 */
sealed class DeleteResult {
    object Success : DeleteResult()

    data class RequiresPermission(
        val intentSender: IntentSender,
        /** Non-null only on Android 10 (RecoverableSecurityException path). */
        val retryUri: Uri?
    ) : DeleteResult()

    data class Error(val message: String) : DeleteResult()
}
