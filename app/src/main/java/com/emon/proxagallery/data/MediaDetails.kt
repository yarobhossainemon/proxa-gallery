package com.emon.proxagallery.data

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * Rich metadata for a single media item, loaded on demand for the Details sheet.
 * Intentionally separate from [MediaItem] so the lightweight paging model stays lean.
 */
@Immutable
data class MediaDetails(
    val id: Long,
    val uri: Uri,
    // ── File ──
    val displayName: String = "",
    val mimeType: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long? = null,
    val isVideo: Boolean = false,
    // ── Dates ──
    val dateTakenMs: Long? = null,
    val dateAddedMs: Long? = null,
    val dateModifiedSec: Long? = null,
    // ── Storage ──
    val bucketId: Long? = null,
    val bucketDisplayName: String? = null,
    val relativePath: String? = null,
    // ── Video ──
    val durationMs: Long? = null,
    val frameRate: Float? = null,
    val bitrate: Long? = null,
    // ── Camera / EXIF (images only) ──
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val exposureMode: String? = null,
    // ── GPS ──
    val latitude: Double? = null,
    val longitude: Double? = null,
)
