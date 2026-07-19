package com.emon.proxagallery.ui

import android.content.IntentSender
import android.net.Uri

sealed class TrashEffect {
    data class LaunchSystemDeleteDialog(
        val intentSender: IntentSender,
        val retryUri: Uri? = null
    ) : TrashEffect()

    data class ShowError(val message: String) : TrashEffect()

    object RestoreSuccess : TrashEffect()
}
