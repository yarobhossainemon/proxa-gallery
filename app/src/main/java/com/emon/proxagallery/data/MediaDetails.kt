package com.emon.proxagallery.data

import android.net.Uri
import androidx.compose.runtime.Immutable

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
    val storageType: String? = null,
    // ── Video ──
    val durationMs: Long? = null,
    val codec: String? = null,
    val audioCodec: String? = null,
    val frameRate: Float? = null,
    val bitrate: Long? = null,
    val rotation: Int? = null,
    val sampleRate: Int? = null,
    val audioChannels: Int? = null,
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
    val exposureCompensation: String? = null,
    val meteringMode: String? = null,
    val digitalZoom: String? = null,
    val colorSpace: String? = null,
    val software: String? = null,
    val artist: String? = null,
    val copyright: String? = null,
    val bitsPerPixel: Int? = null,
    // ── Orientation ──
    val orientationDegrees: Int? = null,
    // ── Color / HDR ──
    val hdr: Boolean? = null,
    val wideGamut: Boolean? = null,
    // ── GPS ──
    val latitude: Double? = null,
    val longitude: Double? = null,
)
