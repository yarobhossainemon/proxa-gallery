package com.emon.proxagallery.ui

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.MediaItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PhotoViewerScreen(
    photoIds: List<Long>,
    initialPhotoId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    favoriteKeys: Set<String> = emptySet(),
    albums: List<Album> = emptyList(),
    onToggleFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    getMediaItem: suspend (Long) -> MediaItem?
) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var isUiVisible by remember { mutableStateOf(false) }

    val activity = LocalActivity.current
    val insetsController = remember {
        activity?.window?.let { WindowInsetsControllerCompat(it, it.decorView) }
    }

    LaunchedEffect(Unit) {
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    LaunchedEffect(isUiVisible) {
        if (isUiVisible) {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
    ) {
        if (photoIds.isNotEmpty()) {
            val initialPage = photoIds.indexOf(initialPhotoId).coerceAtLeast(0)
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { photoIds.size }
            )
            val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                val rawScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
                val snappedScale = if (rawScale - MIN_SCALE <= SCALE_EPSILON) MIN_SCALE else rawScale

                scale = snappedScale
                offset = (offset + panChange * snappedScale).coerceWithinBounds(snappedScale, imageSize)
            }

            LaunchedEffect(pagerState.currentPage) {
                scale = MIN_SCALE
                offset = Offset.Zero
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = scale == MIN_SCALE,
                key = { index -> photoIds[index] }
            ) { page ->
                val photoId = photoIds[page]
                var mediaItem by remember(photoId) { mutableStateOf<MediaItem?>(null) }
                LaunchedEffect(photoId) {
                    mediaItem = getMediaItem(photoId)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val currentMediaItem = mediaItem
                    if (currentMediaItem != null) {
                        val context = LocalContext.current
                        val request = remember(currentMediaItem.uri) {
                            ImageRequest.Builder(context)
                                .data(currentMediaItem.uri)
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = currentMediaItem.displayName.takeIf { it.isNotBlank() },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { imageSize = it }
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .pointerInput(currentMediaItem.id) {
                                    detectTapGestures(
                                        onTap = { isUiVisible = !isUiVisible },
                                        onDoubleTap = {
                                            if (scale == MIN_SCALE) {
                                                scale = DOUBLE_TAP_SCALE
                                            } else {
                                                scale = MIN_SCALE
                                                offset = Offset.Zero
                                            }
                                        }
                                    )
                                }
                                .transformable(
                                    state = transformableState,
                                    canPan = { scale > MIN_SCALE },
                                    lockRotationOnZoomPan = true
                                )
                        )

                        if (currentMediaItem.isVideo) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(currentMediaItem.uri, "video/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    activity?.startActivity(intent)
                                },
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play video",
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            val currentPhotoId = photoIds.getOrNull(pagerState.currentPage)
            var currentMedia by remember(currentPhotoId) { mutableStateOf<MediaItem?>(null) }
            LaunchedEffect(currentPhotoId) {
                if (currentPhotoId != null) {
                    currentMedia = getMediaItem(currentPhotoId)
                }
            }

            val mediaItemToDisplay = currentMedia
            if (mediaItemToDisplay != null) {
                // Top controls floating glass bar
                AnimatedVisibility(
                    visible = isUiVisible,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xD0161A22))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(28.dp))
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = mediaItemToDisplay.displayName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                onToggleFavorite(mediaItemToDisplay.id, mediaItemToDisplay.isVideo)
                            }
                        ) {
                            val favKey = if (mediaItemToDisplay.isVideo) "v:${mediaItemToDisplay.id}" else "i:${mediaItemToDisplay.id}"
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Favorite",
                                tint = if (favKey in favoriteKeys) Color(0xFFFF1744) else Color.White
                            )
                        }
                    }
                }

                // Bottom info card floating glass panel
                AnimatedVisibility(
                    visible = isUiVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    PhotoInfoCard(
                        mediaItem = mediaItemToDisplay,
                        favoriteKeys = favoriteKeys,
                        onToggleFavorite = onToggleFavorite,
                        albums = albums,
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoInfoCard(
    mediaItem: MediaItem,
    favoriteKeys: Set<String>,
    onToggleFavorite: (Long, Boolean) -> Unit,
    albums: List<Album>,
    modifier: Modifier = Modifier
) {
    val favKey = if (mediaItem.isVideo) "v:${mediaItem.id}" else "i:${mediaItem.id}"
    val isFav = favKey in favoriteKeys
    val albumName = remember(mediaItem.bucketId, albums) {
        albums.find { it.id == mediaItem.bucketId }?.displayName
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xD0161A22))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaItem.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (mediaItem.isVideo) "Video" else "Photo",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(mediaItem.id, mediaItem.isVideo) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color(0xFFFF1744) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            mediaItem.dateTakenMs?.let { dateMs ->
                InfoRow(label = "Date", value = formatDate(dateMs))
            }
            if (mediaItem.width != null && mediaItem.height != null) {
                InfoRow(label = "Resolution", value = "${mediaItem.width} × ${mediaItem.height}")
            }
            mediaItem.fileSize?.let { size ->
                InfoRow(label = "Size", value = formatFileSize(size))
            }
            if (mediaItem.mimeType.isNotBlank()) {
                InfoRow(label = "Mime Type", value = mediaItem.mimeType)
            }
            albumName?.let { album ->
                InfoRow(label = "Album", value = album)
            }
            if (mediaItem.isVideo) {
                mediaItem.durationMs?.let { durMs ->
                    InfoRow(label = "Duration", value = formatDuration(durMs))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp
        )
    }
}

private fun getFormatName(mimeType: String, displayName: String): String? {
    if (mimeType.isBlank()) return null
    val format = mimeType.substringAfter("/").lowercase()
    return when (format) {
        "jpeg", "jpg" -> "JPEG"
        "png" -> "PNG"
        "webp" -> "WEBP"
        "gif" -> "GIF"
        "heic", "heif" -> "HEIF"
        "mp4" -> "MP4"
        "quicktime" -> "MOV"
        "webm" -> "WEBM"
        "3gpp" -> "3GP"
        "x-matroska" -> "MKV"
        else -> format.uppercase()
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}

private fun formatDate(timestampMs: Long): String {
    val localDate = Instant.ofEpochMilli(timestampMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()).format(localDate)
}

private const val MIN_SCALE = 1f
private const val DOUBLE_TAP_SCALE = 2.5f
private const val MAX_SCALE = 5f
private const val SCALE_EPSILON = 0.01f

private fun Offset.coerceWithinBounds(scale: Float, viewport: IntSize): Offset {
    if (scale <= MIN_SCALE || viewport.width == 0 || viewport.height == 0) return Offset.Zero

    val overflowX = viewport.width * (scale - MIN_SCALE) / 2f
    val overflowY = viewport.height * (scale - MIN_SCALE) / 2f

    return Offset(
        x = x.coerceIn(-overflowX, overflowX),
        y = y.coerceIn(-overflowY, overflowY)
    )
}
