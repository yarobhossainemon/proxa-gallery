package com.emon.proxagallery.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew

import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import coil3.imageLoader
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.MediaItem
import com.emon.proxagallery.data.MediaDetails
import com.emon.proxagallery.util.formatDateMs
import com.emon.proxagallery.util.formatDateSec
import com.emon.proxagallery.util.formatDurationMs
import com.emon.proxagallery.util.formatFileSizeBytes
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────────────────────────────────────
// Screen entry point
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun PhotoViewerScreen(
    photoIds: List<Long>,
    initialPhotoId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    favoriteKeys: Set<String> = emptySet(),
    albums: List<Album> = emptyList(),
    onToggleFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    onDeletePhoto: (id: Long, uri: Uri) -> Unit = { _, _ -> },
    getMediaItem: suspend (Long) -> MediaItem?,
    getMediaDetails: suspend (Long) -> MediaDetails? = { null }
) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var isUiVisible by remember { mutableStateOf(false) }

    // Bottom-sheet state
    var showMoreSheet by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }

    val activity = LocalActivity.current
    val context = LocalContext.current
    val imageLoader = remember { context.imageLoader }
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

            var displayedMedia by remember { mutableStateOf<MediaItem?>(null) }
            LaunchedEffect(currentPhotoId) {
                if (currentPhotoId != null) {
                    val loaded = getMediaItem(currentPhotoId)
                    if (loaded != null) displayedMedia = loaded
                }
            }

            // ── Adjacent image preloading ────────────────────────────────────
            LaunchedEffect(pagerState.currentPage) {
                val page = pagerState.currentPage
                val start = maxOf(0, page - PRELOAD_RANGE)
                val end = minOf(photoIds.size - 1, page + PRELOAD_RANGE)
                for (i in start..end) {
                    if (i == page) continue
                    val id = photoIds[i]
                    val item = getMediaItem(id)
                    if (item != null) {
                        imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .data(item.uri)
                                .build()
                        )
                    }
                }
            }

            val mediaItemToDisplay = displayedMedia

            // ── Top bar ──────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isUiVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)),
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

                    Crossfade(
                        targetState = mediaItemToDisplay?.displayName ?: "",
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                        label = "filename"
                    ) { name ->
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                    }

                    IconButton(onClick = { showDetailsSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Details",
                            tint = Color.White
                        )
                    }
                }
            }

            // ── Bottom action bar ────────────────────────────────────────────
            val currentItem by rememberUpdatedState(mediaItemToDisplay)

            AnimatedVisibility(
                visible = isUiVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val favKey = currentItem?.let {
                    if (it.isVideo) "v:${it.id}" else "i:${it.id}"
                } ?: ""
                val isFavorite = favKey.isNotEmpty() && favKey in favoriteKeys

                ViewerActionBar(
                    isFavorite = isFavorite,
                    onShare = {
                        currentItem?.let { item ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = item.mimeType.ifBlank { "*/*" }
                                putExtra(Intent.EXTRA_STREAM, item.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                        }
                    },
                    onEdit = {
                        currentItem?.let { item ->
                            val editIntent = Intent(Intent.ACTION_EDIT).apply {
                                setDataAndType(item.uri, item.mimeType.ifBlank { "*/*" })
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(editIntent, "Edit"))
                        }
                    },
                    onFavorite = {
                        currentItem?.let { onToggleFavorite(it.id, it.isVideo) }
                    },
                    onDelete = {
                        currentItem?.let { onDeletePhoto(it.id, it.uri) }
                    },
                    onMore = { showMoreSheet = true },
                    modifier = Modifier.navigationBarsPadding()
                )
            }

            // ── More bottom sheet ────────────────────────────────────────────
            if (showMoreSheet && mediaItemToDisplay != null) {
                val moreCtx = LocalContext.current
                val moreActions = remember(mediaItemToDisplay) {
                    buildMoreActions(
                        mediaItem = mediaItemToDisplay,
                        onOpenWith = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(mediaItemToDisplay.uri,
                                    mediaItemToDisplay.mimeType.ifBlank { "*/*" })
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            moreCtx.startActivity(Intent.createChooser(intent, "Open with"))
                        },
                        onSetWallpaper = {
                            val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                                setDataAndType(mediaItemToDisplay.uri, "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                putExtra("mimeType", "image/*")
                            }
                            moreCtx.startActivity(Intent.createChooser(intent, "Set as wallpaper"))
                        },
                        onDetails = {
                            showMoreSheet = false
                            showDetailsSheet = true
                        }
                    )
                }
                MoreBottomSheet(
                    actions = moreActions,
                    onDismiss = { showMoreSheet = false }
                )
            }

            // ── Details bottom sheet ─────────────────────────────────────────
            if (showDetailsSheet && mediaItemToDisplay != null) {
                var mediaDetails by remember { mutableStateOf<MediaDetails?>(null) }
                LaunchedEffect(mediaItemToDisplay.id) {
                    mediaDetails = getMediaDetails(mediaItemToDisplay.id)
                }
                mediaDetails?.let { details ->
                    DetailsBottomSheet(
                        mediaDetails = details,
                        onDismiss = { showDetailsSheet = false }
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ViewerActionBar — 5-button bottom action strip
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ViewerActionBar(
    isFavorite: Boolean,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xD0161A22))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionBarButton(
                icon = Icons.Rounded.Share,
                label = "Share",
                tint = Color.White,
                onClick = onShare
            )
            ActionBarButton(
                icon = Icons.Rounded.Edit,
                label = "Edit",
                tint = Color.White,
                onClick = onEdit
            )
            AnimatedContent(
                targetState = isFavorite,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
                },
                label = "favorite"
            ) { fav ->
                ActionBarButton(
                    icon = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = if (fav) "Liked" else "Like",
                    tint = if (fav) Color(0xFFFF1744) else Color.White,
                    onClick = onFavorite
                )
            }
            ActionBarButton(
                icon = Icons.Rounded.Delete,
                label = "Delete",
                tint = Color(0xFFFF5252),
                onClick = onDelete
            )
            ActionBarButton(
                icon = Icons.Rounded.MoreVert,
                label = "More",
                tint = Color.White,
                onClick = onMore
            )
        }
    }
}

@Composable
private fun ActionBarButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// More bottom sheet
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreBottomSheet(
    actions: List<ViewerMoreAction>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161A22),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "More options",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            actions.forEach { action ->
                MoreActionRow(
                    action = action,
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                            action.onClick()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MoreActionRow(
    action: ViewerMoreAction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(action.title) { detectTapGestures { onClick() } }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (action.subtitle != null) {
                Text(
                    text = action.subtitle,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Details bottom sheet
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsBottomSheet(
    mediaDetails: MediaDetails,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161A22),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Details",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Spacer(Modifier.height(8.dp))

            DetailRow(label = "Name", value = mediaDetails.displayName)

            DetailRow(
                label = "Type",
                value = if (mediaDetails.isVideo) "Video" else "Photo"
            )

            if (mediaDetails.mimeType.isNotBlank()) {
                DetailRow(label = "MIME type", value = mediaDetails.mimeType)
            }

            if (mediaDetails.width != null && mediaDetails.height != null) {
                DetailRow(
                    label = "Resolution",
                    value = "${mediaDetails.width} × ${mediaDetails.height}"
                )
            }

            mediaDetails.fileSize?.let {
                DetailRow(label = "File size", value = formatFileSizeBytes(it))
            }

            mediaDetails.dateTakenMs?.let {
                DetailRow(label = "Date taken", value = formatDateMs(it))
            }

            mediaDetails.dateAddedMs?.let {
                DetailRow(label = "Date added", value = formatDateMs(it))
            }

            mediaDetails.dateModifiedSec?.let {
                DetailRow(label = "Date modified", value = formatDateSec(it))
            }

            mediaDetails.bucketDisplayName?.let {
                DetailRow(label = "Folder", value = it)
            }

            mediaDetails.relativePath?.let {
                DetailRow(label = "Relative path", value = it)
            }

            if (mediaDetails.isVideo) {
                mediaDetails.durationMs?.let {
                    DetailRow(label = "Duration", value = formatDurationMs(it))
                }
            }

            val hasCameraInfo = mediaDetails.cameraMake != null ||
                mediaDetails.cameraModel != null ||
                mediaDetails.lensModel != null ||
                mediaDetails.aperture != null ||
                mediaDetails.shutterSpeed != null ||
                mediaDetails.iso != null ||
                mediaDetails.focalLength != null ||
                mediaDetails.flash != null ||
                mediaDetails.whiteBalance != null

            if (hasCameraInfo) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Text(
                    text = "Camera info",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            mediaDetails.cameraMake?.let {
                DetailRow(label = "Camera make", value = it)
            }

            mediaDetails.cameraModel?.let {
                DetailRow(label = "Camera model", value = it)
            }

            mediaDetails.lensModel?.let {
                DetailRow(label = "Lens", value = it)
            }

            mediaDetails.aperture?.let {
                DetailRow(label = "Aperture", value = it)
            }

            mediaDetails.shutterSpeed?.let {
                DetailRow(label = "Shutter speed", value = it)
            }

            mediaDetails.iso?.let {
                DetailRow(label = "ISO", value = it)
            }

            mediaDetails.focalLength?.let {
                DetailRow(label = "Focal length", value = it)
            }

            mediaDetails.flash?.let {
                DetailRow(label = "Flash", value = it)
            }

            mediaDetails.whiteBalance?.let {
                DetailRow(label = "White balance", value = it)
            }

            if (mediaDetails.latitude != null && mediaDetails.longitude != null) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Text(
                    text = "Location",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                DetailRow(
                    label = "GPS",
                    value = "${"%.6f".format(mediaDetails.latitude)}, ${"%.6f".format(mediaDetails.longitude)}"
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helper: build More actions list
// ──────────────────────────────────────────────────────────────────────────────

private fun buildMoreActions(
    mediaItem: MediaItem,
    onOpenWith: () -> Unit,
    onSetWallpaper: () -> Unit,
    onDetails: () -> Unit
): List<ViewerMoreAction> = buildList {
    add(ViewerMoreAction(
        icon = Icons.AutoMirrored.Rounded.OpenInNew,
        title = "Open with",
        onClick = onOpenWith
    ))
    if (!mediaItem.isVideo) {
        add(ViewerMoreAction(
            icon = Icons.Rounded.Wallpaper,
            title = "Set as wallpaper",
            onClick = onSetWallpaper
        ))
    }
    add(ViewerMoreAction(
        icon = Icons.Rounded.Info,
        title = "Details",
        subtitle = "View file information",
        onClick = onDetails
    ))
}

// ──────────────────────────────────────────────────────────────────────────────
// Constants and utilities
// ──────────────────────────────────────────────────────────────────────────────

private const val PRELOAD_RANGE = 2
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
