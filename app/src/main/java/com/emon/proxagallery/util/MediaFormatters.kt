package com.emon.proxagallery.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

internal fun formatDateSec(timestampSec: Long): String = formatDateMs(timestampSec * 1000L)

internal fun formatDateMs(timestampMs: Long): String {
    val localDateTime = Instant.ofEpochMilli(timestampMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return DateTimeFormatter
        .ofPattern("MMM d, yyyy  –  HH:mm", Locale.getDefault())
        .format(localDateTime)
}

internal fun formatFileSizeBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000     -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000         -> "%.0f KB".format(bytes / 1_000.0)
    else                   -> "$bytes B"
}

internal fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = durationMs / 1_000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3_600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

internal fun formatResolution(width: Int, height: Int): String = "$width \u00D7 $height"

internal fun formatMegapixels(width: Int, height: Int): String {
    val mp = width.toLong() * height.toLong() / 1_000_000.0
    return "${"%.1f".format(mp)} MP"
}

internal fun formatAspectRatio(width: Int, height: Int): String {
    val g = gcd(width, height)
    val w = width / g
    val h = height / g
    return "$w:$h"
}

internal fun formatOrientation(width: Int, height: Int): String = when {
    width > height -> "Landscape"
    height > width -> "Portrait"
    else -> "Square"
}

internal fun formatMegabitsPerSecond(bps: Long): String {
    val mbps = bps / 1_000_000.0
    return "${"%.1f".format(mbps)} Mbps"
}

internal fun formatBitrate(bitrate: Long?): String? {
    if (bitrate == null || bitrate <= 0) return null
    return formatMegabitsPerSecond(bitrate)
}

internal fun formatFrameRate(frameRate: Float?): String? {
    if (frameRate == null || frameRate <= 0) return null
    return "${"%.0f".format(frameRate)} fps"
}

internal fun formatRotation(degrees: Int?): String? {
    if (degrees == null) return null
    return when (degrees) {
        0 -> "0\u00B0 (Normal)"
        90 -> "90\u00B0"
        180 -> "180\u00B0"
        270 -> "270\u00B0"
        else -> "$degrees\u00B0"
    }
}

internal fun formatSampleRate(rate: Int?): String? {
    if (rate == null || rate <= 0) return null
    return "${rate / 1000} kHz"
}

internal fun formatChannels(channels: Int?): String? {
    if (channels == null || channels <= 0) return null
    return when (channels) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> "$channels channels"
    }
}

internal fun formatEstimatedPixelCount(width: Int, height: Int): String {
    val pixels = width.toLong() * height.toLong()
    return when {
        pixels >= 1_000_000 -> "${"%.1f".format(pixels / 1_000_000.0)} MP"
        pixels >= 1_000 -> "${pixels / 1_000}K"
        else -> "$pixels"
    }
}

internal fun formatGpsCoord(lat: Double, lon: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lonDir = if (lon >= 0) "E" else "W"
    return "${"%.6f".format(kotlin.math.abs(lat))}\u00B0 $latDir, ${"%.6f".format(kotlin.math.abs(lon))}\u00B0 $lonDir"
}

internal fun formatGpsDms(lat: Double, lon: Double): String {
    fun toDms(coord: Double): Triple<Int, Int, Double> {
        val abs = kotlin.math.abs(coord)
        val d = abs.toInt()
        val m = ((abs - d) * 60).toInt()
        val s = ((abs - d - m / 60.0) * 3600.0)
        return Triple(d, m, s)
    }
    val latDms = toDms(lat)
    val lonDms = toDms(lon)
    val latDir = if (lat >= 0) "N" else "S"
    val lonDir = if (lon >= 0) "E" else "W"
    return "${latDms.first}\u00B0${latDms.second}'${"%.1f".format(latDms.third)}\"$latDir, ${lonDms.first}\u00B0${lonDms.second}'${"%.1f".format(lonDms.third)}\"$lonDir"
}

internal fun formatFlashDescription(flash: String?): String? = flash

internal fun formatWhiteBalanceDescription(wb: String?): String? = wb

internal fun formatColorSpace(cs: String?): String? = cs

internal fun formatMeteringMode(mode: String?): String? = mode

internal fun formatExposureCompensation(comp: String?): String? {
    if (comp == null) return null
    val parts = comp.split("/")
    if (parts.size == 2) {
        val num = parts[0].toDoubleOrNull()
        val den = parts[1].toDoubleOrNull()
        if (num != null && den != null && den > 0) {
            val ev = num / den
            return "${"%.1f".format(ev)} EV"
        }
    }
    return "${comp} EV"
}

internal fun formatDigitalZoom(zoom: String?): String? {
    if (zoom == null) return null
    val parts = zoom.split("/")
    if (parts.size == 2) {
        val num = parts[0].toDoubleOrNull()
        val den = parts[1].toDoubleOrNull()
        if (num != null && den != null && den > 0) {
            return "${"%.1f".format(num / den)}x"
        }
    }
    return "${zoom}x"
}
