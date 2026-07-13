package com.emon.proxagallery.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.emon.proxagallery.data.MediaItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PhotoViewerScreen(
    photos: List<MediaItem>,
    initialPhotoId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    favoriteKeys: Set<String> = emptySet(),
    onToggleFavorite: (Long, Boolean) -> Unit = { _, _ -> }
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
            .background(Color.Black)
    ) {
        if (photos.isNotEmpty()) {
            val initialPage = photos.indexOfFirst { photo ->
                photo.id == initialPhotoId
            }.coerceAtLeast(0)
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { photos.size }
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
                key = { index -> photos[index].id }
            ) { page ->
                val mediaItem = photos[page]

                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = mediaItem.uri,
                        contentDescription = mediaItem.displayName.takeIf { it.isNotBlank() },
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
                            .pointerInput(mediaItem.id) {
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

                    if (mediaItem.isVideo) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(mediaItem.uri, "video/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                activity?.startActivity(intent)
                            },
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play video",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isUiVisible) {
                Button(
                    onClick = {
                        insetsController?.show(WindowInsetsCompat.Type.systemBars())
                        onBackClick()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Text("Back")
                }

                IconButton(
                    onClick = {
                        val current = photos[pagerState.currentPage]
                        onToggleFavorite(current.id, current.isVideo)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    val current = photos[pagerState.currentPage]
                    val favKey = if (current.isVideo) "v:${current.id}" else "i:${current.id}"
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = if (favKey in favoriteKeys) {
                            Color(0xFFFF1744)
                        } else {
                            Color.White
                        }
                    )
                }

                PhotoInfoCard(
                    mediaItem = photos[pagerState.currentPage],
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun PhotoInfoCard(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color.Black.copy(alpha = 0.6f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = mediaItem.displayName,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        mediaItem.width?.let { width ->
            mediaItem.height?.let { height ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${width} × ${height}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        mediaItem.fileSize?.let { size ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatFileSize(size),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }

        mediaItem.dateTakenMs?.let { dateMs ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatDate(dateMs),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
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
