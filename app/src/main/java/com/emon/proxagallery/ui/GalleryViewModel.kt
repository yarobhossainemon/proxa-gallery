package com.emon.proxagallery.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.AlbumCustomization
import com.emon.proxagallery.data.AlbumCustomizationRepository
import com.emon.proxagallery.data.AlbumSortOption
import com.emon.proxagallery.data.AlbumStats
import com.emon.proxagallery.data.PhotoSortOption
import com.emon.proxagallery.data.DeleteResult
import com.emon.proxagallery.data.FavoritesRepository
import com.emon.proxagallery.data.GalleryRepository
import com.emon.proxagallery.data.MediaDetails
import com.emon.proxagallery.data.MediaItem
import com.emon.proxagallery.data.SettingsRepository
import com.emon.proxagallery.data.ThemeMode
import com.emon.proxagallery.data.TrashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.emon.proxagallery.data.MediaPagingSource
import com.emon.proxagallery.data.AlbumPagingSource
import com.emon.proxagallery.data.SearchPagingSource
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@Stable
data class GalleryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val favoriteKeys: Set<String> = emptySet(),
    val favoriteItems: List<MediaItem> = emptyList(),
    val albums: List<Album> = emptyList(),
    val selectedAlbumId: Long? = null,
    val selectedTab: Int = 0,
    val viewerPhotoIds: List<Long> = emptyList(),
    val albumSortOption: AlbumSortOption = AlbumSortOption.NAME_ASC,
    val photoSortOption: PhotoSortOption = PhotoSortOption.NEWEST,
    // --- Selection Mode ---
    val isSelectionMode: Boolean = false,
    val selectedPhotoIds: Set<Long> = emptySet(),
    val showDeleteConfirmation: Boolean = false,
    // --- Album Customization ---
    // Indexed by bucketId. Kept in sync with Room so the UI can tell whether an
    // album is pinned/hidden/renamed and whether "Reset" should be offered.
    val albumCustomizations: Map<Long, AlbumCustomization> = emptyMap(),
    val editingAlbum: Album? = null,
    val editingAlbumStats: AlbumStats? = null,
    val showEditAlbumDialog: Boolean = false,
    // --- Hidden Albums ---
    // Pure persistent flag: true iff Room holds at least one customization with
    // isHidden = true. Driven ONLY by the AlbumCustomization database — it does
    // NOT depend on rawAlbums, MediaStore loading, or any cached list — so the
    // "Hidden Albums" entry survives an app restart even before MediaStore loads.
    val hasHiddenAlbums: Boolean = false,
    // The hidden albums enriched with full MediaStore data (cover, item count,
    // display name). Used by the Hidden Albums SCREEN — not for button visibility.
    val hiddenAlbums: List<Album> = emptyList(),
    // --- Library Dashboard ---
    // Library-wide photo/video counts and total storage size. Null until the
    // first MediaStore emission lands, so the UI can show an em-dash placeholder
    // during that brief window without fabricating numbers.
    val libraryStats: AlbumStats? = null,
    // Number of items currently in Recently Deleted. Backed directly by the
    // Trash Room table via [TrashRepository.getTrashItems]; updates live.
    val recentlyDeletedCount: Int = 0
)

private const val PAGE_SIZE = 100
private const val PREFETCH_DISTANCE = 30

/**
 * Highest valid bottom-navigation tab index (Photos=0, Albums=1, AI=2,
 * Library=3, Settings=4). Used to clamp the persisted `selectedTab` value
 * read back from DataStore so a tab persisted under a previous navigation
 * scheme can never index out of range after a redesign.
 */
private const val LAST_TAB_INDEX = 4

class GalleryViewModel(
    private val galleryRepository: GalleryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val trashRepository: TrashRepository,
    private val settingsRepository: SettingsRepository,
    private val albumCustomizationRepository: AlbumCustomizationRepository
) : ViewModel() {
    private var isLoading = false
    private var hasLoadedPhotos = false
    private var isLoadingMore = false
    private var allPhotosLoaded = false
    private val _uiState = MutableStateFlow(GalleryUiState())

    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _currentAlbumId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _photoSortOption = MutableStateFlow(PhotoSortOption.NEWEST)

    private val _viewerEffects = MutableSharedFlow<ViewerEffect>(extraBufferCapacity = 1)
    val viewerEffects: SharedFlow<ViewerEffect> = _viewerEffects.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val allPhotosFlow: Flow<PagingData<MediaItem>> = _photoSortOption.flatMapLatest { sortOption ->
        val sortOrder = galleryRepository.buildSortOrder(sortOption)
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PREFETCH_DISTANCE
            )
        ) {
            MediaPagingSource(galleryRepository, sortOrder)
        }.flow
    }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val albumPhotosFlow: Flow<PagingData<MediaItem>> = combine(_currentAlbumId, _photoSortOption) { albumId, sortOption ->
        albumId to sortOption
    }.flatMapLatest { (albumId, sortOption) ->
        if (albumId == null) {
            flowOf(PagingData.empty())
        } else {
            val sortOrder = galleryRepository.buildSortOrder(sortOption)
            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    enablePlaceholders = false,
                    prefetchDistance = PREFETCH_DISTANCE
                )
            ) {
                AlbumPagingSource(galleryRepository, albumId, sortOrder)
            }.flow
        }
    }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchPhotosFlow: Flow<PagingData<MediaItem>> = combine(_searchQuery, _photoSortOption) { query, sortOption ->
        query to sortOption
    }.flatMapLatest { (query, sortOption) ->
        if (query.isBlank()) {
            flowOf(PagingData.empty())
        } else {
            val sortOrder = galleryRepository.buildSortOrder(sortOption)
            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    enablePlaceholders = false,
                    prefetchDistance = PREFETCH_DISTANCE
                )
            ) {
                SearchPagingSource(galleryRepository, query, sortOrder)
            }.flow
        }
    }.cachedIn(viewModelScope)


    /**
     * Latest raw albums from MediaStore, before customization overlay.
     *
     * A [MutableStateFlow] (not a plain field) so the Hidden Albums merge in
     * [observeHiddenAlbums] re-runs whenever MediaStore loads — without it, the
     * hidden-albums list would stay empty after a restart until something else
     * nudged the Room flow. Both [refreshAlbums] (via [applyCustomizationsToAlbums])
     * and the hidden-albums collector read from this single source of truth.
     *
     * Declared ABOVE [init] deliberately: Kotlin initializes properties and init
     * blocks top-to-bottom, so this MUST exist before [observeHiddenAlbums] runs
     * in [init], otherwise [combine] receives a null Flow and the app crashes on
     * startup (NPE on Flow.collect).
     */
    private val rawAlbums = MutableStateFlow<List<Album>>(emptyList())

    init {
        loadPhotos()
        observeFavorites()
        observeSettings()
        observeAlbumCustomizations()
        observeHiddenAlbums()
        observeLibraryStats()
        observeTrash()
        observeMediaStoreChanges()
    }

    /**
     * Live-refreshes the gallery when MediaStore changes externally — camera
     * capture, screenshot, download, copy, or a delete by another app.
     *
     * Subscribes to [GalleryRepository.mediaStoreChanges], which is backed by a
     * single [android.database.ContentObserver] (debounced + conflated) on the
     * external MediaStore volume. Each emission reuses [refreshAfterExternalChange],
     * the exact same path used after a restore: it re-queries albums and emits
     * [ViewerEffect.GalleryRefresh] so [GalleryNavHost] refreshes all three paging
     * flows. No polling, no timers, no duplicated refresh logic.
     *
     * Runs on [viewModelScope], so observation stops automatically when the
     * ViewModel is cleared and the ContentObserver is unregistered.
     */
    private fun observeMediaStoreChanges() {
        viewModelScope.launch {
            galleryRepository.mediaStoreChanges.collect {
                refreshAfterExternalChange()
            }
        }
    }

    /**
     * Restores persisted user settings (album sort + selected tab + photo sort)
     * from DataStore on startup, and keeps the UI state in sync with any
     * future change.
     *
     * All three flows are combined into one collector to avoid multiple state
     * writes racing on first emission.
     */
    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.albumSortOption
                .combine(settingsRepository.selectedTab) { sortOption, tabIndex ->
                    sortOption to tabIndex
                }
                .combine(settingsRepository.photoSortOption) { (sortOption, tabIndex), photoSort ->
                    Triple(sortOption, tabIndex, photoSort)
                }
                .collect { (sortOption, tabIndex, photoSort) ->
                    val previousSort = _uiState.value.albumSortOption
                    val previousPhotoSort = _uiState.value.photoSortOption

                    // The persisted tab index is read back verbatim on startup.
                    // The 5-tab navigation was redesigned (Photos/Albums/AI/Library/
                    // Settings), so a value persisted under the OLD scheme
                    // (0..4 mapping to Gallery/Search/Albums/Favorites/Settings)
                    // would now point at the wrong destination. Clamp any
                    // out-of-range value to 0 (Photos) and re-persist it so the
                    // user lands safely and the bad value is overwritten once.
                    val safeTabIndex = tabIndex.coerceIn(0, LAST_TAB_INDEX)
                    if (safeTabIndex != tabIndex) {
                        viewModelScope.launch {
                            settingsRepository.setSelectedTab(safeTabIndex)
                        }
                    }

                    // Update photo sort — triggers flatMapLatest Pager recreation.
                    if (previousPhotoSort != photoSort) {
                        _photoSortOption.value = photoSort
                    }

                    _uiState.value = _uiState.value.copy(
                        albumSortOption = sortOption,
                        selectedTab = safeTabIndex,
                        photoSortOption = photoSort
                    )

                    // Re-sort only when the album sort actually changes, and only after
                    // albums have been loaded. refreshAlbums() reads the updated
                    // albumSortOption from state, so it applies the new order.
                    if (previousSort != sortOption && _uiState.value.albums.isNotEmpty()) {
                        refreshAlbums()
                    }

                    // Re-sort favorites in-memory when photo sort changes.
                    if (previousPhotoSort != photoSort && _uiState.value.favoriteItems.isNotEmpty()) {
                        sortFavorites()
                    }
                }
        }
    }

    fun loadPhotos() {
        viewModelScope.launch {
            refreshAlbums()
        }
    }

    /**
     * Single source of truth for loading the album/folder list from MediaStore.
     * Called both at init (via [loadPhotos]) and after a successful delete
     * (via [onPhotoDeletedSuccess]) so folders emptied by a delete disappear
     * immediately instead of lingering until the app is restarted.
     *
     * MediaStore albums are cached in [rawAlbums] (un-customized), then the
     * current customizations are applied via [applyCustomizationsToAlbums].
     * Sorting honors [GalleryUiState.albumSortOption] and always floats pinned
     * albums to the top (see [AlbumSortOption.sort]). No album-loading logic is
     * duplicated — the customization observer reuses the same merge step.
     */
    private suspend fun refreshAlbums() {
        try {
            val loadedAlbums = withContext(Dispatchers.IO) {
                galleryRepository.getAlbums()
            }
            rawAlbums.value = loadedAlbums
            applyCustomizationsToAlbums()
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Unable to load albums."
            )
        }
    }

    /**
     * Keeps [GalleryUiState.albumCustomizations] in sync with Room and re-applies
     * the overlay whenever a customization changes — without re-querying
     * MediaStore. Reuses [applyCustomizationsToAlbums] so the merge logic lives
     * in exactly one place (also used by [refreshAlbums]).
     */
    private fun observeAlbumCustomizations() {
        viewModelScope.launch {
            albumCustomizationRepository.observeAll().collect { customizations ->
                val byBucket = customizations.associateBy { it.bucketId }
                _uiState.value = _uiState.value.copy(albumCustomizations = byBucket)
                applyCustomizationsToAlbums()
            }
        }
    }

    /**
     * Overlays the current customizations onto [rawAlbums], drops hidden albums
     * from the normal list, and writes the merged + sorted result into state.
     * Pure function of (rawAlbums, albumCustomizations, albumSortOption) — safe
     * to call from either the MediaStore load path or the customization flow.
     */
    private fun applyCustomizationsToAlbums() {
        val customizations = _uiState.value.albumCustomizations
        val sortOption = _uiState.value.albumSortOption

        val merged = rawAlbums.value.map { album ->
            val customization = customizations[album.id]
            if (customization == null) {
                album
            } else {
                val validCoverUri = customization.customCoverUri?.let { uri ->
                    if (albumCustomizationRepository.isCoverUriAccessible(uri)) uri else null
                }
                if (validCoverUri != customization.customCoverUri) {
                    viewModelScope.launch {
                        albumCustomizationRepository.clearCustomCover(album.id)
                    }
                }
                album.copy(
                    customName = customization.customName,
                    customCoverUri = validCoverUri?.let(Uri::parse),
                    isPinned = customization.isPinned,
                    isHidden = customization.isHidden,
                    colorTag = customization.colorTag,
                    sortMode = customization.sortMode
                )
            }
        }.filterNot { it.isHidden }

        _uiState.value = _uiState.value.copy(albums = sortOption.sort(merged))
    }

    /**
     * Change the album sort order. Re-sorts the already-loaded in-memory album
     * list immediately — no MediaStore re-query, so the UI stays responsive.
     * Affects ONLY the album list; photo grids are untouched.
     *
     * The choice is persisted to DataStore (fire-and-forget) so it survives
     * app restarts. Note: the persist writes the new ordinal, then
     * [observeSettings]' collector re-emits and re-sorts — but the redundant
     * re-sort is skipped because the state already holds this option.
     */
    fun setAlbumSortOption(option: AlbumSortOption) {
        if (_uiState.value.albumSortOption == option) return
        _uiState.value = _uiState.value.copy(albumSortOption = option)
        applyCustomizationsToAlbums()
        viewModelScope.launch {
            settingsRepository.setAlbumSortOption(option)
        }
    }

    // -------------------------------------------------------------------------
    // Album Customization actions
    // -------------------------------------------------------------------------

    /**
     * Opens the Edit Album bottom sheet for [album]. Also loads the album's
     * aggregate stats (photo/video count, storage size) from MediaStore so the
     * info section can display them immediately.
     */
    fun onEditAlbum(album: Album) {
        _uiState.value = _uiState.value.copy(
            editingAlbum = album,
            editingAlbumStats = null,
            showEditAlbumDialog = true
        )
        viewModelScope.launch {
            val stats = withContext(Dispatchers.IO) {
                galleryRepository.getAlbumStats(album.id)
            }
            // Only update stats if the user hasn't already dismissed the sheet.
            if (_uiState.value.editingAlbum?.id == album.id) {
                _uiState.value = _uiState.value.copy(editingAlbumStats = stats)
            }
        }
    }

    /** Dismisses the Edit Album bottom sheet and discards the in-progress edit. */
    fun onCancelEditAlbum() {
        _uiState.value = _uiState.value.copy(
            editingAlbum = null,
            editingAlbumStats = null,
            showEditAlbumDialog = false
        )
    }

    /**
     * Persists a virtual rename for the album being edited. Blank/whitespace is
     * treated as null so a cleared field falls back to the MediaStore name.
     * Updates Room only — MediaStore is never touched. The customization flow
     * then re-emits and re-merges, so the UI updates without a re-query.
     */
    fun saveAlbumRename(newName: String?) {
        val album = _uiState.value.editingAlbum ?: return
        val cleaned = newName?.trim()?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            albumCustomizationRepository.updateCustomName(album.id, cleaned)
            _uiState.value = _uiState.value.copy(
                editingAlbum = null,
                editingAlbumStats = null,
                showEditAlbumDialog = false
            )
        }
    }

    /**
     * Persists a virtual album cover URI for the album being edited. The URI is
     * stored as a string in Room; it is only used for display — no file move or
     * MediaStore change. The customization flow re-emits and re-merges.
     */
    fun saveAlbumCover(coverUri: String) {
        val album = _uiState.value.editingAlbum ?: return
        viewModelScope.launch {
            albumCustomizationRepository.updateCustomCover(album.id, coverUri)
            // Update the in-progress editing album so the bottom sheet preview
            // reflects the new cover immediately (before the flow re-emits).
            val updatedAlbum = album.copy(customCoverUri = Uri.parse(coverUri))
            _uiState.value = _uiState.value.copy(editingAlbum = updatedAlbum)
        }
    }

    /**
     * Clears ALL customizations for the album being edited — name, cover, pin,
     * hidden, color tag, sort mode — restoring full MediaStore defaults.
     */
    fun resetAlbumCustomization() {
        val album = _uiState.value.editingAlbum ?: return
        viewModelScope.launch {
            albumCustomizationRepository.resetCustomization(album.id)
            _uiState.value = _uiState.value.copy(
                editingAlbum = null,
                editingAlbumStats = null,
                showEditAlbumDialog = false
            )
        }
    }

    /** Toggles the pinned flag for [albumId]. Floats it above unpinned albums. */
    fun toggleAlbumPinned(albumId: Long) {
        val isPinned = _uiState.value.albumCustomizations[albumId]?.isPinned ?: false
        viewModelScope.launch {
            albumCustomizationRepository.setPinned(albumId, !isPinned)
        }
    }

    /** Toggles the hidden flag for [albumId]. Hidden albums leave the list. */
    fun toggleAlbumHidden(albumId: Long) {
        val isHidden = _uiState.value.albumCustomizations[albumId]?.isHidden ?: false
        viewModelScope.launch {
            albumCustomizationRepository.setHidden(albumId, !isHidden)
        }
    }

    /**
     * Keeps [GalleryUiState.hiddenAlbums] and [GalleryUiState.hasHiddenAlbums] in
     * sync with Room, joined with live MediaStore data.
     *
     * Combines TWO sources so the Hidden Albums SCREEN is correct in every phase:
     *
     * 1. [AlbumCustomizationRepository.observeHidden] — Room. Emits the persisted
     *    set of hidden customizations. Survives a restart; updates instantly on
     *    hide/unhide.
     * 2. [rawAlbums] — MediaStore (cached). Emits when [refreshAlbums] finishes.
     *
     * [hasHiddenAlbums] is derived PURELY from source (1) — it never touches
     * [rawAlbums] — so the "Hidden Albums" entry button survives an app restart
     * even before MediaStore has loaded.
     *
     * [hiddenAlbums] merges each hidden customization with its matching
     * [rawAlbums] entry (by [Album.id] / [AlbumCustomization.bucketId]) so the
     * screen has full Album data (cover URI, item count, display name). Combining
     * the two flows is what fixes the persistence bug: before this, the merge ran
     * only inside the Room collector, so when MediaStore loaded LATER the list was
     * never re-joined — the screen stayed empty after restart, then suddenly
     * filled in only when the user hid ANOTHER album (re-emitting Room). Now the
     * merge re-runs whenever EITHER source changes. A customization whose
     * MediaStore album no longer exists is dropped gracefully via mapNotNull.
     */
    private fun observeHiddenAlbums() {
        viewModelScope.launch {
            albumCustomizationRepository.observeHidden()
                .combine(rawAlbums) { hiddenCustomizations, currentRawAlbums ->
                    hiddenCustomizations to currentRawAlbums
                }
                .collect { (hiddenCustomizations, currentRawAlbums) ->
                    val merged = hiddenCustomizations.mapNotNull { customization ->
                        // Join on bucketId. If the MediaStore album is gone, drop
                        // the customization from the screen gracefully — it stays
                        // in Room and will reappear if the album returns.
                        val raw = currentRawAlbums.find { it.id == customization.bucketId }
                            ?: return@mapNotNull null
                        val validCoverUri = customization.customCoverUri?.let { uri ->
                            if (albumCustomizationRepository.isCoverUriAccessible(uri)) uri else null
                        }
                        if (validCoverUri != customization.customCoverUri) {
                            viewModelScope.launch {
                                albumCustomizationRepository.clearCustomCover(raw.id)
                            }
                        }
                        raw.copy(
                            customName = customization.customName,
                            customCoverUri = validCoverUri?.let(android.net.Uri::parse),
                            isPinned = customization.isPinned,
                            isHidden = true,
                            colorTag = customization.colorTag,
                            sortMode = customization.sortMode
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        hasHiddenAlbums = hiddenCustomizations.isNotEmpty(),
                        hiddenAlbums = merged
                    )
                }
        }
    }

    /**
     * Loads library-wide aggregate statistics (photo count, video count, total
     * storage size) once from MediaStore and pushes them into [GalleryUiState].
     *
     * Backed by [GalleryRepository.getLibraryStats], which is the same query as
     * [GalleryRepository.getAlbumStats] without the `BUCKET_ID` filter — so the
     * dashboard counts stay consistent with what the media grid shows.
     */
    private fun observeLibraryStats() {
        viewModelScope.launch {
            val stats = withContext(Dispatchers.IO) {
                galleryRepository.getLibraryStats()
            }
            _uiState.value = _uiState.value.copy(libraryStats = stats)
        }
    }

    /**
     * Observes the Recently Deleted count directly from the Trash Room table.
     * Reuses [TrashRepository.getTrashItems] (already injected) — no extra query
     * is introduced. The badge updates live as items are trashed or restored.
     */
    private fun observeTrash() {
        viewModelScope.launch {
            trashRepository.getTrashItems()
                .collect { items ->
                    _uiState.value = _uiState.value.copy(
                        recentlyDeletedCount = items.size
                    )
                }
        }
    }

    /**
     * Immediately unhides [albumId].
     *
     * Sets `isHidden = false` via Room. Both [observeAlbumCustomizations] and
     * [observeHiddenAlbums] will re-emit, so the album reappears in the main
     * Albums tab and disappears from the Hidden Albums screen — no restart needed.
     * Rename and custom cover are preserved.
     */
    fun unhideAlbum(albumId: Long) {
        viewModelScope.launch {
            albumCustomizationRepository.setHidden(albumId, false)
        }
    }

    /**
     * Opens the Edit Album bottom sheet for a hidden album.
     * Reuses the same sheet and ViewModel state as [onEditAlbum].
     */
    fun onEditHiddenAlbum(album: Album) {
        onEditAlbum(album)
    }

    /**
     * Change the photo sort order for all photo grids.
     *
     * Updates the reactive [_photoSortOption] StateFlow, which triggers
     * flatMapLatest in all three PagingData flows to recreate their Pagers.
     *
     * [GalleryUiState.photoSortOption] is updated FIRST so that the immediate
     * [sortFavorites] call below reads the new option and re-sorts the
     * in-memory favorites in the current session — no DataStore round-trip,
     * no waiting on [observeSettings] (whose re-sort guard would otherwise be
     * defeated by this very update).
     *
     * The choice is persisted to DataStore (fire-and-forget).
     */
    fun setPhotoSortOption(option: PhotoSortOption) {
        if (_uiState.value.photoSortOption == option) return
        _photoSortOption.value = option
        _uiState.value = _uiState.value.copy(photoSortOption = option)
        sortFavorites()
        viewModelScope.launch {
            settingsRepository.setPhotoSortOption(option)
        }
    }

    /**
     * Re-sorts the currently held favorites in [GalleryUiState] by the current
     * [GalleryUiState.photoSortOption]. A thin state-mutating wrapper around
     * [sortFavoriteItems]; the comparator itself lives in one place so the
     * in-session re-sort and the load path can never diverge.
     *
     * Called from [observeSettings] (to honor a persisted sort change) and from
     * [setPhotoSortOption] (to re-sort immediately, before DataStore emits).
     */
    private fun sortFavorites() {
        _uiState.value = _uiState.value.copy(
            favoriteItems = sortFavoriteItems(_uiState.value.favoriteItems)
        )
    }

    /**
     * Returns [items] sorted by the current [GalleryUiState.photoSortOption].
     *
     * Single source of truth for the favorites comparator. Both the in-session
     * re-sort ([sortFavorites]) and the reload path ([observeFavorites]) route
     * through here, so a freshly loaded list and an already-displayed list are
     * always ordered identically for a given sort option. Reads — never writes
     * — [_uiState], so callers must have already updated [GalleryUiState.photoSortOption]
     * if they want the new option applied.
     *
     * Sort semantics match the paged grids exactly: Newest/Oldest use
     * [MediaItem.dateAddedMs] (the MediaStore DATE_ADDED column the paged
     * queries ORDER BY), so "Newest" means the same thing in every tab.
     */
    private fun sortFavoriteItems(items: List<MediaItem>): List<MediaItem> {
        return when (_uiState.value.photoSortOption) {
            PhotoSortOption.NEWEST     -> items.sortedByDescending { it.dateAddedMs }
            PhotoSortOption.OLDEST     -> items.sortedBy { it.dateAddedMs }
            PhotoSortOption.NAME_ASC   -> items.sortedBy { it.displayName.lowercase() }
            PhotoSortOption.NAME_DESC  -> items.sortedByDescending { it.displayName.lowercase() }
            PhotoSortOption.LARGEST    -> items.sortedByDescending { it.fileSize }
            PhotoSortOption.SMALLEST   -> items.sortedBy { it.fileSize }
        }
    }

    /**
     * Refresh the gallery after a MediaStore change that happened outside the
     * delete flow (e.g. a restore from Recently Deleted). Reuses [refreshAlbums]
     * so there is no duplicated album-loading logic, then emits [ViewerEffect.GalleryRefresh]
     * so [GalleryNavHost] can refresh the paging flows — the same mechanism delete uses.
     */
    fun refreshAfterExternalChange() {
        viewModelScope.launch {
            refreshAlbums()
            _viewerEffects.tryEmit(ViewerEffect.GalleryRefresh)
        }
    }

    fun selectAlbum(albumId: Long?) {
        _currentAlbumId.value = albumId
        // Leaving the current album must drop any active selection so it cannot
        // leak into the previous/next screen.
        _uiState.value = _uiState.value.copy(
            selectedAlbumId = albumId,
            isSelectionMode = false,
            selectedPhotoIds = emptySet()
        )
    }

    fun selectTab(tabIndex: Int) {
        if (_uiState.value.selectedTab == tabIndex) return
        _uiState.value = _uiState.value.copy(
            selectedTab = tabIndex,
            // Switching screens clears the selection; it is scoped per screen.
            isSelectionMode = false,
            selectedPhotoIds = emptySet()
        )
        viewModelScope.launch {
            settingsRepository.setSelectedTab(tabIndex)
        }
    }

    /**
     * Currently selected theme mode. Collected by the Settings screen to show
     * the live selection in the Theme row.
     */
    val themeMode: kotlinx.coroutines.flow.Flow<ThemeMode>
        get() = settingsRepository.themeMode

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    /**
     * Currently selected accent color. Collected by the Settings screen to show
     * the live selection and by the Activity (via the Repository) to recolor the
     * theme. Mirrors [themeMode].
     */
    val accentColor: kotlinx.coroutines.flow.Flow<com.emon.proxagallery.data.AccentColor>
        get() = settingsRepository.accentColor

    fun setAccentColor(accent: com.emon.proxagallery.data.AccentColor) {
        viewModelScope.launch {
            settingsRepository.setAccentColor(accent)
        }
    }

    private val mediaItemCache = java.util.concurrent.ConcurrentHashMap<Long, MediaItem>()

    /**
     * IDs that have been saved to Room but whose MediaStore deletion is still pending
     * (because the system permission dialog is showing). If the user cancels, these
     * records and their local files must be cleaned up.
     */
    private val pendingDeletions = java.util.concurrent.ConcurrentHashMap<Long, Boolean>()

    suspend fun getMediaItem(id: Long): MediaItem? {
        return mediaItemCache[id] ?: withContext(Dispatchers.IO) {
            galleryRepository.getPhotoById(id)?.also {
                mediaItemCache[id] = it
            }
        }
    }

    suspend fun getMediaDetails(id: Long): MediaDetails? = withContext(Dispatchers.IO) {
        galleryRepository.getMediaDetails(id)
    }

    fun prepareViewer(photoId: Long) {
        viewModelScope.launch {
            val sortOrder = galleryRepository.buildSortOrder(_uiState.value.photoSortOption)
            val ids = withContext(Dispatchers.IO) {
                val albumId = _uiState.value.selectedAlbumId
                val query = _uiState.value.searchQuery

                when {
                    // Inside an album drill-in.
                    albumId != null -> galleryRepository.getPhotoIdsForAlbum(albumId, sortOrder)
                    // Search now lives inside the Photos tab — a non-blank query
                    // means the grid is showing filtered search results.
                    query.isNotBlank() -> galleryRepository.getPhotoIdsForSearch(query, sortOrder)
                    // Default Photos grid (all media).
                    else -> galleryRepository.getAllPhotoIds(sortOrder)
                }
            }
            _uiState.value = _uiState.value.copy(viewerPhotoIds = ids)
        }
    }

    /**
     * Prepares the photo viewer for the Favorites route.
     *
     * Favorites is no longer a tab — it lives behind its own navigation route
     * ([FavoritesScreen]). Its viewer ordering is the already-sorted in-memory
     * [GalleryUiState.favoriteItems] (kept in sync with the photo sort option by
     * [observeFavorites]/[sortFavorites]), so no repository query is needed.
     */
    fun prepareFavoritesViewer() {
        _uiState.value = _uiState.value.copy(
            viewerPhotoIds = _uiState.value.favoriteItems.map { it.id }
        )
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.favoriteKeys.collect { keys ->
                val favIds = keys.mapNotNull { key ->
                    key.substringAfter(":").toLongOrNull()
                }
                val items = withContext(Dispatchers.IO) {
                    galleryRepository.getPhotosByIds(favIds)
                }
                // Route every reload through the same comparator used by the
                // in-session re-sort, so adding/removing a favorite can never
                // drop the list back into the loader's hard-coded DATE_ADDED
                // DESC order. Reads the current photoSortOption from state.
                val sortedItems = sortFavoriteItems(items)
                _uiState.value = _uiState.value.copy(
                    favoriteKeys = keys,
                    favoriteItems = sortedItems
                )
            }
        }
    }

    fun toggleFavorite(id: Long, isVideo: Boolean) {
        viewModelScope.launch {
            favoritesRepository.toggle(id, isVideo)
        }
    }

    fun isFavorite(id: Long, isVideo: Boolean): Boolean {
        val key = if (isVideo) "v:$id" else "i:$id"
        return _uiState.value.favoriteKeys.contains(key)
    }

    fun onSearchQueryChange(query: String) {
        if (_uiState.value.searchQuery == query) return
        _searchQuery.value = query
        // Changing the search query leaves the previous results screen, so the
        // selection must be cleared to avoid leaking into the new results.
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            isSelectionMode = false,
            selectedPhotoIds = emptySet()
        )
    }

    /**
     * Initiates deletion for the current photo.
     *
     * 1. Copies the original file to app-private storage.
     * 2. Verifies the copy succeeded (file exists, size > 0).
     * 3. Generates a thumbnail from the copied file.
     * 4. Saves the TrashItem to Room with local file paths.
     * 5. Only then deletes the original from MediaStore.
     *
     * If any step before MediaStore delete fails, the operation is aborted
     * and any partial files are cleaned up. The original MediaStore file is
     * never touched unless the copy, verification, thumbnail, and Room insert
     * all succeed.
     */
    fun deleteCurrentPhoto(id: Long, uri: Uri) {
        viewModelScope.launch {
            val mediaItem = mediaItemCache[id] ?: return@launch

            val copyResult = withContext(Dispatchers.IO) {
                trashRepository.copyToTrash(
                    sourceUri = uri,
                    mimeType = mediaItem.mimeType,
                    fileSize = mediaItem.fileSize ?: 0
                )
            }

            if (copyResult == null) {
                _viewerEffects.tryEmit(
                    ViewerEffect.ShowError("Failed to back up file before deletion.")
                )
                return@launch
            }

            val thumbPath = withContext(Dispatchers.IO) {
                trashRepository.generateThumbnail(
                    localFilePath = copyResult.localFilePath,
                    isVideo = mediaItem.isVideo
                )
            }

            // Capture the original MediaStore folder BEFORE the file is deleted,
            // so restore can return the file to the same location. Read while the
            // original row still exists; if missing, falls back to the default.
            val originalRelativePath = withContext(Dispatchers.IO) {
                galleryRepository.getMediaDetails(mediaItem.id)?.relativePath
            }

            withContext(Dispatchers.IO) {
                trashRepository.moveToTrash(
                    mediaId = mediaItem.id,
                    uri = mediaItem.uri,
                    displayName = mediaItem.displayName,
                    mimeType = mediaItem.mimeType,
                    originalAlbum = mediaItem.bucketDisplayName,
                    localFilePath = copyResult.localFilePath,
                    localThumbnailPath = thumbPath,
                    fileSize = copyResult.fileSize,
                    originalRelativePath = originalRelativePath
                )
            }

            pendingDeletions[id] = true

            val deleteResult = withContext(Dispatchers.IO) {
                galleryRepository.deletePhoto(uri)
            }

            when (deleteResult) {
                is DeleteResult.RequiresPermission -> {
                    _viewerEffects.tryEmit(
                        ViewerEffect.LaunchSystemDeleteDialog(
                            intentSender = deleteResult.intentSender,
                            retryUri = deleteResult.retryUri
                        )
                    )
                }
                is DeleteResult.Success -> {
                    onPhotoDeletedSuccess(id)
                }
                is DeleteResult.Error -> {
                    pendingDeletions.remove(id)
                    withContext(Dispatchers.IO) {
                        trashRepository.deleteForever(id)
                    }
                    _viewerEffects.tryEmit(
                        ViewerEffect.ShowError(deleteResult.message)
                    )
                }
            }
        }
    }

    /**
     * Called after the user confirmed the system delete-permission dialog (RESULT_OK).
     */
    fun confirmDeleteAfterPermission(id: Long, retryUri: Uri?) {
        viewModelScope.launch {
            if (retryUri != null) {
                withContext(Dispatchers.IO) {
                    galleryRepository.retryDelete(retryUri)
                }
            }
            onPhotoDeletedSuccess(id)
        }
    }

    /**
     * Called when the user cancels the system delete-permission dialog (RESULT_CANCELED).
     * Removes the Room record and cleans up local files.
     */
    fun cancelDelete(id: Long) {
        viewModelScope.launch {
            pendingDeletions.remove(id)
            withContext(Dispatchers.IO) {
                trashRepository.deleteForever(id)
            }
        }
    }

    private fun onPhotoDeletedSuccess(id: Long) {
        pendingDeletions.remove(id)
        mediaItemCache.remove(id)
        val updatedIds = _uiState.value.viewerPhotoIds.filter { it != id }
        _uiState.value = _uiState.value.copy(viewerPhotoIds = updatedIds)

        // Refresh the album list so folders emptied by this delete disappear
        // immediately. Runs on the same coroutine scope as init; reuses the
        // single album-loading path so there is no duplicated logic.
        viewModelScope.launch {
            refreshAlbums()
        }

        if (updatedIds.isEmpty()) {
            _viewerEffects.tryEmit(ViewerEffect.NavigateBack)
        } else {
            _viewerEffects.tryEmit(ViewerEffect.PhotoDeleted)
        }
    }

    // -------------------------------------------------------------------------
    // Selection Mode
    // -------------------------------------------------------------------------

    /**
     * Enters selection mode and immediately selects [id] as the first item.
     * Both changes are applied atomically in a single state update to avoid
     * a transient frame where selection mode is active but nothing is selected.
     */
    fun enterSelectionMode(id: Long) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedPhotoIds = setOf(id)
        )
    }

    /**
     * Toggles selection for [id].
     *
     * If [id] was already selected it is removed; otherwise it is added.
     * When the resulting set is empty, selection mode is exited automatically
     * so callers never observe a state where mode is active but nothing is
     * selected. The ID is a stable MediaStore identifier — immune to paging
     * refreshes, recomposition, and sort-order changes.
     */
    fun togglePhotoSelection(id: Long) {
        val current = _uiState.value.selectedPhotoIds
        val updated = if (id in current) current - id else current + id
        val stillInSelectionMode = updated.isNotEmpty()
        _uiState.value = _uiState.value.copy(
            isSelectionMode = stillInSelectionMode,
            selectedPhotoIds = if (stillInSelectionMode) updated else emptySet()
        )
    }

    /**
     * Adds [id] to the current selection without removing anything — used by
     * drag-selection so items crossed by the pointer can only ever be turned
     * ON, never accidentally toggled OFF. Existing selections (made before the
     * drag, or crossed earlier in the same drag) are preserved.
     *
     * If selection mode is not yet active it is entered; the first photo touched
     * by a drag flows through here so the gesture and the selection state stay
     * in sync atomically.
     */
    fun selectPhoto(id: Long) {
        val current = _uiState.value.selectedPhotoIds
        if (id in current && _uiState.value.isSelectionMode) return
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedPhotoIds = current + id
        )
    }

    /**
     * Exits selection mode and clears all selected IDs.
     * Called by the back-press handler and the close button in the selection bar.
     */
    fun exitSelectionMode() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedPhotoIds = emptySet()
        )
    }

    /**
     * Selects every photo in the current data source — or deselects all if the
     * current selection already covers the entire source.
     *
     * The full ID set is resolved from the repository (Gallery / Search / Album)
     * or from in-memory favorites, so the action works immediately without
     * scrolling and never loads [MediaItem] objects into memory. Paging remains
     * responsible only for display. The ViewModel stores only the resulting
     * [GalleryUiState.selectedPhotoIds] (a [Set<Long>]).
     *
     * Repository ID queries run on [Dispatchers.IO]; the UI state update happens
     * back on the main thread.
     */
    fun selectAllOrNone() {
        val albumId = _uiState.value.selectedAlbumId
        val query = _uiState.value.searchQuery
        val sortOrder = galleryRepository.buildSortOrder(_uiState.value.photoSortOption)

        viewModelScope.launch {
            val allIds = withContext(Dispatchers.IO) {
                when {
                    albumId != null ->
                        galleryRepository.getPhotoIdsForAlbum(albumId, sortOrder)
                    query.isNotBlank() ->
                        galleryRepository.getPhotoIdsForSearch(query, sortOrder)
                    else ->
                        galleryRepository.getAllPhotoIds(sortOrder)
                }
            }

            val allIdSet = allIds.toSet()
            if (allIdSet.isEmpty()) return@launch

            val current = _uiState.value.selectedPhotoIds
            // "Everything already selected" = same size and current contains all.
            val everythingSelected =
                current.size == allIdSet.size && allIdSet.all { it in current }

            val updated = if (everythingSelected) emptySet() else allIdSet
            val stillInSelectionMode = updated.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isSelectionMode = stillInSelectionMode,
                selectedPhotoIds = updated
            )
        }
    }

    /**
     * "Select all" for the Favorites route.
     *
     * Favorites is no longer a tab — it lives behind its own navigation route
     * and resolves its full ID set from the in-memory [GalleryUiState.favoriteItems]
     * (no repository query). Behavior otherwise mirrors [selectAllOrNone].
     */
    fun selectAllFavoritesOrNone() {
        val allIdSet = _uiState.value.favoriteItems.map { it.id }.toSet()
        if (allIdSet.isEmpty()) return

        val current = _uiState.value.selectedPhotoIds
        val everythingSelected =
            current.size == allIdSet.size && allIdSet.all { it in current }

        val updated = if (everythingSelected) emptySet() else allIdSet
        val stillInSelectionMode = updated.isNotEmpty()
        _uiState.value = _uiState.value.copy(
            isSelectionMode = stillInSelectionMode,
            selectedPhotoIds = updated
        )
    }

    /**
     * Returns true when every currently selected photo is already a favorite.
     * Drives the Favorite/Unfavorite button label in the selection bar.
     */
    fun allSelectedAreFavorite(): Boolean {
        val keys = _uiState.value.favoriteKeys
        return _uiState.value.selectedPhotoIds.all { id ->
            keys.contains("i:$id") || keys.contains("v:$id")
        }
    }

    /**
     * Adds or removes the favorite flag for every selected photo in one decision.
     *
     * Rather than toggling each id individually, we decide once: if ALL selected
     * photos are already favorites, remove the flag from all of them; otherwise
     * add the flag to all. Avoids the ambiguity of a per-item toggle when the
     * selection is a mixed favorites/non-favorites set. Reuses [FavoritesRepository]
     * so the existing favorites flow keeps [GalleryUiState.favoriteItems] in sync.
     *
     * After the batch completes, selection mode is exited via [exitSelectionMode]
     * — consistent with every other successful batch action.
     */
    fun bulkToggleFavorite() {
        val selected = _uiState.value.selectedPhotoIds
        if (selected.isEmpty()) return
        val removeAll = allSelectedAreFavorite()
        viewModelScope.launch {
            // Resolve every selected item once to learn its media type; no
            // per-id MediaStore round-trips.
            val items = withContext(Dispatchers.IO) {
                galleryRepository.getPhotosByIds(selected.toList())
            }
            val byId = items.associateBy { it.id }
            selected.forEach { id ->
                val isVideo = byId[id]?.isVideo ?: false
                val key = if (isVideo) "v:$id" else "i:$id"
                val isFav = _uiState.value.favoriteKeys.contains(key)
                if (removeAll && isFav) {
                    favoritesRepository.toggle(id, isVideo)
                } else if (!removeAll && !isFav) {
                    favoritesRepository.toggle(id, isVideo)
                }
            }
            exitSelectionMode()
        }
    }

    /**
     * Resolves the selected photo IDs into shareable content Uris and emits a
     * one-shot [ViewerEffect.SharePhotos] for [GalleryNavHost] to launch a single
     * ACTION_SEND_MULTIPLE intent. The UI never builds the Android share request.
     *
     * Selection mode is exited once the share intent payload is emitted, so the
     * user returns to the normal toolbar after the chooser is launched — matching
     * every other successful batch action.
     */
    fun shareSelected() {
        val selected = _uiState.value.selectedPhotoIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                galleryRepository.getPhotosByIds(selected.toList())
            }
            if (items.isNotEmpty()) {
                _viewerEffects.tryEmit(ViewerEffect.SharePhotos(items.map { it.uri }))
                exitSelectionMode()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Bulk Delete (with trash backup + system permission dialog)
    // -------------------------------------------------------------------------

    /**
     * Selected IDs awaiting a bulk MediaStore delete. Set when the user confirms
     * the confirmation dialog, carried through the system permission flow, and
     * cleared on success or cancel so it never leaks across operations.
     */
    private var pendingBulkDeleteIds: Set<Long>? = null

    /**
     * Request the bulk-delete confirmation dialog. Pure state change — the actual
     * deletion only happens after [confirmBulkDelete]. No-op when nothing is
     * selected (the UI also disables the button, this guards against stray calls).
     */
    fun requestBulkDelete() {
        if (_uiState.value.selectedPhotoIds.isEmpty()) return
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = true)
    }

    /**
     * Cancel the bulk-delete: hide the confirmation dialog and discard the
     * pending selection. Selection mode itself is left untouched.
     */
    fun cancelBulkDelete() {
        pendingBulkDeleteIds = null
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
    }

    /**
     * Confirm the bulk-delete. Resolves the selected IDs into [MediaItem]s via the
     * repository (no object passing from the UI), backs each up to app-private
     * trash storage, then issues a single batched MediaStore delete request.
     *
     * - RequiresPermission → emit [ViewerEffect.LaunchSystemDeleteDialog] (reusing
     *   the existing OS-dialog launcher) and keep [pendingBulkDeleteIds].
     * - Success → finalize, refresh paging, exit selection mode.
     * - Error → roll back trash records, emit [ViewerEffect.ShowError].
     *
     * The confirmation dialog is always dismissed here.
     */
    fun confirmBulkDelete() {
        val ids = _uiState.value.selectedPhotoIds
        if (ids.isEmpty()) {
            _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
            return
        }
        pendingBulkDeleteIds = ids
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)

        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                galleryRepository.getPhotosByIds(ids.toList())
            }
            if (items.isEmpty()) {
                pendingBulkDeleteIds = null
                _viewerEffects.tryEmit(ViewerEffect.ShowError("Selected items no longer exist."))
                return@launch
            }

            val backedUp = items.mapNotNull { item ->
                val copy = withContext(Dispatchers.IO) {
                    trashRepository.copyToTrash(
                        sourceUri = item.uri,
                        mimeType = item.mimeType,
                        fileSize = item.fileSize ?: 0
                    )
                }
                if (copy == null) {
                    null
                } else {
                    val thumb = withContext(Dispatchers.IO) {
                        trashRepository.generateThumbnail(
                            localFilePath = copy.localFilePath,
                            isVideo = item.isVideo
                        )
                    }
                    item to copy.localFilePath to thumb
                }
            }

            if (backedUp.size != items.size) {
                backedUp.forEach { (pair, _) ->
                    withContext(Dispatchers.IO) { trashRepository.deleteForever(pair.first.id) }
                }
                pendingBulkDeleteIds = null
                _viewerEffects.tryEmit(
                    ViewerEffect.ShowError("Failed to back up files before deletion.")
                )
                return@launch
            }

            backedUp.forEach { (pair, thumb) ->
                val (item, localPath) = pair
                withContext(Dispatchers.IO) {
                    trashRepository.moveToTrash(
                        mediaId = item.id,
                        uri = item.uri,
                        displayName = item.displayName,
                        mimeType = item.mimeType,
                        originalAlbum = item.bucketDisplayName,
                        localFilePath = localPath,
                        localThumbnailPath = thumb,
                        fileSize = item.fileSize ?: 0,
                        originalRelativePath = null
                    )
                }
            }

            val deleteResult = withContext(Dispatchers.IO) {
                galleryRepository.deletePhotos(items.map { it.uri })
            }

            when (deleteResult) {
                is DeleteResult.RequiresPermission -> {
                    _viewerEffects.tryEmit(
                        ViewerEffect.LaunchSystemDeleteDialog(
                            intentSender = deleteResult.intentSender,
                            retryUri = deleteResult.retryUri
                        )
                    )
                }
                is DeleteResult.Success -> {
                    onBulkDeletedSuccess()
                }
                is DeleteResult.Error -> {
                    pendingBulkDeleteIds = null
                    items.forEach { item ->
                        withContext(Dispatchers.IO) { trashRepository.deleteForever(item.id) }
                    }
                    _viewerEffects.tryEmit(ViewerEffect.ShowError(deleteResult.message))
                }
            }
        }
    }

    /**
     * Called after the user confirmed the system bulk-delete dialog (RESULT_OK).
     * On Android 11+ the OS already performed the deletion; on Android 10 we retry
     * the single pending uri. Then finalizes and exits selection mode.
     */
    fun confirmBulkDeleteAfterPermission(retryUri: Uri?) {
        viewModelScope.launch {
            if (retryUri != null) {
                withContext(Dispatchers.IO) { galleryRepository.retryDelete(retryUri) }
            }
            onBulkDeletedSuccess()
        }
    }

    /**
     * Finalizes a successful bulk delete: clears caches, refreshes albums, emits
     * [ViewerEffect.GalleryRefresh] (which refreshes all three paging flows in
     * [GalleryNavHost]), and exits selection mode via [exitSelectionMode].
     */
    private fun onBulkDeletedSuccess() {
        val ids = pendingBulkDeleteIds.orEmpty()
        pendingBulkDeleteIds = null
        ids.forEach { mediaItemCache.remove(it) }
        val updatedIds = _uiState.value.viewerPhotoIds.filter { it !in ids }
        _uiState.value = _uiState.value.copy(viewerPhotoIds = updatedIds)
        exitSelectionMode()
        viewModelScope.launch { refreshAlbums() }
        _viewerEffects.tryEmit(ViewerEffect.GalleryRefresh)
    }
}