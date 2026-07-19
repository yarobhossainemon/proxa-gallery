package com.emon.proxagallery.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lens
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.emon.proxagallery.data.MediaDetails
import com.emon.proxagallery.ui.theme.FavoriteRed
import com.emon.proxagallery.ui.theme.MapPinBlue
import com.emon.proxagallery.ui.theme.PhotoBadgeGreen
import com.emon.proxagallery.ui.theme.VideoBadgeBlue
import com.emon.proxagallery.util.formatAspectRatio
import com.emon.proxagallery.util.formatDateMs
import com.emon.proxagallery.util.formatDateSec
import com.emon.proxagallery.util.formatDurationMs
import com.emon.proxagallery.util.formatEstimatedPixelCount
import com.emon.proxagallery.util.formatFileSizeBytes
import com.emon.proxagallery.util.formatFrameRate
import com.emon.proxagallery.util.formatMegabitsPerSecond
import com.emon.proxagallery.util.formatMegapixels
import com.emon.proxagallery.util.formatOrientation
import com.emon.proxagallery.util.formatResolution

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsSheet(
    details: MediaDetails,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            PreviewHeader(details, isFavorite, onToggleFavorite)
            BasicInfoSection(details)
            if (details.isVideo) VideoInfoSection(details)
            CameraInfoSection(details)
            if (details.latitude != null && details.longitude != null) {
                LocationSection(details)
            }
            StorageSection(details)
            TechnicalSection(details)
        }
    }
}

// ── Preview Header ──────────────────────────────────────────────────────────

@Composable
private fun PreviewHeader(
    details: MediaDetails,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val request = remember(details.uri) {
        ImageRequest.Builder(context)
            .data(details.uri)
            .crossfade(true)
            .size(480)
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MediaTypeBadge(isVideo = details.isVideo)
                    if (details.width != null && details.height != null) {
                        Text(
                            text = formatResolution(details.width, details.height),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    details.fileSize?.let {
                        Text(
                            text = formatFileSizeBytes(it),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    details.dateTakenMs?.let {
                        Text(
                            text = formatDateMs(it),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite
                    else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Favorited" else "Not favorited",
                    tint = if (isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun MediaTypeBadge(isVideo: Boolean) {
    val (icon, label, bg) = if (isVideo) {
        Triple(Icons.Rounded.Videocam, "Video", VideoBadgeBlue)
    } else {
        Triple(Icons.Rounded.Photo, "Photo", PhotoBadgeGreen)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = bg,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = bg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Section Card ────────────────────────────────────────────────────────────

@Composable
private fun DetailsSection(
    title: String,
    icon: ImageVector,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .animateContentSize(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        rotationZ = if (expanded) 0f else -90f
                    }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(250)) +
                slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                content()
            }
        }
    }
}

// ── Info Row ────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isLast: Boolean = false
) {
    val clipboard = LocalClipboardManager.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = { clipboard.setText(AnnotatedString(value)) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy $label",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
    if (!isLast) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ── Sections ────────────────────────────────────────────────────────────────

@Composable
private fun BasicInfoSection(details: MediaDetails) {
    DetailsSection(title = "Basic Information", icon = Icons.Rounded.Info) {
        DetailRow(Icons.Rounded.Description, "Filename", details.displayName, isLast = false)
        val ext = details.displayName.substringAfterLast('.', "")
        if (ext.isNotBlank()) {
            DetailRow(Icons.Rounded.Code, "Extension", ext, isLast = false)
        }
        if (details.mimeType.isNotBlank()) {
            DetailRow(Icons.Rounded.Info, "MIME type", details.mimeType, isLast = false)
        }
        DetailRow(
            Icons.Rounded.Photo, "Media type",
            if (details.isVideo) "Video" else "Image", isLast = false
        )
        if (details.width != null && details.height != null) {
            DetailRow(
                Icons.Rounded.Image, "Resolution",
                formatResolution(details.width, details.height), isLast = false
            )
            DetailRow(
                Icons.Rounded.Crop, "Megapixels",
                formatMegapixels(details.width, details.height), isLast = false
            )
            DetailRow(
                Icons.Rounded.Crop, "Aspect ratio",
                formatAspectRatio(details.width, details.height), isLast = false
            )
            DetailRow(
                Icons.Rounded.SwapHoriz, "Orientation",
                formatOrientation(details.width, details.height), isLast = false
            )
        }
        details.dateTakenMs?.let {
            DetailRow(Icons.Rounded.CalendarMonth, "Date taken", formatDateMs(it), isLast = false)
        }
        details.dateAddedMs?.let {
            DetailRow(Icons.Rounded.DateRange, "Date added", formatDateMs(it), isLast = false)
        }
        details.dateModifiedSec?.let {
            DetailRow(Icons.Rounded.DateRange, "Date modified", formatDateSec(it), isLast = false)
        }
        details.bucketDisplayName?.let {
            DetailRow(Icons.Rounded.Folder, "Folder", it, isLast = false)
        }
        details.relativePath?.let {
            DetailRow(Icons.Rounded.Folder, "Relative path", it, isLast = false)
        }
        details.storageType?.let {
            DetailRow(Icons.Rounded.Storage, "Storage", it, isLast = true)
        }
    }
}

@Composable
private fun CameraInfoSection(details: MediaDetails) {
    val hasCameraInfo = details.cameraMake != null ||
        details.cameraModel != null ||
        details.lensModel != null ||
        details.iso != null ||
        details.shutterSpeed != null ||
        details.aperture != null ||
        details.focalLength != null ||
        details.flash != null ||
        details.whiteBalance != null ||
        details.exposureMode != null ||
        details.exposureCompensation != null ||
        details.meteringMode != null ||
        details.digitalZoom != null ||
        details.colorSpace != null ||
        details.software != null ||
        details.artist != null ||
        details.copyright != null

    if (!hasCameraInfo) return

    DetailsSection(title = "Camera Information", icon = Icons.Rounded.CameraAlt) {
        details.cameraMake?.let {
            DetailRow(Icons.Rounded.CameraAlt, "Camera make", it, isLast = false)
        }
        details.cameraModel?.let {
            DetailRow(Icons.Rounded.CameraAlt, "Camera model", it, isLast = false)
        }
        details.lensModel?.let {
            DetailRow(Icons.Rounded.Lens, "Lens", it, isLast = false)
        }
        details.iso?.let {
            DetailRow(Icons.Rounded.Lens, "ISO", it, isLast = false)
        }
        details.shutterSpeed?.let {
            DetailRow(Icons.Rounded.Timer, "Shutter speed", it, isLast = false)
        }
        details.aperture?.let {
            DetailRow(Icons.Rounded.Lens, "Aperture", it, isLast = false)
        }
        details.focalLength?.let {
            DetailRow(Icons.Rounded.Lens, "Focal length", it, isLast = false)
        }
        details.flash?.let {
            DetailRow(Icons.Rounded.FlashOn, "Flash", it, isLast = false)
        }
        details.whiteBalance?.let {
            DetailRow(Icons.Rounded.Settings, "White balance", it, isLast = false)
        }
        details.exposureMode?.let {
            DetailRow(Icons.Rounded.Settings, "Exposure mode", it, isLast = false)
        }
        details.exposureCompensation?.let {
            DetailRow(Icons.Rounded.Settings, "Exposure compensation", it, isLast = false)
        }
        details.meteringMode?.let {
            DetailRow(Icons.Rounded.Settings, "Metering mode", it, isLast = false)
        }
        details.digitalZoom?.let {
            DetailRow(Icons.Rounded.Settings, "Digital zoom", it, isLast = false)
        }
        details.colorSpace?.let {
            DetailRow(Icons.Rounded.Settings, "Color space", it, isLast = false)
        }
        details.software?.let {
            DetailRow(Icons.Rounded.Code, "Software", it, isLast = false)
        }
        details.artist?.let {
            DetailRow(Icons.Rounded.Person, "Artist", it, isLast = false)
        }
        details.copyright?.let {
            DetailRow(Icons.Rounded.Settings, "Copyright", it, isLast = true)
        }
    }
}

@Composable
private fun VideoInfoSection(details: MediaDetails) {
    val hasVideoInfo = details.durationMs != null ||
        details.codec != null ||
        details.frameRate != null ||
        details.bitrate != null ||
        details.rotation != null

    if (!hasVideoInfo) return

    DetailsSection(title = "Video Information", icon = Icons.Rounded.Videocam) {
        details.durationMs?.let {
            DetailRow(Icons.Rounded.Timer, "Duration", formatDurationMs(it), isLast = false)
        }
        details.codec?.let {
            DetailRow(Icons.Rounded.Code, "Codec", it, isLast = false)
        }
        details.frameRate?.let {
            val fps = formatFrameRate(it) ?: "${it}fps"
            DetailRow(Icons.Rounded.Speed, "Frame rate", fps, isLast = false)
        }
        details.bitrate?.let {
            DetailRow(
                Icons.Rounded.Speed, "Bitrate",
                formatMegabitsPerSecond(it), isLast = false
            )
        }
        if (details.width != null && details.height != null) {
            DetailRow(
                Icons.Rounded.Image, "Resolution",
                formatResolution(details.width, details.height), isLast = false
            )
        }
        details.rotation?.let {
            DetailRow(Icons.Rounded.SwapHoriz, "Rotation", "${it}\u00B0", isLast = true)
        }
    }
}

@Composable
private fun LocationSection(details: MediaDetails) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lat = details.latitude ?: return
    val lon = details.longitude ?: return
    val geoUri = Uri.parse("geo:$lat,$lon")
    val coordText = "${"%.6f".format(lat)}, ${"%.6f".format(lon)}"

    DetailsSection(title = "Location", icon = Icons.Rounded.LocationOn) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MapPinBlue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = coordText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionChip(
                text = "Open in Maps",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                },
                modifier = Modifier.weight(1f)
            )
            ActionChip(
                text = "Copy",
                onClick = { clipboard.setText(AnnotatedString(coordText)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StorageSection(details: MediaDetails) {
    DetailsSection(title = "Storage", icon = Icons.Rounded.Storage) {
        details.fileSize?.let {
            DetailRow(Icons.Rounded.Storage, "File size", formatFileSizeBytes(it), isLast = false)
        }
        details.relativePath?.let {
            DetailRow(Icons.Rounded.Folder, "Path", it, isLast = false)
        }
        details.bucketDisplayName?.let {
            DetailRow(Icons.Rounded.Folder, "Album", it, isLast = false)
        }
        details.storageType?.let {
            DetailRow(Icons.Rounded.Storage, "Storage type", it, isLast = true)
        }
    }
}

@Composable
private fun TechnicalSection(details: MediaDetails) {
    DetailsSection(title = "Technical", icon = Icons.Rounded.Settings) {
        if (details.width != null && details.height != null) {
            DetailRow(
                Icons.Rounded.Image, "Megapixels",
                formatMegapixels(details.width, details.height), isLast = false
            )
            DetailRow(
                Icons.Rounded.Crop, "Aspect ratio",
                formatAspectRatio(details.width, details.height), isLast = false
            )
            DetailRow(
                Icons.Rounded.SwapHoriz, "Orientation",
                formatOrientation(details.width, details.height), isLast = false
            )
            DetailRow(
                Icons.Rounded.Image, "Estimated pixels",
                formatEstimatedPixelCount(details.width, details.height), isLast = false
            )
        }
        details.fileSize?.let {
            DetailRow(
                Icons.Rounded.Storage, "Compressed size",
                formatFileSizeBytes(it), isLast = false
            )
        }
        details.bitsPerPixel?.let {
            DetailRow(Icons.Rounded.Settings, "Bits per pixel", "$it bpp", isLast = false)
        }
        details.orientationDegrees?.let {
            DetailRow(Icons.Rounded.SwapHoriz, "Rotation", "${it}\u00B0", isLast = false)
        }
        details.colorSpace?.let {
            DetailRow(Icons.Rounded.Settings, "Color space", it, isLast = false)
        }
        details.hdr?.let {
            DetailRow(Icons.Rounded.Settings, "HDR", if (it) "Yes" else "No", isLast = false)
        }
        details.wideGamut?.let {
            DetailRow(
                Icons.Rounded.Settings, "Wide gamut",
                if (it) "Yes" else "No", isLast = true
            )
        }
    }
}
