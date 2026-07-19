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
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import coil3.size.Precision
import coil3.size.Scale
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.MediaItem
import com.emon.proxagallery.data.MediaDetails
import com.emon.proxagallery.ui.theme.DeleteRed
import com.emon.proxagallery.ui.theme.FavoriteRed
import com.emon.proxagallery.ui.theme.RestoreBlue
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────────────────────────────────────
// Theme-aware "glass" colors for the floating control bars.
//
// The photo canvas itself stays pure black in every theme (it is an image
// viewing surface, like every gallery/cinema app), but the floating top bar,
// the bottom action bar and the page indicator pills are translucent "glass"
// overlays — so they pick up the active surface color and read correctly on
// both dark and light themes.
// ──────────────────────────────────────────────────────────────────────────────
private val ViewerCanvasColor = Color.Black

@Composable
private fun viewerGlassColor(): Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)

@Composable
private fun viewerGlassBorder(): Color = MaterialTheme.colorScheme.outline

// ──────────────────────────────────────────────────────────────────────────────
// Screen entry point
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun PhotoViewerScreen(
    itemIds: List<Long>,
    initialItemId: Long,
    onBackClick: () -> Unit,
    resolveViewerItem: suspend (Long) -> ViewerItem?,
    mode: ViewerMode = ViewerMode.GALLERY,
    modifier: Modifier = Modifier,
    favoriteKeys: Set<String> = emptySet(),
    albums: List<Album> = emptyList(),
    onToggleFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    onDeletePhoto: (id: Long, uri: Uri) -> Unit = { _, _ -> },
    onRestore: (ViewerItem) -> Unit = {},
    onDeleteForever: (ViewerItem) -> Unit = {},
    getMediaItem: suspend (Long) -> MediaItem? = { null },
    getMediaDetails: suspend (Long) -> MediaDetails? = { null }
) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var isUiVisible by remember { mutableStateOf(false) }

    // Bottom-sheet state
    var showMoreSheet by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }

    // Trash-only: confirm before permanently deleting the current item.
    var showDeleteForeverDialog by remember { mutableStateOf(false) }

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
            .background(ViewerCanvasColor)
    ) {
        if (itemIds.isNotEmpty()) {
            val initialPage = itemIds.indexOf(initialItemId).coerceAtLeast(0)
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { itemIds.size }
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
                key = { index -> itemIds[index] }
            ) { page ->
                val itemId = itemIds[page]
                var viewerItem by remember(itemId) { mutableStateOf<ViewerItem?>(null) }
                LaunchedEffect(itemId) {
                    viewerItem = resolveViewerItem(itemId)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val current = viewerItem
                    if (current != null) {
                        val context = LocalContext.current
                        val request = remember(current.imageData) {
                            ImageRequest.Builder(context)
                                .data(current.imageData)
                                // Reuse the thumbnail that the grid already put in
                                // memory cache — shows instantly while full-res loads.
                                .placeholderMemoryCacheKey(current.imageData.toString())
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = current.displayName.takeIf { it.isNotBlank() },
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
                                .pointerInput(current.id) {
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

                        // Video playback is only available in gallery mode: trashed files are
                        // no longer addressable via MediaStore, so there is no Uri to hand off.
                        if (mode == ViewerMode.GALLERY && current.isVideo && current.videoUri != null) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(current.videoUri, "video/*")
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

            val currentItemId = itemIds.getOrNull(pagerState.currentPage)

            // Gallery only: the MediaItem backing the action bar (share/edit/favorite/delete)
            // and the Details sheet. Not needed in trash mode, which acts on ViewerItem directly.
            var displayedMedia by remember { mutableStateOf<MediaItem?>(null) }
            LaunchedEffect(currentItemId) {
                if (mode == ViewerMode.GALLERY && currentItemId != null) {
                    val loaded = getMediaItem(currentItemId)
                    if (loaded != null) displayedMedia = loaded
                } else if (mode != ViewerMode.GALLERY) {
                    // Avoid carrying a stale MediaItem across modes.
                    displayedMedia = null
                }
            }

            // Both modes: the ViewerItem for the current page (top-bar filename and, in
            // trash mode, the Restore / Delete Forever targets).
            var currentViewerItem by remember { mutableStateOf<ViewerItem?>(null) }
            LaunchedEffect(currentItemId) {
                currentViewerItem = if (currentItemId != null) resolveViewerItem(currentItemId) else null
            }

            // ── Adjacent image preloading (gallery only) ────────────────────
            LaunchedEffect(pagerState.currentPage) {
                if (mode != ViewerMode.GALLERY) return@LaunchedEffect
                val page = pagerState.currentPage
                val start = maxOf(0, page - PRELOAD_RANGE)
                val end = minOf(itemIds.size - 1, page + PRELOAD_RANGE)
                val displayMetrics = context.resources.displayMetrics
                val displayW = displayMetrics.widthPixels
                val displayH = displayMetrics.heightPixels
                for (i in start..end) {
                    if (i == page) continue
                    val id = itemIds[i]
                    val item = getMediaItem(id)
                    if (item != null) {
                        imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .data(item.uri)
                                // Bound the preload to screen size — never decode
                                // a 3084×4096 bitmap for an adjacent page that may
                                // never be visited. Saves ~180 MB per swipe.
                                .size(displayW, displayH)
                                .precision(Precision.INEXACT)
                                .scale(Scale.FIT)
                                .memoryCacheKey(item.uri.toString())
                                .diskCacheKey(item.uri.toString())
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
                        .background(viewerGlassColor())
                        .border(1.dp, viewerGlassBorder(), RoundedCornerShape(28.dp))
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Crossfade(
                        targetState = currentViewerItem?.displayName
                            ?: mediaItemToDisplay?.displayName
                            ?: "",
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                        label = "filename",
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) { name ->
                        Column {
                            Text(
                                text = name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (itemIds.size > 1) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${itemIds.size}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (mode == ViewerMode.GALLERY) {
                        IconButton(onClick = { showDetailsSheet = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Details",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
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
                when (mode) {
                    ViewerMode.GALLERY -> {
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

                    ViewerMode.TRASH -> {
                        TrashActionBar(
                            onRestore = { currentViewerItem?.let(onRestore) },
                            onDeleteForever = { showDeleteForeverDialog = true },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                }
            }

            // ── More bottom sheet (gallery only) ────────────────────────────
            if (mode == ViewerMode.GALLERY && showMoreSheet && mediaItemToDisplay != null) {
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

            // ── Details bottom sheet (gallery only) ─────────────────────────
            if (mode == ViewerMode.GALLERY && showDetailsSheet && mediaItemToDisplay != null) {
                var mediaDetails by remember { mutableStateOf<MediaDetails?>(null) }
                LaunchedEffect(mediaItemToDisplay.id) {
                    mediaDetails = getMediaDetails(mediaItemToDisplay.id)
                }
                mediaDetails?.let { details ->
                    val favKey = if (mediaItemToDisplay.isVideo)
                        "v:${mediaItemToDisplay.id}" else "i:${mediaItemToDisplay.id}"
                    MediaDetailsSheet(
                        details = details,
                        isFavorite = favKey in favoriteKeys,
                        onToggleFavorite = {
                            onToggleFavorite(mediaItemToDisplay.id, mediaItemToDisplay.isVideo)
                        },
                        onDismiss = { showDetailsSheet = false }
                    )
                }
            }

            // ── Delete-forever confirmation (trash only) ────────────────────
            if (showDeleteForeverDialog && currentViewerItem != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteForeverDialog = false },
                    title = { Text("Delete forever?") },
                    text = {
                        Text(
                            "This item will be permanently removed from Recently Deleted.\n" +
                                "This action cannot be undone."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val target = currentViewerItem
                                showDeleteForeverDialog = false
                                if (target != null) onDeleteForever(target)
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete Forever")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteForeverDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// TrashActionBar — 2-button bottom action strip (Restore + Delete Forever)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrashActionBar(
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(viewerGlassColor())
            .border(1.dp, viewerGlassBorder(), RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionBarButton(
                icon = Icons.Rounded.Restore,
                label = "Restore",
                tint = RestoreBlue,
                onClick = onRestore
            )
            ActionBarButton(
                icon = Icons.Rounded.DeleteForever,
                label = "Delete Forever",
                tint = DeleteRed,
                onClick = onDeleteForever
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ViewerActionBar — 5-button bottom action strip (gallery mode)
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
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(viewerGlassColor())
            .border(1.dp, viewerGlassBorder(), RoundedCornerShape(32.dp))
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
                tint = onSurface,
                onClick = onShare
            )
            ActionBarButton(
                icon = Icons.Rounded.Edit,
                label = "Edit",
                tint = onSurface,
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
                    tint = if (fav) FavoriteRed else onSurface,
                    onClick = onFavorite
                )
            }
            ActionBarButton(
                icon = Icons.Rounded.Delete,
                label = "Delete",
                tint = DeleteRed,
                onClick = onDelete
            )
            ActionBarButton(
                icon = Icons.Rounded.MoreVert,
                label = "More",
                tint = onSurface,
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
                .background(MaterialTheme.colorScheme.outlineVariant)
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "More options",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (action.subtitle != null) {
                Text(
                    text = action.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
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
