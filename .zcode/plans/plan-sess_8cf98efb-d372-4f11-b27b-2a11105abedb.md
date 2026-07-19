## Plan: Replace Blossom Theme with a Dynamic Accent Color System

### Goal
Remove the separate Blossom theme, reduce theme modes to System/Light/Dark, and introduce a single Accent Color system that recolors the entire app (Material tokens + glass ExtendedColors) without touching neutral surfaces, blur, shadows, or thumbnails.

---

### Part A — Remove every Blossom reference (5 files)

**1. `data/ThemeMode.kt`** — drop the `BLOSSOM_PINK` enum value and its `displayName` arm. Persisted ordinals are safe: any stale value of `3` falls back to `SYSTEM_DEFAULT` via the existing `getOrElse` guard in `SettingsRepository`.

**2. `ui/theme/Color.kt`** — delete the entire Blossom palette block (lines 65–100, ~24 `Blossom*` constants). Keep shared semantic colors.

**3. `ui/theme/Theme.kt`** — delete `BlossomPinkColorScheme`, `BlossomExtendedColors`, and the `BLOSSOM_PINK ->` arm in `ProxaGalleryTheme`.

**4. `MainActivity.kt`** — remove the `ThemeMode.BLOSSOM_PINK -> true` arm from the `isLightTheme` `when`.

**5. `ui/glass/SignatureGlassNav.kt`** — delete `SignatureGlassNavBlossomPreview` and update the section comment to "Dark (flagship), Light".

---

### Part B — Add the Accent Color system

**6. NEW `data/AccentColor.kt`** — enum with the 7 required entries:
```kotlin
enum class AccentColor { BLUE, PURPLE, GREEN, ORANGE, RED, TEAL, INDIGO }
```
Each entry exposes:
- `displayName()` → "Blue", "Purple", …
- `lightSeed` → a small data holder (`primary`, `secondary`, `tertiary`) of attractive Material-style hexes
- `darkSeed` → the dark-variant equivalents
- BLUE is the default (matches current Gemini/Light primary so existing look is unchanged)

The seed is the **single source of truth** — no hardcoded accent colors anywhere else.

**7. `data/SettingsKeys.kt`** — add `ACCENT_COLOR = intPreferencesKey("accent_color")` and `Defaults.DEFAULT_ACCENT_COLOR = AccentColor.BLUE.ordinal` (following the in-file 3-step recipe).

**8. `data/SettingsRepository.kt`** — add `accentColor: Flow<AccentColor>` + `suspend fun setAccentColor(...)` mirroring the exact `themeMode` pattern (try/catch + `getOrElse { AccentColor.BLUE }`).

**9. `ui/GalleryViewModel.kt`** — add `fun setAccentColor(accent: AccentColor)` next to `setThemeMode`. Also collect the accent flow into `GalleryUiState.accentColor` so the Settings screen can show the current selection (this also fixes the pre-existing issue where `currentTheme` was never actually collected into the UI — but I will scope the fix to accent only to respect "modify only files required"; theme label fix is out of scope unless trivial).

**10. `ui/GalleryViewModelFactory.kt`** — no change needed (already constructs `SettingsRepository`).

**11. `MainActivity.kt`** — collect `settingsRepository.accentColor` via `collectAsStateWithLifecycle(initialValue = AccentColor.BLUE)` and pass `accentColor = accentColor` into `ProxaGalleryTheme(...)`.

**12. `ui/theme/Theme.kt`** — extend `ProxaGalleryTheme` signature:
```kotlin
fun ProxaGalleryTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    accentColor: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit
)
```
After resolving the base `(colorScheme, extended)` pair for the chosen mode, **apply the accent** by `colorScheme.copy(...)` overriding only the accent-sensitive Material tokens (`primary`, `primaryContainer`, `onPrimary`, `inversePrimary`, `secondary`, `secondaryContainer`, `tertiary`, `tertiaryContainer`) and by `extended.copy(...)` overriding only the accent-sensitive ExtendedColors tokens (`gradientStart`, `gradientEnd`, `glowColor`, `gradientBorderStart/Mid/End`, `cardShadowPrimary`, `cardShadowSecondary`, `badgeText`). Neutral tokens (background, surface, surfaceVariant, outline, glassSurface, glassBorder, innerHighlight, glassSurfaceStrong, onNavUnselected) are left untouched, satisfying "do NOT recolor neutrals/blur/shadows."

Accent application is a pure transformation of an existing `ColorScheme`/`ExtendedColors` — one function `applyAccent(colorScheme, extended, accent, isDark)`, no manual screen-by-screen recoloring.

**13. `ui/HomeScreen.kt`** — wire the Accent Color row:
- Thread `currentAccent: AccentColor` and `onAccentChange: (AccentColor) -> Unit` down through `HomeScreen` → `HomeScreenContent` → tab host → `SettingsTab` (mirroring the `currentTheme`/`onThemeChange` plumbing at lines 229/282/323/403/511/855).
- Replace the placeholder `SettingsRow` (lines 2417–2424): `trailingText = currentAccent.displayName()`, `onClick = { showAccentSheet = true }`.
- Add a new `AccentPickerSheet` composable cloned from `ThemePickerSheet` (ModalBottomSheet + `RadioButton` per option, selected row colored with `MaterialTheme.colorScheme.primary`, immediate select-then-dismiss behavior).
- `GalleryNavHost.kt` line ~191: pass `onAccentChange = viewModel::setAccentColor` and `currentAccent = uiState.accentColor`.

Because the accent flows through `ProxaGalleryTheme` → `MaterialTheme.colorScheme` + `LocalExtendedColors`, **every glass component, chip, nav item, FAB, switch, radio, checkbox, progress indicator, and slider inherits the new accent automatically** — no edits to any glass component (verified: all read exclusively from `MaterialTheme.extendedColors` / `MaterialTheme.colorScheme`).

---

### Part C — What is NOT touched
Gallery logic, library, AI, navigation, repository, database, MediaStore, non-theme ViewModels, glass rendering architecture, neutral colors, blur, shadows, thumbnails. No new dependencies. AGENTS.md rules respected.

---

### Part D — Verification
1. `./gradlew assembleDebug` builds successfully.
2. Confirm `grep -rni "blossom" app/src` returns nothing.
3. Light/Dark/System themes still render correctly.
4. Tapping Accent Color opens the ModalBottomSheet; selecting any of the 7 colors updates the app instantly (no restart, no Activity recreation — only `MainActivity` recomposes the theme wrapper).
5. Selection persists after restart (DataStore-backed).
6. Glass components reflect the accent via `extendedColors` without per-file edits.

---

### Notes / decisions
- The stale "Theme" label bug (Settings always shows "System Default" because `currentTheme` isn't actually collected) is **pre-existing and out of scope** — but the same plumbing is being added for accent and will work correctly because `uiState.accentColor` will be a real collected value. I will not fix the theme label unless you want it (it's a 1-line extra).
- Accent hexes will be chosen to be Material-style, attractive, and to keep sufficient contrast for `onPrimary = White` in both light and dark variants.
