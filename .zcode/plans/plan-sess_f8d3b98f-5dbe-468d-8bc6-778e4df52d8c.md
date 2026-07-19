## Premium Settings Screen — Implementation Plan

**Scope:** Settings UI only. No business logic, no ViewModels, no navigation changes. Everything stays inside `HomeScreen.kt` (where `SettingsTab` already lives), plus a one-line `buildConfig = true` in gradle.

### Decisions (confirmed)
- Theme picker shows all 4 existing `ThemeMode` entries (reuses enum as-is).
- Version displays `"v" + BuildConfig.VERSION_NAME` (resolves to `"v1.0"`, no hardcoding).
- Theme picker upgraded to **ModalBottomSheet**, and the currently-selected option's RadioButton reflects the live theme.

---

### Step 1 — Enable BuildConfig in `app/build.gradle.kts`
Add `buildConfig = true` to the existing `buildFeatures { compose = true }` block. Required because AGP 8+ no longer generates `BuildConfig` by default, and the version row must read `BuildConfig.VERSION_NAME`.

### Step 2 — Add imports to `HomeScreen.kt`
Add only what the new rows use (icons + bottom sheet bits):
- `androidx.compose.material.icons.rounded.*`: `Palette`, `Person`, `Star`, `BugReport`, `PrivacyTip`, `Description`, `ChevronRight` (or `AutoMirrored.ArrowForwardIos`).
- `androidx.compose.material3.ModalBottomSheet` + `rememberModalBottomSheetState` — already imported (lines 98/100). Add `Text`/`TextButton` — already imported.
- `com.emon.proxagallery.BuildConfig` (new).

### Step 3 — Replace `SettingsItem` with a reusable `SettingsRow`
Introduce one shared `SettingsRow` composable (replaces `SettingsItem`) so every row shares identical layout and avoids duplication (a hard requirement in the spec). Signature:

```kotlin
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,           // shown under title if present
    trailingText: String? = null,       // e.g. "Dark", "v1.0"
    onClick: (() -> Unit)? = null,      // null => non-clickable, no chevron
    iconContentDescription: String? = title
)
```

Layout (matches existing design language):
- Row, `fillMaxWidth`, `clickable` only when `onClick != null`, `heightIn(min = 56.dp)` for the touch target.
- Leading: same 40dp circular `primary.copy(0.12f)` tinted box with 20dp icon (unchanged from current `SettingsItem`).
- Middle: title (`bodyLarge`, Bold, `onSurface`) + optional subtitle (`bodyMedium`, `onSurfaceVariant`).
- Trailing: optional value text (`bodyMedium`, `onSurfaceVariant`) + chevron icon (`Icons.Rounded.ChevronRight`, 24dp, tint `onSurfaceVariant`) — chevron only when `onClick != null`.
- Stable values, no animations, `contentDescription` on icon for a11y.

### Step 4 — Rebuild `SettingsTab` with two sections
Two grouped `GlassCard` blocks (same `elevation = 0.dp`, `padding(16.dp)`, `HorizontalDivider` separators as today), each preceded by a section label. Reuse the `LibrarySectionLabel` pattern inline (it's `private` to this file already at line 3076 — I'll just call it; no new helper).

```
Settings                          // 28sp Bold onBackground
Customize your Proxa Gallery experience   // subtitle, bodyMedium onSurfaceVariant

LibrarySectionLabel("Appearance")  → GlassCard:
  • SettingsRow(Palette, "Theme", trailingText=<current>, onClick=openSheet)
  • divider
  • SettingsRow(Palette/Paint, "Accent Color", trailingText="Blue", onClick={ TODO })

LibrarySectionLabel("About")       → GlassCard:
  • SettingsRow(Person, "Developer", subtitle="Emon")         // no click
  • divider
  • SettingsRow(Info, "Version", trailingText="v" + BuildConfig.VERSION_NAME)  // no click
  • divider
  • SettingsRow(Star, "Rate Proxa Gallery", onClick={ TODO })
  • divider
  • SettingsRow(BugReport, "Send Feedback", onClick={ TODO })
  • divider
  • SettingsRow(PrivacyTip, "Privacy Policy", onClick={ TODO })
  • divider
  • SettingsRow(Description, "Open Source Licenses", onClick={ TODO })
```

The whole column gets `verticalScroll(rememberScrollState())` so all rows fit on small screens. `verticalScroll`/`rememberScrollState` are already imported (lines 62/63).

### Step 5 — Show the current theme value
`SettingsTab` needs the live theme to (a) show "Dark"/"Light"/... as the Theme row's trailing text and (b) mark the selected radio in the sheet. Thread it down the existing callback chain that already passes `onThemeChange`:
- `HomeScreen` (218) → inner content (310, 496) → `SettingsTab` (837): add one new param `currentTheme: ThemeMode` next to each existing `onThemeChange`, defaulting to `ThemeMode.SYSTEM_DEFAULT`, and read it from `MainActivity` where `themeMode` is already collected.

**Important constraint check:** `MainActivity` already collects `themeMode` and passes `onThemeChange`. Adding `currentTheme = themeMode` is a pure pass-through of an already-existing state — no new ViewModel, no repository change, no new DataStore read. This satisfies "ViewModels only unless absolutely required for existing settings."

### Step 6 — Replace `ThemePickerDialog` with `ThemePickerSheet`
A `ModalBottomSheet` (uses `cardShape()`-consistent styling via Material3 defaults) listing all `ThemeMode.entries`, each row: RadioButton (selected = `mode == currentTheme`), label `mode.displayName()`, full-width clickable. Selecting calls `onThemeSelected` + dismisses. Keep a Cancel affordance via swipe-down / scrim (default M3 behavior); no extra button needed, matching the glass language.

### Step 7 — Delete the old `SettingsItem` and `ThemePickerDialog`
Both are fully superseded. Nothing else references them.

### Step 8 — Build & verify
- `./gradlew :app:assembleDebug` (or the project's build command).
- Fix any compile errors.
- Manual verify points (code-review level, since I can't run the app): theme still applies immediately (unchanged wiring), version shows `v1.0`, rows align, glass styling matches, no regressions outside Settings (only `HomeScreen.kt` + `MainActivity.kt` + `build.gradle.kts` touched).

---

### Files touched
1. `app/build.gradle.kts` — add `buildConfig = true` (one line).
2. `app/src/main/java/com/emon/proxagallery/ui/HomeScreen.kt` — rewrite `SettingsTab`, replace `SettingsItem`→`SettingsRow`, replace `ThemePickerDialog`→`ThemePickerSheet`, add imports, thread `currentTheme` through the `HomeScreen`→content→`SettingsTab` chain.
3. `app/src/main/java/com/emon/proxagallery/MainActivity.kt` — pass `currentTheme = themeMode` into `HomeScreen` (one keyword arg).

No new files. No repository/database/ViewModel/navigation changes. All click handlers for Accent Color / Rate / Feedback / Privacy / Licenses stay as `TODO` comments — UI prep only, per spec.