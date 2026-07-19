package com.emon.proxagallery.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.emon.proxagallery.util.PermissionHelper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import com.emon.proxagallery.ui.theme.DeleteRed
import com.emon.proxagallery.ui.theme.extendedColors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Scale
import com.emon.proxagallery.BuildConfig
import com.emon.proxagallery.data.AccentColor
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.AlbumCustomization
import com.emon.proxagallery.data.AlbumSortOption
import com.emon.proxagallery.data.AlbumStats
import com.emon.proxagallery.data.MediaItem
import com.emon.proxagallery.data.PhotoSortOption
import com.emon.proxagallery.data.ThemeMode
import com.emon.proxagallery.util.formatFileSizeBytes
import com.emon.proxagallery.ui.glass.GlassCard
import com.emon.proxagallery.ui.glass.GlassChip
import com.emon.proxagallery.ui.glass.GlassSurface
import com.emon.proxagallery.ui.glass.GlassBadge
import com.emon.proxagallery.ui.glass.GlassCountBadge
import com.emon.proxagallery.ui.glass.GlassNavigationBar
import com.emon.proxagallery.ui.glass.GlassSearchBar
import com.emon.proxagallery.ui.glass.cardShape
import com.emon.proxagallery.ui.glass.smallCardShape
import com.emon.proxagallery.ui.glass.GlassTokens
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged

// ──────────────────────────────────────────────────────────────────────────
// File-level animation spec singletons.
// Defined once at class-load time so they are never re-allocated during
// recomposition. Each grid cell composable (PhotoGridItem, SelectionCheckmark)
// runs on every visible item, so even small per-recomposition allocations
// add up quickly when scrolling through a large library.
// ──────────────────────────────────────────────────────────────────────────
private val tween170 = tween<Float>(durationMillis = 170)
private val tween200 = tween<Float>(durationMillis = 200)
private val springPressScale = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)
private val springCheckmark = spring<Float>(
    dampingRatio = 0.6f,
    stiffness = 700f
)

@Composable
fun HomeScreen(
    uiState: GalleryUiState,
    allPhotos: LazyPagingItems<MediaItem>,
    albumPhotos: LazyPagingItems<MediaItem>,
    searchPhotos: LazyPagingItems<MediaItem>,
    onPhotosAccessGranted: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    onAlbumClick: (Long?) -> Unit = {},
    onTabClick: (Int) -> Unit = {},
    onRecentlyDeletedClick: () -> Unit = {},
    onAlbumSortChange: (AlbumSortOption) -> Unit = {},
    onPhotoSortChange: (PhotoSortOption) -> Unit = {},
    onEnterSelectionMode: (Long) -> Unit = {},
    onTogglePhotoSelection: (Long) -> Unit = {},
    onSelectPhoto: (Long) -> Unit = {},
    onExitSelectionMode: () -> Unit = {},
    onSelectAllVisible: () -> Unit = {},
    onShareSelected: () -> Unit = {},
    onFavoriteSelected: () -> Unit = {},
    onRequestBulkDelete: () -> Unit = {},
    onConfirmBulkDelete: () -> Unit = {},
    onCancelBulkDelete: () -> Unit = {},
    showDeleteConfirmation: Boolean = false,
    currentTheme: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    onThemeChange: (com.emon.proxagallery.data.ThemeMode) -> Unit = {},
    currentAccent: AccentColor = AccentColor.BLUE,
    onAccentChange: (AccentColor) -> Unit = {},
    onEditAlbum: (Album) -> Unit = {},
    onCancelEditAlbum: () -> Unit = {},
    onSaveAlbumRename: (String?) -> Unit = {},
    onSaveAlbumCover: (String) -> Unit = {},
    onResetAlbumCustomization: () -> Unit = {},
    onToggleAlbumPinned: () -> Unit = {},
    onToggleAlbumHidden: () -> Unit = {},
    onRequestAlbumCover: () -> Unit = {},
    onHiddenAlbumsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {}
) {
    // When a selection is active (photos selected), the first Back press must
    // clear the selection and exit selection mode WITHOUT navigating. Only once
    // nothing is selected does Back navigate. Registering this handler last makes
    // it take priority over the album-navigation handler below.
    BackHandler(enabled = uiState.selectedPhotoIds.isNotEmpty()) {
        onExitSelectionMode()
    }
    // Album navigation only when not in selection mode, so it cannot steal the
    // Back press while photos are selected (which would leak the selection into
    // the previous screen).
    BackHandler(enabled = uiState.selectedAlbumId != null && !uiState.isSelectionMode) {
        onAlbumClick(null)
    }

    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
        allPhotos = allPhotos,
        albumPhotos = albumPhotos,
        searchPhotos = searchPhotos,
        onPhotosAccessGranted = onPhotosAccessGranted,
        onSearchQueryChange = onSearchQueryChange,
        onPhotoClick = onPhotoClick,
        onToggleFavorite = onToggleFavorite,
        onAlbumClick = onAlbumClick,
        onTabClick = onTabClick,
        onRecentlyDeletedClick = onRecentlyDeletedClick,
        onAlbumSortChange = onAlbumSortChange,
        onPhotoSortChange = onPhotoSortChange,
        onEnterSelectionMode = onEnterSelectionMode,
        onTogglePhotoSelection = onTogglePhotoSelection,
        onSelectPhoto = onSelectPhoto,
        onExitSelectionMode = onExitSelectionMode,
        onSelectAllVisible = onSelectAllVisible,
        onShareSelected = onShareSelected,
        onFavoriteSelected = onFavoriteSelected,
        onRequestBulkDelete = onRequestBulkDelete,
        onConfirmBulkDelete = onConfirmBulkDelete,
        onCancelBulkDelete = onCancelBulkDelete,
        showDeleteConfirmation = showDeleteConfirmation,
        currentTheme = currentTheme,
        onThemeChange = onThemeChange,
        currentAccent = currentAccent,
        onAccentChange = onAccentChange,
        onEditAlbum = onEditAlbum,
        onCancelEditAlbum = onCancelEditAlbum,
        onSaveAlbumRename = onSaveAlbumRename,
        onSaveAlbumCover = onSaveAlbumCover,
        onResetAlbumCustomization = onResetAlbumCustomization,
        onToggleAlbumPinned = onToggleAlbumPinned,
        onToggleAlbumHidden = onToggleAlbumHidden,
        onRequestAlbumCover = onRequestAlbumCover,
        onHiddenAlbumsClick = onHiddenAlbumsClick,
        onFavoritesClick = onFavoritesClick
    )
}

@Composable
private fun HomeScreenContent(
    uiState: GalleryUiState,
    allPhotos: LazyPagingItems<MediaItem>,
    albumPhotos: LazyPagingItems<MediaItem>,
    searchPhotos: LazyPagingItems<MediaItem>,
    onPhotosAccessGranted: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    onAlbumClick: (Long?) -> Unit = {},
    onTabClick: (Int) -> Unit = {},
    onRecentlyDeletedClick: () -> Unit = {},
    onAlbumSortChange: (AlbumSortOption) -> Unit = {},
    onPhotoSortChange: (PhotoSortOption) -> Unit = {},
    onEnterSelectionMode: (Long) -> Unit = {},
    onTogglePhotoSelection: (Long) -> Unit = {},
    onSelectPhoto: (Long) -> Unit = {},
    onExitSelectionMode: () -> Unit = {},
    onSelectAllVisible: () -> Unit = {},
    onShareSelected: () -> Unit = {},
    onFavoriteSelected: () -> Unit = {},
    onRequestBulkDelete: () -> Unit = {},
    onConfirmBulkDelete: () -> Unit = {},
    onCancelBulkDelete: () -> Unit = {},
    showDeleteConfirmation: Boolean = false,
    currentTheme: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    onThemeChange: (com.emon.proxagallery.data.ThemeMode) -> Unit = {},
    currentAccent: AccentColor = AccentColor.BLUE,
    onAccentChange: (AccentColor) -> Unit = {},
    onEditAlbum: (Album) -> Unit = {},
    onCancelEditAlbum: () -> Unit = {},
    onSaveAlbumRename: (String?) -> Unit = {},
    onSaveAlbumCover: (String) -> Unit = {},
    onResetAlbumCustomization: () -> Unit = {},
    onToggleAlbumPinned: () -> Unit = {},
    onToggleAlbumHidden: () -> Unit = {},
    onRequestAlbumCover: () -> Unit = {},
    onHiddenAlbumsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission state as mutableState so recomposition triggers on change.
    var hasPermission by remember {
        mutableStateOf(PermissionHelper.hasPermission(context))
    }

    // Re-check permission when the app resumes (e.g. user granted it from Settings).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = PermissionHelper.hasPermission(context)
                if (nowGranted && !hasPermission) {
                    hasPermission = true
                    onPhotosAccessGranted()
                } else {
                    hasPermission = nowGranted
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.all { it.value }) {
            hasPermission = true
            onPhotosAccessGranted()
        }
    }

    if (!hasPermission) {
        PermissionScreen(
            onRequestPermission = {
                permissionLauncher.launch(PermissionHelper.requiredPermissions())
            }
        )
    } else {
        GalleryContainer(
            uiState = uiState,
            allPhotos = allPhotos,
            albumPhotos = albumPhotos,
            searchPhotos = searchPhotos,
            onSearchQueryChange = onSearchQueryChange,
            onPhotoClick = onPhotoClick,
            onToggleFavorite = onToggleFavorite,
            onAlbumClick = onAlbumClick,
            onAlbumClear = { onAlbumClick(null) },
            onTabClick = onTabClick,
            onAlbumSortChange = onAlbumSortChange,
            onPhotoSortChange = onPhotoSortChange,
            onRecentlyDeletedClick = onRecentlyDeletedClick,
            onEnterSelectionMode = onEnterSelectionMode,
            onTogglePhotoSelection = onTogglePhotoSelection,
            onSelectPhoto = onSelectPhoto,
            onExitSelectionMode = onExitSelectionMode,
            onSelectAllVisible = onSelectAllVisible,
            onShareSelected = onShareSelected,
            onFavoriteSelected = onFavoriteSelected,
            onRequestBulkDelete = onRequestBulkDelete,
            onConfirmBulkDelete = onConfirmBulkDelete,
            onCancelBulkDelete = onCancelBulkDelete,
            showDeleteConfirmation = showDeleteConfirmation,
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            currentAccent = currentAccent,
            onAccentChange = onAccentChange,
            onEditAlbum = onEditAlbum,
            onCancelEditAlbum = onCancelEditAlbum,
            onSaveAlbumRename = onSaveAlbumRename,
            onSaveAlbumCover = onSaveAlbumCover,
            onResetAlbumCustomization = onResetAlbumCustomization,
            onToggleAlbumPinned = onToggleAlbumPinned,
            onToggleAlbumHidden = onToggleAlbumHidden,
            onRequestAlbumCover = onRequestAlbumCover,
            onHiddenAlbumsClick = onHiddenAlbumsClick,
            onFavoritesClick = onFavoritesClick
        )
    }
}

@Composable
private fun PermissionScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Proxa Gallery",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Your memories, beautifully organized.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Allow Photos Access",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun GalleryContainer(
    uiState: GalleryUiState,
    allPhotos: LazyPagingItems<MediaItem>,
    albumPhotos: LazyPagingItems<MediaItem>,
    searchPhotos: LazyPagingItems<MediaItem>,
    onSearchQueryChange: (String) -> Unit,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onAlbumClick: (Long?) -> Unit,
    onAlbumClear: () -> Unit,
    onTabClick: (Int) -> Unit,
    onAlbumSortChange: (AlbumSortOption) -> Unit,
    onPhotoSortChange: (PhotoSortOption) -> Unit,
    onRecentlyDeletedClick: () -> Unit,
    onEnterSelectionMode: (Long) -> Unit = {},
    onTogglePhotoSelection: (Long) -> Unit = {},
    onSelectPhoto: (Long) -> Unit = {},
    onExitSelectionMode: () -> Unit = {},
    onSelectAllVisible: () -> Unit = {},
    onShareSelected: () -> Unit = {},
    onFavoriteSelected: () -> Unit = {},
    onRequestBulkDelete: () -> Unit = {},
    onConfirmBulkDelete: () -> Unit = {},
    onCancelBulkDelete: () -> Unit = {},
    showDeleteConfirmation: Boolean = false,
    currentTheme: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    onThemeChange: (com.emon.proxagallery.data.ThemeMode) -> Unit = {},
    currentAccent: AccentColor = AccentColor.BLUE,
    onAccentChange: (AccentColor) -> Unit = {},
    onEditAlbum: (Album) -> Unit = {},
    onCancelEditAlbum: () -> Unit = {},
    onSaveAlbumRename: (String?) -> Unit = {},
    onSaveAlbumCover: (String) -> Unit = {},
    onResetAlbumCustomization: () -> Unit = {},
    onToggleAlbumPinned: () -> Unit = {},
    onToggleAlbumHidden: () -> Unit = {},
    onRequestAlbumCover: () -> Unit = {},
    onHiddenAlbumsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {}
) {
    val homeGridState = rememberLazyStaggeredGridState()

    // The Photos-tab header is fully static — it intentionally reads nothing
    // from homeGridState. (See the Photos Tab section below for why: the grid
    // is a sibling below the header, not under it, so a scroll-driven scrim
    // would only flicker through the translucent glass instead of separating
    // the header from scrolling content.) homeGridState is still created here
    // because it is shared with PagedPhotoGridTab on the Photos tab.

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val hasSelection = uiState.selectedPhotoIds.isNotEmpty()
        val allSelectedAreFav = remember(uiState.selectedPhotoIds, uiState.favoriteKeys) {
            allSelectedAreFavorite(uiState)
        }

        Scaffold(
            // Suppress the default status-bar inset so innerPadding.top == 0.
            // Each individual header already applies .statusBarsPadding() itself,
            // so letting Scaffold also add the status-bar height would double it
            // and create the large empty gap below the glass header.
            contentWindowInsets = WindowInsets(0),
            // Selection action bar (Samsung style) shown only while a
            // selection is active. It cross-fades in/out; the normal tab
            // navigation floats over the content and is NOT in this slot.
            bottomBar = {
                AnimatedVisibility(
                    visible = hasSelection,
                    enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                        slideInVertically(animationSpec = tween(durationMillis = 280)) { it },
                    exit = fadeOut(animationSpec = tween(durationMillis = 180)) +
                        slideOutVertically(animationSpec = tween(durationMillis = 240)) { it }
                ) {
                    SelectionBottomBar(
                        selectedCount = uiState.selectedPhotoIds.size,
                        allSelectedAreFavorite = allSelectedAreFav,
                        onFavorite = onFavoriteSelected,
                        onShare = onShareSelected,
                        onDelete = onRequestBulkDelete
                    )
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.selectedAlbumId != null) {
                // Inside an album view
                val selectedAlbum = remember(uiState.albums, uiState.selectedAlbumId) {
                    uiState.albums.find { it.id == uiState.selectedAlbumId }
                }

                // One top bar slot: normal album header in normal mode, selection
                // bar in selection mode — same position, no gap.
                TopBarSlot(
                    isSelectionMode = uiState.isSelectionMode,
                    normalHeader = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = remember(onAlbumClick) { { onAlbumClick(null) } }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedAlbum?.displayNameToDisplay ?: "Album",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${selectedAlbum?.itemCount ?: 0} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            PhotoSortMenuButton(
                                selectedOption = uiState.photoSortOption,
                                onOptionSelected = onPhotoSortChange
                            )
                        }
                    },
                    selectionTopBar = {
                        SelectionTopBar(
                            selectedCount = uiState.selectedPhotoIds.size,
                            onClose = onExitSelectionMode
                        )
                    }
                )

                PagedPhotoGridTab(
                    pagingItems = albumPhotos,
                    uiState = uiState,
                    onPhotoClick = onPhotoClick,
                    onToggleFavorite = onToggleFavorite,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedPhotoIds = uiState.selectedPhotoIds,
                    onEnterSelectionMode = onEnterSelectionMode,
                    onTogglePhotoSelection = onTogglePhotoSelection,
                    onSelectPhoto = onSelectPhoto
                )
            } else {
                // Root tabs view
                when (uiState.selectedTab) {
                    0 -> {
                        // ── Photos Tab ──────────────────────────────────────────
                        // Merged: all photos + inline search. When the search query
                        // is non-blank the grid shows filtered search results;
                        // otherwise it shows the full photo library.
                        //
                        // ── Static header ──────────────────────────────────────
                        // The glass header is intentionally NOT given any
                        // scroll-derived modifier. The photo grid is a sibling
                        // BELOW this header in the Column (it does not scroll
                        // underneath it), so any scroll-driven scrim here has no
                        // legitimate purpose. Worse, because the glass fill is
                        // translucent (glassSurface alpha ≈ 0.60–0.85 by design),
                        // a black scrim placed behind it inside the same Box
                        // would bleed through the glass and visibly darken the
                        // header the moment scrolling starts — the exact
                        // "blink on scroll" symptom. Keeping the header fully
                        // static is what makes the blink impossible.
                        TopBarSlot(
                            isSelectionMode = uiState.isSelectionMode,
                            normalHeader = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                ) {
                                    ProxaGalleryHeader()

                                    // Inline search bar + quick chips
                                    GlassSearchBar(
                                        query = uiState.searchQuery,
                                        onQueryChange = onSearchQueryChange,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        trailingContent = {
                                            // Sort affordance now lives inside
                                            // the search bar (Samsung Gallery /
                                            // Google Photos style) so the
                                            // header stays clean. Reuses the
                                            // existing, state-backed
                                            // PhotoSortMenuButton verbatim —
                                            // no ViewModel logic changed.
                                            Spacer(Modifier.width(10.dp))

                                            Box(
                                                modifier = Modifier
                                                    .size(1.dp, 20.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                                    )
                                            )

                                            Spacer(Modifier.width(10.dp))

                                            PhotoSortMenuButton(
                                                selectedOption = uiState.photoSortOption,
                                                onOptionSelected = onPhotoSortChange,
                                                icon = Icons.Rounded.Tune,
                                                contentDescription = "Sort photos"
                                            )
                                        }
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    QuickSearchChips(
                                        onChipClick = onSearchQueryChange
                                    )
                                }
                            },
                            selectionTopBar = {
                                SelectionTopBar(
                                    selectedCount = uiState.selectedPhotoIds.size,
                                    onClose = onExitSelectionMode
                                )
                            }
                        ) // end TopBarSlot

                        // Show search results when query is active, otherwise all photos.
                        PagedPhotoGridTab(
                            pagingItems = if (uiState.searchQuery.isNotBlank()) searchPhotos else allPhotos,
                            uiState = uiState,
                            gridState = homeGridState,
                            onPhotoClick = onPhotoClick,
                            onToggleFavorite = onToggleFavorite,
                            isSelectionMode = uiState.isSelectionMode,
                            selectedPhotoIds = uiState.selectedPhotoIds,
                            onEnterSelectionMode = onEnterSelectionMode,
                            onTogglePhotoSelection = onTogglePhotoSelection,
                            onSelectPhoto = onSelectPhoto
                        )
                    }
                    1 -> {
                        // ── Albums Tab ──────────────────────────────────────────
                        // Unchanged logic — moved from old index 2.
                        var sortExpanded by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "My Albums",
                                    fontSize = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                Box {
                                    IconButton(onClick = { sortExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Rounded.SwapVert,
                                            contentDescription = "Sort albums",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    androidx.compose.material3.DropdownMenu(
                                        expanded = sortExpanded,
                                        onDismissRequest = { sortExpanded = false },
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        AlbumSortOption.entries.forEach { option ->
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = option.label,
                                                        color = if (option == uiState.albumSortOption)
                                                            MaterialTheme.colorScheme.primary
                                                        else
                                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                                        fontWeight = if (option == uiState.albumSortOption)
                                                            FontWeight.Bold
                                                        else
                                                            FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    onAlbumSortChange(option)
                                                    sortExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Hidden Albums entry — visibility is driven by the
                        // persistent hasHiddenAlbums flag, which is derived
                        // directly from the AlbumCustomization database
                        // (Room). It does NOT depend on rawAlbums, MediaStore
                        // loading, or any cached list, so the entry correctly
                        // survives an app restart even before MediaStore loads.
                        if (uiState.hasHiddenAlbums) {
                            SettingsRow(
                                icon = Icons.Rounded.VisibilityOff,
                                title = "Hidden Albums",
                                subtitle = "View your hidden albums",
                                onClick = onHiddenAlbumsClick,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }

                        AlbumGridTab(
                            albums = uiState.albums,
                            onAlbumClick = onAlbumClick,
                            onEditAlbum = onEditAlbum
                        )
                    }
                    2 -> {
                        // ── AI Tab ─────────────────────────────────────────────
                        AiTab()
                    }
                    3 -> {
                        // ── Library Tab ────────────────────────────────────────
                        val stats = uiState.libraryStats
                        LibraryTab(
                            hiddenAlbumsCount = uiState.hiddenAlbums.size,
                            albumsCount = uiState.albums.size,
                            favoritesCount = uiState.favoriteItems.size,
                            recentlyDeletedCount = uiState.recentlyDeletedCount,
                            photosCount = stats?.photoCount ?: 0,
                            videosCount = stats?.videoCount ?: 0,
                            storageBytes = stats?.totalSizeBytes ?: 0L,
                            onFavoritesClick = onFavoritesClick,
                            onHiddenAlbumsClick = onHiddenAlbumsClick,
                            onRecentlyDeletedClick = onRecentlyDeletedClick
                        )
                    }
                    4 -> {
                        // ── Settings Tab ───────────────────────────────────────
                        SettingsTab(
                            modifier = Modifier.statusBarsPadding(),
                            currentTheme = currentTheme,
                            onThemeChange = onThemeChange,
                            currentAccent = currentAccent,
                            onAccentChange = onAccentChange
                        )
                    }
                }
            }
        }
        }

        // Floating bottom navigation — overlaid directly on the content
        // rather than placed in Scaffold.bottomBar, so it floats above
        // the photo grid without reserving a full-width dark strip.
        if (!hasSelection && uiState.selectedAlbumId == null) {
            GlassNavigationBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }

        // Bulk-delete confirmation. Visibility is driven by ViewModel state
        // (uiState.showDeleteConfirmation); the dialog only renders and reports
        // the user's choice back to the ViewModel.
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = onCancelBulkDelete,
                title = {
                    Text(
                        text = "Delete ${uiState.selectedPhotoIds.size} photo" +
                            if (uiState.selectedPhotoIds.size == 1) "?" else "s?",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "This will move the selected photos to Recently Deleted.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = onConfirmBulkDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancelBulkDelete) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Edit Album bottom sheet (Samsung One UI–style). Visibility is driven
        // by ViewModel state (uiState.showEditAlbumDialog). All edits are virtual
        // and stored only in Room — no filesystem or MediaStore change.
        if (uiState.showEditAlbumDialog && uiState.editingAlbum != null) {
            EditAlbumBottomSheet(
                album = uiState.editingAlbum,
                customization = uiState.albumCustomizations[uiState.editingAlbum.id],
                stats = uiState.editingAlbumStats,
                onRename = onSaveAlbumRename,
                onChangeCover = { onRequestAlbumCover() },
                onTogglePinned = { onToggleAlbumPinned() },
                onToggleHidden = { onToggleAlbumHidden() },
                onResetCustomization = onResetAlbumCustomization,
                onDismiss = onCancelEditAlbum
            )
        }
    }
}

/**
 * Samsung One UI–style Edit Album bottom sheet.
 *
 * Surfaces every virtual album customization in one place. All edits persist
 * ONLY to Room ([AlbumCustomization]) — MediaStore, file paths, and folder
 * names are never touched. Sections:
 *
 * 1. Header — current cover preview + inline rename field (Save/Reset).
 * 2. Actions — Change Cover, Pin/Unpin, Hide/Unhide, Reset Customization.
 * 3. Album Information — photo count, video count, storage size, original
 *    MediaStore folder name.
 *
 * Pin/Hide/Reset apply instantly via the ViewModel; only Rename uses an inline
 * editable field with an explicit Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditAlbumBottomSheet(
    album: Album,
    customization: AlbumCustomization?,
    stats: AlbumStats?,
    onRename: (String?) -> Unit,
    onChangeCover: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleHidden: () -> Unit,
    onResetCustomization: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(album.id) { mutableStateOf(album.displayNameToDisplay) }
    val hasCustomName = customization?.customName != null
    val isPinned = customization?.isPinned ?: false
    val isHidden = customization?.isHidden ?: false
    val hasAnyCustomization = customization != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp)
                .padding(bottom = 16.dp)
        ) {
            // --- Header: cover preview + title ---
            AlbumSheetHeader(album = album)

            Spacer(Modifier.height(12.dp))

            // --- Rename section ---
            RenameSection(
                name = name,
                onNameChange = { name = it },
                placeholder = album.displayName,
                hasCustomName = hasCustomName,
                onSave = { onRename(name) },
                onReset = { onRename(null) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            // --- Action rows ---
            AlbumSheetActionRow(
                icon = Icons.Rounded.Photo,
                label = "Change Album Cover",
                onClick = onChangeCover
            )
            AlbumSheetActionRow(
                icon = Icons.Rounded.PushPin,
                label = if (isPinned) "Unpin Album" else "Pin Album",
                trailing = if (isPinned) "Pinned" else null,
                onClick = onTogglePinned
            )
            AlbumSheetActionRow(
                icon = Icons.Rounded.VisibilityOff,
                label = if (isHidden) "Unhide Album" else "Hide Album",
                trailing = if (isHidden) "Hidden" else null,
                onClick = onToggleHidden
            )
            AlbumSheetActionRow(
                icon = Icons.Rounded.Refresh,
                label = "Reset Album Customization",
                labelColor = MaterialTheme.colorScheme.error,
                enabled = hasAnyCustomization,
                onClick = onResetCustomization
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            // --- Album Information ---
            AlbumInformationSection(album = album, stats = stats)
        }
    }
}

/** Centered pill drag handle for the bottom sheet, matching One UI styling. */
@Composable
internal fun BottomSheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .width(36.dp)
            .height(4.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(2.dp)
            )
    )
}

/** Sheet header: rounded cover thumbnail + album title + item count. */
@Composable
internal fun AlbumSheetHeader(album: Album) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val coverRequest = remember(album.coverUriToDisplay) {
            ImageRequest.Builder(context)
                .data(album.coverUriToDisplay)
                .size(160)
                .crossfade(false)
                .build()
        }
        AsyncImage(
            model = coverRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.displayNameToDisplay,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.itemCount} ${if (album.itemCount == 1) "item" else "items"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Inline rename field with Save and (conditional) Reset buttons. */
@Composable
internal fun RenameSection(
    name: String,
    onNameChange: (String) -> Unit,
    placeholder: String,
    hasCustomName: Boolean,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Rename Album",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (name.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    innerTextField()
                }
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasCustomName) {
                TextButton(onClick = onReset) {
                    Text("Reset Name", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(4.dp))
            }
            TextButton(onClick = onSave) {
                Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * A single tappable action row in the sheet. Samsung One UI–style: leading
 * icon, label, optional trailing status text. Tints to [labelColor] when given
 * (used for the destructive Reset action) and greys out when disabled.
 */
@Composable
internal fun AlbumSheetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    val rowColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) labelColor else rowColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) labelColor else rowColor,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Album Information section: photo count, video count, total storage size, and
 * the original MediaStore folder name (never the custom name).
 */
@Composable
internal fun AlbumInformationSection(album: Album, stats: AlbumStats?) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Album Information",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        InfoRow(
            label = "Original folder",
            value = album.displayName.ifBlank { "Unknown" }
        )
        if (stats != null) {
            InfoRow(label = "Photos", value = stats.photoCount.toString())
            InfoRow(label = "Videos", value = stats.videoCount.toString())
            InfoRow(
                label = "Storage size",
                value = formatFileSizeBytes(stats.totalSizeBytes)
            )
        } else {
            InfoRow(label = "Photos", value = "…")
            InfoRow(label = "Videos", value = "…")
            InfoRow(label = "Storage size", value = "…")
        }
    }
}

/** Label/value row used inside [AlbumInformationSection]. */
@Composable
internal fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PagedPhotoGridTab(
    pagingItems: LazyPagingItems<MediaItem>,
    uiState: GalleryUiState,
    gridState: LazyStaggeredGridState? = null,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    isSelectionMode: Boolean = false,
    selectedPhotoIds: Set<Long> = emptySet(),
    onEnterSelectionMode: (Long) -> Unit = {},
    onTogglePhotoSelection: (Long) -> Unit = {},
    onSelectPhoto: (Long) -> Unit = {}
) {
    val actualGridState = gridState ?: rememberLazyStaggeredGridState()
    val density = LocalDensity.current
    
    val currentOnPhotoClick by rememberUpdatedState(onPhotoClick)
    val currentOnToggleFavorite by rememberUpdatedState(onToggleFavorite)
    val currentOnEnterSelectionMode by rememberUpdatedState(onEnterSelectionMode)
    val currentOnTogglePhotoSelection by rememberUpdatedState(onTogglePhotoSelection)
    val currentOnSelectPhoto by rememberUpdatedState(onSelectPhoto)
    
    val handlePhotoClick = remember { { item: MediaItem -> currentOnPhotoClick(item) } }
    val handleToggleFavorite = remember { { id: Long, isFav: Boolean -> currentOnToggleFavorite(id, isFav) } }
    val handleEnterSelectionMode = remember { { id: Long -> currentOnEnterSelectionMode(id) } }
    val handleTogglePhotoSelection = remember { { id: Long -> currentOnTogglePhotoSelection(id) } }
    val handleSelectPhoto = remember { { id: Long -> currentOnSelectPhoto(id) } }

    // Compute the grid cell size once per composition, not per-item.
    // 3-column grid → cell = screenWidth / 3, clamped to sensible bounds.
    val configuration = LocalConfiguration.current
    val thumbnailSizePx = with(density) {
        (configuration.screenWidthDp.dp / 3).roundToPx().coerceIn(120, 600)
    }
    val dragModifier = Modifier.dragSelection(
        gridState = actualGridState,
        density = density,
        resolveId = { index -> pagingItems[index]?.id },
        onSelectPhoto = handleSelectPhoto,
        onUnselectPhoto = handleTogglePhotoSelection
    )

    val refreshState = pagingItems.loadState.refresh
    val appendState = pagingItems.loadState.append

    when {
        refreshState is LoadState.Loading && pagingItems.itemCount == 0 -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GeminiLoadingRing()
            }
        }
        refreshState is LoadState.Error && pagingItems.itemCount == 0 -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = refreshState.error.localizedMessage ?: "Unable to load images.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { pagingItems.retry() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        refreshState is LoadState.NotLoading && pagingItems.itemCount == 0 -> {
            GeminiEmptyState(
                icon = Icons.Rounded.Image,
                title = "No memories here yet",
                subtitle = "Your photos and videos will show up here."
            )
        }
        else -> {
            LazyVerticalStaggeredGrid(
                state = actualGridState,
                columns = StaggeredGridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().then(dragModifier),
                contentPadding = PaddingValues(
                    start = 4.dp,
                    end = 4.dp,
                    top = 4.dp,
                    bottom = if (uiState.selectedAlbumId == null) 96.dp else 16.dp
                )
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = { index -> pagingItems[index]?.id ?: index.toLong() },
                    contentType = { index -> 
                        val item = pagingItems[index]
                        if (item?.isVideo == true) "video" else "image"
                    }
                ) { index ->
                    val item = pagingItems[index] ?: return@items
                    val favKey = if (item.isVideo) "v:${item.id}" else "i:${item.id}"
                    val isFav = favKey in uiState.favoriteKeys
                    PhotoGridItem(
                        item = item,
                        isFavorite = isFav,
                        thumbnailSizePx = thumbnailSizePx,
                        onPhotoClick = handlePhotoClick,
                        onToggleFavorite = handleToggleFavorite,
                        isSelectionMode = isSelectionMode,
                        isSelected = item.id in selectedPhotoIds,
                        onEnterSelectionMode = handleEnterSelectionMode,
                        onTogglePhotoSelection = handleTogglePhotoSelection
                    )
                }

                if (appendState is LoadState.Loading) {
                    item(
                        span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine,
                        contentType = { "loading_indicator" }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                             GeminiLoadingRing(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                        }
                    }
                } else if (appendState is LoadState.Error) {
                    item(
                        span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine,
                        contentType = { "error_indicator" }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = appendState.error.localizedMessage ?: "Failed to load more items.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { pagingItems.retry() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoGridTab(
    uiState: GalleryUiState,
    photos: List<MediaItem>,
    gridState: LazyStaggeredGridState? = null,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onLoadMore: () -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedPhotoIds: Set<Long> = emptySet(),
    onEnterSelectionMode: (Long) -> Unit = {},
    onTogglePhotoSelection: (Long) -> Unit = {},
    onSelectPhoto: (Long) -> Unit = {}
) {
    val actualGridState = gridState ?: rememberLazyStaggeredGridState()
    val density = LocalDensity.current
    // Compute cell size once per composition for the 3-column grid.
    val configuration = LocalConfiguration.current
    val thumbnailSizePx = with(density) {
        (configuration.screenWidthDp.dp / 3).roundToPx().coerceIn(120, 600)
    }
    val dragModifier = Modifier.dragSelection(
        gridState = actualGridState,
        density = density,
        resolveId = { index -> photos.getOrNull(index)?.id },
        onSelectPhoto = onSelectPhoto,
        onUnselectPhoto = onTogglePhotoSelection
    )

    LaunchedEffect(actualGridState) {
        snapshotFlow {
            val layoutInfo = actualGridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 6
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd) onLoadMore()
        }
    }

    when {
        uiState.isLoading && photos.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GeminiLoadingRing()
            }
        }
        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        photos.isEmpty() -> {
            GeminiEmptyState(
                icon = Icons.Rounded.Favorite,
                title = "No favorites yet",
                subtitle = "Tap the heart on any photo or video to add it here."
            )
        }
        else -> {
            LazyVerticalStaggeredGrid(
                state = actualGridState,
                columns = StaggeredGridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().then(dragModifier),
                contentPadding = PaddingValues(
                    start = 4.dp,
                    end = 4.dp,
                    top = 4.dp,
                    bottom = if (uiState.selectedAlbumId == null) 96.dp else 16.dp
                )
            ) {
                items(
                    items = photos,
                    key = { item -> item.id },
                    contentType = { item -> if (item.isVideo) "video" else "image" }
                ) { item ->
                    val favKey = if (item.isVideo) "v:${item.id}" else "i:${item.id}"
                    val isFav = favKey in uiState.favoriteKeys
                    PhotoGridItem(
                        item = item,
                        isFavorite = isFav,
                        thumbnailSizePx = thumbnailSizePx,
                        onPhotoClick = onPhotoClick,
                        onToggleFavorite = onToggleFavorite,
                        isSelectionMode = isSelectionMode,
                        isSelected = item.id in selectedPhotoIds,
                        onEnterSelectionMode = onEnterSelectionMode,
                        onTogglePhotoSelection = onTogglePhotoSelection
                    )
                }
            }
        }
    }
}

@Composable
internal fun PhotoGridItem(
    item: MediaItem,
    isFavorite: Boolean,
    thumbnailSizePx: Int,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onEnterSelectionMode: (Long) -> Unit = {},
    onTogglePhotoSelection: (Long) -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }
    // Cache the shape so it isn't reallocated on every recomposition.
    val cardShape = remember { smallCardShape() }
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected && isSelectionMode -> 0.96f
            isPressed -> 0.94f
            else -> 1f
        },
        animationSpec = springPressScale,
        label = "photo_press_scale"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected && isSelectionMode) 1f else 0f,
        animationSpec = tween200,
        label = "border_alpha"
    )
    val primaryColor = MaterialTheme.colorScheme.primary

    GlassCard(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(
                if (item.width != null && item.height != null && item.width > 0 && item.height > 0)
                    item.width.toFloat() / item.height.toFloat()
                else 1f
            )
            .border(
                width = 1.5.dp,
                color = primaryColor.copy(alpha = borderAlpha),
                shape = cardShape
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(item.id, isSelectionMode) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        if (isSelectionMode) onTogglePhotoSelection(item.id)
                        else onPhotoClick(item)
                    }
                )
            },
        shape = cardShape,
        pressed = null,
        elevation = 2.dp
    ) {
        val context = LocalContext.current
        // thumbnailSizePx is the actual rendered cell width in device pixels,
        // hoisted by the parent grid to avoid computing it per-item per-frame.
        // Precision.INEXACT allows Coil to reuse a nearby cached size rather
        // than re-decoding. Scale.FILL signals that ContentScale.Crop is used,
        // so Coil subsamples to the correct dimension without loading too much.
        val cacheKey = item.uri.toString()
        val imageRequest = remember(cacheKey, thumbnailSizePx) {
            ImageRequest.Builder(context)
                .data(item.uri)
                .size(thumbnailSizePx)
                .precision(Precision.INEXACT)
                .scale(Scale.FILL)
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .crossfade(false)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        val overlayAlpha by animateFloatAsState(
            targetValue = if (isSelected && isSelectionMode) 1f else 0f,
            animationSpec = tween170,
            label = "selection_overlay_alpha"
        )
        // Use graphicsLayer alpha instead of Color.copy() to avoid per-frame
        // Color object allocations while the fade animation is running.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = overlayAlpha * 0.2f }
                .background(primaryColor)
        )

        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play video",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (isSelectionMode) {
            SelectionCheckmark(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
    }
}

/**
 * Bottom action bar shown while at least one photo is selected (Samsung
 * Gallery One UI 7 style).
 *
 * Replaces the old in-top-bar bulk actions. Three equally-spaced actions with
 * the icon ABOVE the label:
 *   Favorite   Share   Delete
 *
 * Rendered on an elevated, rounded-top [Surface] that respects the system
 * navigation bar via [WindowInsets.navigationBars]. It is wrapped by the
 * caller in [AnimatedVisibility] (fade + slide) so it slides in/out as
 * selection starts/ends. Large 48dp+ touch targets, subtly shadowed, and
 * theme-aware (supports dark/light via [MaterialTheme]).
 *
 * No business logic here — every action forwards to the ViewModel through the
 * supplied callbacks, exactly as the previous top-bar buttons did.
 */
@Composable
internal fun SelectionBottomBar(
    selectedCount: Int,
    allSelectedAreFavorite: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        strong = true,
        padding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .size(width = 36.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionAction(
                    icon = if (allSelectedAreFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = "Favorite",
                    onClick = onFavorite,
                    iconTint = MaterialTheme.colorScheme.secondary
                )
                SelectionAction(
                    icon = Icons.Rounded.Share,
                    label = "Share",
                    onClick = onShare,
                    iconTint = MaterialTheme.colorScheme.primary
                )
                SelectionAction(
                    icon = Icons.Rounded.Delete,
                    label = "Delete",
                    onClick = onDelete,
                    iconTint = DeleteRed
                )
            }
        }
    }
}

/**
 * A single Samsung-style action: a centered icon above its label, with a
 * minimum 56dp square touch target (>= 48dp) so it is comfortable to tap.
 */
@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        modifier = Modifier
            .sizeIn(minWidth = 72.dp, minHeight = 64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = iconTint,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Animated selection badge shown in selection mode (Samsung Gallery style):
 * a 24dp blue (primary) filled circle in the top-end corner containing a white
 * check. It pops in (scale 0.6 -> 1.0) when selected and pops out when
 * deselected; the circle is fully transparent when the item is not selected so
 * only selected items show the indicator.
 */
@Composable
private fun SelectionCheckmark(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        label = "selection_checkmark_bg"
    )
    // Pop scale: 0.6 -> 1.0 when selected (spring), reverse when deselected.
    val popScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = springCheckmark,
        label = "selection_checkmark_pop"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween170,
        label = "selection_checkmark_icon_alpha"
    )

    // alpha + scale are both applied on the outer Box's graphicsLayer.
    // The Icon does NOT need a second graphicsLayer — removing it saves one
    // RenderNode layer and avoids a redundant per-frame RenderNode update.
    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = popScale
                scaleY = popScale
                alpha = iconAlpha
            }
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Selected",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Top bar shown when selection mode is active.
 *
 * Samsung Gallery One UI style: a close (back) button on the left and the
 * selection count ("N Selected") centered. The bulk actions (Favorite / Share /
 * Delete) live in the [SelectionBottomBar] so they are reachable by thumb
 * while the grid stays visible. This bar never performs business logic — it
 * only forwards the close intent via [onClose].
 */

/**
 * Keeps a normal (non-selection) top bar and the [SelectionTopBar] in the SAME
 * layout slot, so only ever one bar is visible and the photo grid below never
 * shifts. Both are mounted inside one [Box] and cross-faded via two
 * [AnimatedVisibility]s (no [togetherWith], which is unavailable in Compose
 * 1.10.4): entering selection fades the folder title out while the selection
 * toolbar fades in immediately, in the exact same position — no gap, no jump.
 */
@Composable
internal fun TopBarSlot(
    isSelectionMode: Boolean,
    modifier: Modifier = Modifier,
    normalHeader: @Composable () -> Unit,
    selectionTopBar: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().then(modifier)) {
        AnimatedVisibility(
            visible = !isSelectionMode,
            enter = fadeIn(animationSpec = tween(durationMillis = 250)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            normalHeader()
        }
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn(animationSpec = tween(durationMillis = 250)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            selectionTopBar()
        }
    }
}

@Composable
internal fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "underline_anim")
    val offsetFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "underline_offset"
    )
    val underlineBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.extendedColors.gradientStart,
            MaterialTheme.extendedColors.gradientEnd,
            MaterialTheme.extendedColors.gradientStart
        ),
        start = Offset(0f, 0f),
        end = Offset(800f * offsetFraction, 0f)
    )

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth(),
        strong = true,
        shape = RoundedCornerShape(0.dp),
        padding = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Exit selection",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = "✓ $selectedCount Selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(underlineBrush)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * True when every currently selected photo is already a favorite, based on the
 * favorite keys held in [uiState]. Drives the Favorite/Unfavorite button label.
 */
internal fun allSelectedAreFavorite(uiState: GalleryUiState): Boolean {
    val keys = uiState.favoriteKeys
    return uiState.selectedPhotoIds.all { id ->
        keys.contains("i:$id") || keys.contains("v:$id")
    }
}

/**
 * Reusable sort menu button.
 *
 * Renders an icon button that opens a Material 3 [DropdownMenu] listing every
 * sort option, highlighting the currently selected one. Extracted so the same
 * UI (and styling) is shared by the Albums tab, the in-album photo grid, and
 * later by Gallery / Search / Favorites.
 *
 * The type parameter is bounded to [Enum] (so [entries] is available) and the
 * selected option is passed in by reference — each call site compares enum
 * instances directly. Both [AlbumSortOption] and [PhotoSortOption] expose a
 * `label`, which is read reflectively here to avoid coupling this composable
 * to either enum.
 *
 * @param options all available sort options, in display order.
 * @param selectedOption the option currently applied; highlighted in the menu.
 * @param onOptionSelected invoked with the chosen option. The caller is
 *   responsible for forwarding it to the ViewModel — this composable never
 *   touches the ViewModel directly.
 * @param icon the leading icon for the button.
 * @param contentDescription accessibility description for the icon button.
 */
@Composable
private fun <T> SortMenuButton(
    options: Collection<T>,
    selectedOption: T,
    labelOf: (T) -> String,
    onOptionSelected: (T) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.extendedColors.glassSurfaceStrong,
            shape = RoundedCornerShape(16.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                DropdownMenuItem(
                    text = {
                        Text(
                            text = labelOf(option),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.White.copy(alpha = 0.85f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Compact branded header for the Photos screen.
 *
 * Replaces the old "Good Morning / name / date" greeting with a single premium
 * branding row (One UI / Pixel inspired): a small gallery badge + the
 * "Proxa Gallery" wordmark on the left. Search bar and quick chips stay below
 * — untouched — so only the greeting collapses to a slim brand bar.
 *
 * The branded element (badge + wordmark) is grouped as one visual unit. The
 * trailing [trailingContent] slot defaults to empty, in which case nothing is
 * rendered on the right and no trailing spacing is reserved — leaving a clean
 * brand-only bar. When supplied (e.g. for future cloud-sync status or premium
 * badge) it is right-aligned with a flexible spacer.
 *
 * @param trailingContent optional right-aligned affordance. Empty by default so
 *   the header renders as a clean brand-only row; the Photos tab no longer
 *   needs a sort button here (sorting moved into the search bar).
 */
@Composable
fun ProxaGalleryHeader(
    trailingContent: @Composable () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gradientStart = MaterialTheme.extendedColors.gradientStart
    val gradientEnd = MaterialTheme.extendedColors.gradientEnd

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Branded element: badge + wordmark read as a single unit.
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Premium gallery badge — tinted glass tile with the spark icon.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                gradientStart.copy(alpha = 0.22f),
                                gradientEnd.copy(alpha = 0.22f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = gradientStart.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Proxa Gallery",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        // Only reserve trailing space + render the slot when the caller
        // actually provides trailing content, so the default brand-only header
        // leaves no empty gap on the right.
        // Compose has no zero-content detection, so the caller controls this by
        // passing (or omitting) trailingContent; an empty default produces an
        // empty Row subtree here — visually nothing.
        Spacer(Modifier.weight(1f))
        trailingContent()
    }
}

/**
 * Photo-sort variant of [SortMenuButton].
 *
 * Pre-wires the photo-specific defaults — the [PhotoSortOption] entries, their
 * `label`, the swap-vert icon, and the "Sort photos" accessibility label — so
 * every photo grid (Gallery, Search, in-album, Favorites) renders an identical
 * sort control from a single call site. Behavior is unchanged; this only
 * removes the repeated parameter block that would otherwise be copy-pasted.
 *
 * @param icon the leading icon for the button. Defaults to [Icons.Rounded.SwapVert]
 *   (the classic sort affordance) but can be overridden — e.g. the Photos tab
 *   search bar uses [Icons.Rounded.Tune] to read as a filter, matching the
 *   Samsung Gallery / Google Photos pattern, while the in-album grid keeps the
 *   default swap-vert.
 */
@Composable
internal fun PhotoSortMenuButton(
    selectedOption: PhotoSortOption,
    onOptionSelected: (PhotoSortOption) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.SwapVert,
    contentDescription: String = "Sort photos"
) {
    SortMenuButton(
        options = PhotoSortOption.entries,
        selectedOption = selectedOption,
        labelOf = { it.label },
        onOptionSelected = onOptionSelected,
        icon = icon,
        contentDescription = contentDescription
    )
}

@Composable
private fun AlbumGridTab(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    onEditAlbum: (Album) -> Unit
) {
    if (albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No albums found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        return
    }

    val currentOnAlbumClick by rememberUpdatedState(onAlbumClick)
    val currentOnEditAlbum by rememberUpdatedState(onEditAlbum)
    val handleAlbumClick = remember { { id: Long -> currentOnAlbumClick(id) } }
    val handleEditAlbum = remember { { album: Album -> currentOnEditAlbum(album) } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 96.dp
        )
    ) {
        items(
            items = albums,
            key = { album -> album.id },
            contentType = { "album" }
        ) { album ->
            AlbumCard(
                album = album,
                onAlbumClick = handleAlbumClick,
                onEditAlbum = handleEditAlbum
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    onAlbumClick: (Long) -> Unit,
    onEditAlbum: (Album) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .pointerInput(album.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onAlbumClick(album.id) },
                    onLongPress = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEditAlbum(album)
                    }
                )
            },
        shape = cardShape(),
        pressed = isPressed,
        elevation = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val coverRequest = remember(album.coverUriToDisplay) {
                ImageRequest.Builder(context)
                    .data(album.coverUriToDisplay)
                    .size(384)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = coverRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark overlay to keep premium dark gallery aesthetic
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )

            // Pinned badge — only shown for pinned albums
            if (album.isPinned) {
                GlassBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    size = 28.dp,
                    gradient = true
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Album caption bar — title + item count
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(bottomStart = GlassTokens.CardRadius, bottomEnd = GlassTokens.CardRadius),
                strong = true,
                padding = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = album.displayNameToDisplay,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${album.itemCount} ${if (album.itemCount == 1) "item" else "items"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}







@Composable
fun QuickSearchChips(
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = listOf(
        "People", "Flowers", "Cats", "Cars", "Beach", 
        "Food", "Videos", "Documents", "Screenshots", "Memes", "Favorites"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips) { chip ->
            GlassChip(
                label = chip,
                onClick = { onChipClick(chip.lowercase()) }
            )
        }
    }
}

@Composable
private fun SettingsTab(
    modifier: Modifier = Modifier,
    currentTheme: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    onThemeChange: (ThemeMode) -> Unit = {},
    currentAccent: AccentColor = AccentColor.BLUE,
    onAccentChange: (AccentColor) -> Unit = {}
) {
    var showThemeSheet by remember { mutableStateOf(false) }
    var showAccentSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Customize your Proxa Gallery experience",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(28.dp))

        // ───────────── Appearance ─────────────
        LibrarySectionLabel("Appearance")
        Spacer(Modifier.height(8.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsRow(
                    icon = Icons.Rounded.Palette,
                    title = "Theme",
                    trailingText = currentTheme.displayName(),
                    onClick = { showThemeSheet = true }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Palette,
                    title = "Accent Color",
                    trailingText = currentAccent.displayName(),
                    onClick = { showAccentSheet = true }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ───────────── About ─────────────
        LibrarySectionLabel("About")
        Spacer(Modifier.height(8.dp))
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsRow(
                    icon = Icons.Rounded.Person,
                    title = "Developer",
                    subtitle = "Emon"
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = "Version",
                    trailingText = "v${BuildConfig.VERSION_NAME}"
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Star,
                    title = "Rate Proxa Gallery",
                    onClick = {
                        // TODO: open Play Store listing when available
                    }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.BugReport,
                    title = "Send Feedback",
                    onClick = {
                        // TODO: wire up feedback flow
                    }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.PrivacyTip,
                    title = "Privacy Policy",
                    onClick = {
                        // TODO: open privacy policy page
                    }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Description,
                    title = "Open Source Licenses",
                    onClick = {
                        // TODO: show OSS licenses
                    }
                )
            }
        }
    }

    if (showThemeSheet) {
        ThemePickerSheet(
            currentTheme = currentTheme,
            onDismiss = { showThemeSheet = false },
            onThemeSelected = { mode ->
                showThemeSheet = false
                onThemeChange(mode)
            }
        )
    }

    if (showAccentSheet) {
        AccentPickerSheet(
            currentAccent = currentAccent,
            onDismiss = { showAccentSheet = false },
            onAccentSelected = { accent ->
                showAccentSheet = false
                onAccentChange(accent)
            }
        )
    }
}

/**
 * Shared divider between settings rows inside a [GlassCard]. Kept as a helper
 * so the look stays consistent with the rest of the Settings screen.
 */
@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

/**
 * Single reusable settings row. Used by every entry on the Settings screen so
 * all rows share the same icon box, typography, trailing value and chevron.
 *
 * - [onClick] == null  → non-interactive row (no ripple, no chevron).
 * - [subtitle]         → optional secondary line under the title.
 * - [trailingText]     → optional value shown before the chevron.
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
    iconContentDescription: String? = title
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerSheet(
    currentTheme: ThemeMode,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit
) {
    val options = ThemeMode.entries
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        options.forEach { mode ->
            val selected = mode == currentTheme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { onThemeSelected(mode) }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = { onThemeSelected(mode) }
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = mode.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccentPickerSheet(
    currentAccent: AccentColor,
    onDismiss: () -> Unit,
    onAccentSelected: (AccentColor) -> Unit
) {
    val options = AccentColor.entries
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            text = "Accent Color",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        options.forEach { accent ->
            val selected = accent == currentAccent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { onAccentSelected(accent) }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = { onAccentSelected(accent) }
                )
                Spacer(Modifier.width(12.dp))
                // Color preview swatch
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accent.darkSeed.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = accent.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun GeminiLoadingRing(
    modifier: Modifier = Modifier,
    strokeWidth: androidx.compose.ui.unit.Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val gradientColors = listOf(
        MaterialTheme.extendedColors.gradientStart,
        MaterialTheme.extendedColors.gradientEnd,
        MaterialTheme.extendedColors.gradientStart
    )

    Canvas(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            brush = Brush.sweepGradient(gradientColors),
            style = stroke
        )
    }
}

@Composable
fun GeminiEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val gradientStart = MaterialTheme.extendedColors.gradientStart
        val gradientEnd = MaterialTheme.extendedColors.gradientEnd
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer(alpha = 0.99f)
                .drawWithCache {
                    val brush = Brush.linearGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush, blendMode = BlendMode.SrcAtop)
                    }
                }
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// AI Tab — data-driven premium dashboard
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A single AI feature entry rendered in the [AiTab] feature list.
 *
 * Fully data-driven: toggling [enabled] to `true` and wiring an [onClick]
 * is all that's needed to activate a feature later — no UI redesign.
 */
private data class AiFeature(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val enabled: Boolean = false,
    val onClick: (() -> Unit)? = null
)

/**
 * Premium AI dashboard tab. Designed to feel polished, not empty.
 *
 * Layout:
 * 1. Hero header — gradient AutoAwesome icon + "Search anything" subtitle.
 * 2. Premium search box with example query chips (non-functional, purely
 *    illustrative — communicates future AI capability).
 * 3. "Coming Soon" section label.
 * 4. Data-driven feature list rendered from [aiFeatures].
 */
@Composable
private fun AiTab() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Premium gradient icon
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.extendedColors.gradientStart,
                                MaterialTheme.extendedColors.gradientEnd
                            )
                        )
                    )
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = MaterialTheme.extendedColors.gradientStart.copy(alpha = 0.4f),
                        clip = false
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Search anything",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))

            // Premium search box (illustrative, non-functional)
            GlassSearchBar(
                query = "",
                onQueryChange = {},
                modifier = Modifier.padding(horizontal = 0.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Example query chips (non-functional, purely illustrative)
            AiExampleChips()
        }

        Spacer(Modifier.height(8.dp))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(16.dp))

        // "Coming Soon" label
        Text(
            text = "Coming Soon",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Data-driven feature list
        aiFeatures.forEach { feature ->
            AiFeatureCard(feature = feature)
        }

        // Bottom spacer for the floating nav bar
        Spacer(Modifier.height(100.dp))
    }
}

/** Non-functional example query chips for the AI hero. */
@Composable
private fun AiExampleChips() {
    val examples = listOf(
        "Dog on the beach",
        "Invoice from March",
        "Screenshot with John",
        "Birthday party",
        "Blue car",
        "Sunset"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        examples.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                row.forEach { text ->
                    GlassChip(
                        label = text,
                        onClick = null
                    )
                }
            }
        }
    }
}

/** Renders a single AI feature card in the dashboard list. */
@Composable
private fun AiFeatureCard(feature: AiFeature) {
    val enabledAlpha = if (feature.enabled) 1f else 0.5f

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .graphicsLayer { alpha = enabledAlpha }
            .then(
                if (feature.enabled && feature.onClick != null) {
                    Modifier.clickable(onClick = feature.onClick)
                } else {
                    Modifier
                }
            ),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.extendedColors.gradientStart,
                                MaterialTheme.extendedColors.gradientEnd
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = enabledAlpha)
                )
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = enabledAlpha)
                )
            }

            if (!feature.enabled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Coming soon",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Enabled",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Central data-driven feature list for the AI tab.
 *
 * Adding a new AI feature = appending one [AiFeature] to this list.
 * Enabling = `enabled = true` + wiring an `onClick`.
 * No redesign needed.
 */
private val aiFeatures = listOf(
    AiFeature(
        title = "Smart Albums",
        subtitle = "Auto-grouped by content",
        icon = Icons.Rounded.CollectionsBookmark
    ),
    AiFeature(
        title = "Duplicate Finder",
        subtitle = "Spot and remove copies",
        icon = Icons.Rounded.CheckCircle
    ),
    AiFeature(
        title = "AI Sorting",
        subtitle = "Intelligent ordering",
        icon = Icons.Rounded.SwapVert
    ),
    AiFeature(
        title = "Screenshot Understanding",
        subtitle = "Read text in screenshots",
        icon = Icons.Rounded.SelectAll
    ),
    AiFeature(
        title = "Face Clustering",
        subtitle = "Group photos by people",
        icon = Icons.Rounded.Favorite
    ),
    AiFeature(
        title = "Document Search",
        subtitle = "Find text in documents",
        icon = Icons.Rounded.Search
    ),
    AiFeature(
        title = "Similar Images",
        subtitle = "Visually related photos",
        icon = Icons.Rounded.Image
    ),
    AiFeature(
        title = "Cleanup Suggestions",
        subtitle = "Free up storage space",
        icon = Icons.Rounded.Delete
    )
)

// ═══════════════════════════════════════════════════════════════════════════
// Library Tab — data-driven premium dashboard
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A single section entry rendered in the [LibraryTab].
 *
 * The Library page is built from a `List<LibrarySection>` so adding new
 * categories (Videos, Screenshots, Documents, Downloads, GIFs, RAW, etc.)
 * requires only one new object — no UI rewrite.
 */
private data class LibrarySection(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color,
    val count: Int? = null,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

/**
 * Formats a byte count into a human-readable MB/GB string for the Library
 * dashboard Storage tile. Uses binary units (1 MB = 1_048_576 bytes). Returns
 * an em-dash for empty/unknown sizes so the UI never shows a fake "0 GB".
 */
private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val mb = bytes / 1_048_576.0
    return if (mb >= 1024.0) {
        "%.2f GB".format(mb / 1024.0)
    } else {
        "%.0f MB".format(mb)
    }
}

/**
 * Premium Library dashboard — the user's content organized into categories.
 *
 * Built from a `List<LibrarySection>` so future cards (Videos, Screenshots,
 * Documents, Downloads, GIFs, RAW, Motion Photos, Burst Photos, AI Collections,
 * People, Places, Storage Analysis) can be added as single data objects
 * without any UI redesign. Disabled sections render greyed and non-clickable.
 */
@Composable
private fun LibraryTab(
    hiddenAlbumsCount: Int,
    albumsCount: Int,
    favoritesCount: Int,
    recentlyDeletedCount: Int,
    photosCount: Int,
    videosCount: Int,
    storageBytes: Long,
    onFavoritesClick: () -> Unit,
    onHiddenAlbumsClick: () -> Unit,
    onRecentlyDeletedClick: () -> Unit
) {
    val gradientStart = MaterialTheme.extendedColors.gradientStart
    val gradientEnd = MaterialTheme.extendedColors.gradientEnd

    // All four dashboard values are backed by real data from the ViewModel.
    // Counts render an em-dash only during the brief window before the first
    // MediaStore emission lands — never a fabricated number.
    val photosValue = remember(photosCount) {
        if (photosCount > 0) "%,d".format(photosCount) else "—"
    }
    val videosValue = remember(videosCount) {
        if (videosCount > 0) "%,d".format(videosCount) else "—"
    }
    val storageValue = remember(storageBytes) {
        formatStorageSize(storageBytes)
    }
    val albumsValue = remember(albumsCount) {
        "%,d".format(albumsCount)
    }

    val sections = listOf(
        LibrarySection(
            title = "Favorites",
            subtitle = "Your favorite photos and videos",
            icon = Icons.Rounded.Favorite,
            gradientStart = gradientStart,
            gradientEnd = gradientEnd,
            count = favoritesCount,
            onClick = onFavoritesClick
        ),
        LibrarySection(
            title = "Hidden Albums",
            subtitle = "View your hidden albums",
            icon = Icons.Rounded.VisibilityOff,
            gradientStart = gradientEnd,
            gradientEnd = gradientStart,
            count = hiddenAlbumsCount,
            onClick = onHiddenAlbumsClick
        ),
        LibrarySection(
            title = "Recently Deleted",
            subtitle = "View recently deleted photos & videos",
            icon = Icons.Rounded.Delete,
            gradientStart = gradientEnd,
            gradientEnd = gradientStart,
            count = recentlyDeletedCount,
            onClick = onRecentlyDeletedClick
        )
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Library",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Your content, organized",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(20.dp))

        LibrarySectionLabel("Dashboard")
        Spacer(Modifier.height(8.dp))
        LibraryStatCard(
            photosValue = photosValue,
            videosValue = videosValue,
            albumsValue = albumsValue,
            storageValue = storageValue,
            gradientStart = gradientStart,
            gradientEnd = gradientEnd
        )

        Spacer(Modifier.height(24.dp))

        LibrarySectionLabel("Quick Access")
        Spacer(Modifier.height(8.dp))

        sections.forEach { section ->
            LibrarySectionCard(section = section)
            Spacer(Modifier.height(10.dp))
        }

        // Bottom spacer for the floating nav bar
        Spacer(Modifier.height(100.dp))
    }
}

/**
 * Small uppercase section heading used between the dashboard and quick-access
 * areas of the [LibraryTab]. Keeps the visual rhythm of a premium dashboard.
 */
@Composable
private fun LibrarySectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Premium 2×2 dashboard card. A single [GlassCard] holds four [LibraryStatTile]s
 * separated by hairline dividers, mirroring the look of Samsung Gallery / Google
 * Photos overview cards. No data is fetched here — all values are passed in.
 */
@Composable
private fun LibraryStatCard(
    photosValue: String,
    videosValue: String,
    albumsValue: String,
    storageValue: String,
    gradientStart: Color,
    gradientEnd: Color
) {
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape(),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                LibraryStatTile(
                    icon = Icons.Rounded.Photo,
                    label = "Photos",
                    value = photosValue,
                    gradientStart = gradientStart,
                    gradientEnd = gradientEnd,
                    modifier = Modifier.weight(1f)
                )
                LibraryStatTile(
                    icon = Icons.Rounded.PlayArrow,
                    label = "Videos",
                    value = videosValue,
                    gradientStart = gradientEnd,
                    gradientEnd = gradientStart,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                LibraryStatTile(
                    icon = Icons.Rounded.Folder,
                    label = "Albums",
                    value = albumsValue,
                    gradientStart = gradientStart,
                    gradientEnd = gradientEnd,
                    modifier = Modifier.weight(1f)
                )
                LibraryStatTile(
                    icon = Icons.Rounded.Save,
                    label = "Storage",
                    value = storageValue,
                    gradientStart = gradientEnd,
                    gradientEnd = gradientStart,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * One cell of the [LibraryStatCard]: circular gradient icon, a small label, and
 * a bold value. Reuses the same icon-box styling as [LibrarySectionCard] so the
 * dashboard and quick-access rows feel like one design system.
 */
@Composable
private fun LibraryStatTile(
    icon: ImageVector,
    label: String,
    value: String,
    gradientStart: Color,
    gradientEnd: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Renders a single large card in the Library dashboard. */
@Composable
private fun LibrarySectionCard(section: LibrarySection) {
    var isPressed by remember { mutableStateOf(false) }
    val enabledAlpha = if (section.enabled) 1f else 0.4f

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .graphicsLayer {
                alpha = enabledAlpha
            }
            .then(
                if (section.enabled) {
                    Modifier.pointerInput(section.title) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { section.onClick() }
                        )
                    }
                } else Modifier
            ),
        shape = cardShape(),
        pressed = isPressed,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(section.gradientStart, section.gradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = section.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (section.count != null && section.count > 0) {
                GlassCountBadge(
                    count = section.count,
                    size = 28.dp
                )
            }

            Spacer(Modifier.width(4.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 180f }
            )
        }
    }
}
