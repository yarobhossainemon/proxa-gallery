
# Persistent User Settings — Implementation Plan

## Current Architecture Analysis

### Existing Persistence
| Feature | Storage Mechanism | File |
|---|---|---|
| Favorites | DataStore Preferences (`"favorites"` store, `stringSetPreferencesKey`) | `FavoritesRepository.kt` |
| Trash items | Room database (`proxa_trash.db`) | `TrashRepository.kt` / `TrashDatabase.kt` |

### NOT Persisted (should be)
| Setting | Current Default | Location | Persists? |
|---|---|---|---|
| **Album sort option** | `AlbumSortOption.NAME_ASC` | `GalleryUiState.albumSortOption` | **No** — resets on every launch |
| **Selected gallery tab** | `0` (Gallery tab) | `GalleryUiState.selectedTab` | **No** — resets on every launch |

### DI Pattern
- **No DI framework** — manual `ViewModelProvider.Factory` pattern
- Repositories take `Context` directly, create their own dependencies
- `GalleryViewModelFactory` manually wires `GalleryRepository`, `FavoritesRepository`, `TrashRepository`

### DataStore Already in Project
- `androidx.datastore:datastore-preferences:1.1.3` is **already declared** in `libs.versions.toml` and used by `FavoritesRepository`
- **No new dependencies needed**

---

## Proposed Architecture

### Single DataStore Instance for Settings
Create a new dedicated DataStore file named `"user_settings"` (separate from `"favorites"` to maintain clean separation of concerns). This DataStore will hold all user preferences going forward.

### New File: `SettingsRepository.kt`
A centralized repository in the `data` package that:
- Creates a single `DataStore<Preferences>` via a file-scoped extension property
- Exposes each setting as an individual `Flow<T>` for easy collection
- Provides `suspend fun` setters for each setting
- Handles `IOException` gracefully (emits defaults on corruption)

**Key design decisions:**
- **Enum settings** (`AlbumSortOption`) stored as `intPreferencesKey` using enum ordinal → cheap, no string parsing, no duplicate key collisions
- **Int settings** (`selectedTab`) stored as `intPreferencesKey` directly
- **Future extensibility:** Adding a new setting requires only adding a key + a getter Flow + a setter in `SettingsRepository`, then collecting it in the appropriate ViewModel. No new DataStore instances, no new files.

### Data Flow

```
DataStore ("user_settings")
    ↑ write                    ↓ read (Flow)
SettingsRepository          SettingsRepository
    ↑ write                    ↓ collect
GalleryViewModel            GalleryViewModel
    ↑ setState               ↓ collectAsStateWithLifecycle
GalleryNavHost / HomeScreen
```

---

## Files to Create (2 files)

### 1. `app/src/main/java/com/emon/proxagallery/data/SettingsRepository.kt`
**Purpose:** Central repository for all user settings.
**Contents:**
- Private `Context.dataStore` extension property → `preferencesDataStore(name = "user_settings")`
- Keys: `ALBUM_SORT_OPTION_KEY` (intPreferencesKey), `SELECTED_TAB_KEY` (intPreferencesKey)
- `albumSortOption: Flow<AlbumSortOption>` — maps int to enum, falls back to `NAME_ASC`
- `selectedTab: Flow<Int>` — reads int, falls back to `0`
- `setAlbumSortOption(option: AlbumSortOption)` — writes `option.ordinal`
- `setSelectedTab(tab: Int)` — writes int
- IOException catch on all flows (emits defaults)

### 2. `app/src/main/java/com/emon/proxagallery/data/SettingsKeys.kt`
**Purpose:** Centralized keys and defaults object for settings. This keeps `SettingsRepository` clean and makes it trivial to see all available settings in one place.
**Contents:**
- `object SettingsKeys` with all preference keys and their defaults as constants
- This is the single place to add new keys in the future

---

## Files to Modify (2 files)

### 3. `app/src/main/java/com/emon/proxagallery/ui/GalleryViewModel.kt`
**Why:** Must inject `SettingsRepository` and persist settings when they change.

**Changes:**
- **Constructor:** Add `settingsRepository: SettingsRepository` parameter
- **`init` block:** Add `observeSettings()` call alongside existing `observeFavorites()`
- **New `observeSettings()` private method:** Collects `albumSortOption` and `selectedTab` flows from `SettingsRepository` and merges them into `_uiState`
- **`setAlbumSortOption()`:** After updating `_uiState`, also call `settingsRepository.setAlbumSortOption(option)` (fire-and-forget launch)
- **`selectTab()`:** After updating `_uiState`, also call `settingsRepository.setSelectedTab(tabIndex)` (fire-and-forget launch)

**No other logic changes.** All existing behavior preserved.

### 4. `app/src/main/java/com/emon/proxagallery/ui/GalleryViewModelFactory.kt`
**Why:** Must create and pass `SettingsRepository` to `GalleryViewModel`.

**Changes:**
- **Constructor:** (unchanged — still takes `Context`)
- **`create()`:** Add `SettingsRepository(appContext)` to the `GalleryViewModel(...)` constructor call

---

## Files NOT Modified (confirmed no changes needed)

| File | Reason |
|---|---|
| `HomeScreen.kt` | Already reads `uiState.albumSortOption` and `uiState.selectedTab` — no changes needed |
| `GalleryNavHost.kt` | Passes factory as before — factory change is transparent |
| `FavoritesRepository.kt` | Separate concern, untouched |
| `TrashRepository.kt` | Separate concern, untouched |
| `AlbumSortOption.kt` | Already has ordinal-based enum, untouched |
| `build.gradle.kts` / `libs.versions.toml` | DataStore dependency already present |
| `ProxaGalleryApplication.kt` | No global initialization needed |

---

## Future Settings — How to Add

To add a new setting (e.g., grid size), the changes required are:

1. **`SettingsKeys.kt`** — Add a new key constant and default value (1 line)
2. **`SettingsRepository.kt`** — Add a getter `Flow` and a setter (4-6 lines)
3. **Relevant ViewModel** — Collect the flow in `init`, write in the setter (2-3 lines)

No new files, no new DataStore instances, no architectural changes. Total: ~10 lines per new setting.

---

## Potential Risks

| Risk | Mitigation |
|---|---|
| DataStore read latency on first launch | `collectAsStateWithLifecycle` with `initialValue` from `GalleryUiState` defaults ensures UI shows immediately while DataStore loads. The flow emission replaces the default once ready. |
| Race condition: ViewModel sets state before DataStore loads | SettingsRepository flows emit defaults instantly on IOException or first read, so there's always a valid value. The ViewModel merges incoming values — last-emitted wins, which is correct. |
| DataStore corruption | IOException catch in every flow → emit default → user gets working app with defaults. DataStore auto-heals on next write. |
| Album sort applied before DataStore loads | `AlbumSortOption.NAME_ASC` default is applied first. If user had `NEWEST`, it will briefly show `NAME_ASC` then switch. This is a single-frame transition on cold start — acceptable. To eliminate even this, we could read synchronously in `init`, but that breaks the reactive pattern. |

---

## Implementation Order

1. **Create `SettingsKeys.kt`** — key definitions and defaults
2. **Create `SettingsRepository.kt`** — DataStore, flows, and setters
3. **Modify `GalleryViewModelFactory.kt`** — wire SettingsRepository into factory (small change)
4. **Modify `GalleryViewModel.kt`** — inject SettingsRepository, observe settings, persist on change
5. **Build and test** — verify album sort and tab selection survive app restart
