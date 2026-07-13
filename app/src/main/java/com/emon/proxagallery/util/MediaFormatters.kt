package com.emon.proxagallery.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Format a second timestamp as "MMM d, yyyy – HH:mm". */
internal fun formatDateSec(timestampSec: Long): String = formatDateMs(timestampSec * 1000L)

/** Format a millisecond timestamp as "MMM d, yyyy – HH:mm". */
internal fun formatDateMs(timestampMs: Long): String {
    val localDateTime = Instant.ofEpochMilli(timestampMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return DateTimeFormatter
        .ofPattern("MMM d, yyyy  –  HH:mm", Locale.getDefault())
        .format(localDateTime)
}

/** Format a byte count as a human-readable string (B / KB / MB). */
internal fun formatFileSizeBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000     -> "%.0f KB".format(bytes / 1_000.0)
    else               -> "$bytes B"
}

/** Format a millisecond duration as mm:ss or h:mm:ss. */
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
