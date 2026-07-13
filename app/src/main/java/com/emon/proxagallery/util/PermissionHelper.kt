package com.emon.proxagallery.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Single source of truth for media permission logic.
 *
 * Android 13+ (API 33, TIRAMISU):  READ_MEDIA_IMAGES + READ_MEDIA_VIDEO
 * Android 12 and below (API ≤ 32): READ_EXTERNAL_STORAGE
 *
 * The same APK handles both cases via Build.VERSION.SDK_INT at runtime.
 */
object PermissionHelper {

    /**
     * Returns the array of permissions that must be requested on the current device.
     * Pass this directly to [androidx.activity.result.ActivityResultLauncher.launch].
     */
    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /**
     * Returns true if all required media permissions are currently granted.
     * Safe to call from a Composable via [androidx.compose.ui.platform.LocalContext].
     */
    fun hasPermission(context: Context): Boolean =
        requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
}
