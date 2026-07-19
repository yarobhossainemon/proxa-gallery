# Plan: Fix Light/Blossom Theme UI Visibility

## Root Cause Analysis

### Issue 1 — Album Title Readability
**File:** `HomeScreen.kt` lines 2298-2309
**Problem:** Album name text is hardcoded `Color.White` and item count uses `onSurfaceVariant`. The caption bar is a `GlassSurface(strong = true)` which in Light/Blossom themes renders as ~95% opaque white/very-light-pink with a white inner highlight gradient. White text on this is nearly invisible. The underlying `Color.Black.copy(alpha = 0.25f)` overlay on the thumbnail is too subtle to change anything.

**Strategy:** Use the theme's `onSurface` color (dark text) for the album name and `onSurfaceVariant` for the item count. These are already correctly defined per theme: `LightTextPrimary (#1A1C1E)` for Light, `BlossomTextPrimary (#2D1B2E)` for Blossom, and `Color.White` for Dark. This makes the text automatically adapt without hardcoding.

### Issue 2 — Library Count Badge Visibility
**File:** `GlassBadge.kt` line 70
**Problem:** `GlassCountBadge` hardcodes `Color.White` for the number text. The badge uses `glassBackground` (not gradient), so in Light/Blossom it's a near-white translucent fill. White text on a near-white badge is invisible.

**Strategy:** Add a dedicated `badgeText` color to the `ExtendedColors` system, defined per theme as white for Dark and the theme's `onSurface` (dark text) for Light/Blossom. This follows the existing pattern of extending `ExtendedColors` with new per-theme tokens. The badge itself is small (28dp) and uses `glassBackground`, so dark text will be clearly readable against the translucent light background while remaining subtle.

### Issue 3 — Sort Button Visibility
**File:** `HomeScreen.kt` lines 679-685 (divider) and line 2014 (icon tint)
**Problem:** The divider uses `onSurface.copy(alpha = 0.15f)` — too faint on translucent glass. The sort icon uses `onSurface.copy(alpha = 0.7f)` — adequate but could be improved for better contrast.

**Strategy:** Increase the divider alpha from `0.15f` to `0.25f` for better visibility in all themes while remaining subtle. Increase the sort icon alpha from `0.7f` to `0.85f` for stronger contrast. These are small, conservative adjustments that improve readability without being visually heavy.

## Files to Modify

| # | File | Changes |
|---|------|---------|
| 1 | `ui/theme/Theme.kt` | Add `badgeText: Color` field to `ExtendedColors` data class; populate it in `DarkExtendedColors` (white), `LightExtendedColors` (`LightTextPrimary`), `BlossomExtendedColors` (`BlossomTextPrimary`); update `LocalExtendedColors` default |
| 2 | `ui/glass/GlassBadge.kt` | Change `GlassCountBadge` text color from `Color.White` to `MaterialTheme.extendedColors.badgeText` |
| 3 | `ui/HomeScreen.kt` | (Issue 1) Change album name color from `Color.White` to `MaterialTheme.colorScheme.onSurface`; change item count color from `onSurfaceVariant` to `onSurface.copy(alpha = 0.7f)` for lighter weight. (Issue 3) Increase divider alpha from `0.15f` to `0.25f`; increase sort icon alpha from `0.7f` to `0.85f` |

## What Stays Unchanged
- **Dark theme appearance** — `onSurface` is already `Color.White` in Dark, so album names remain white. `badgeText` will be `Color.White` for Dark. Divider/icon alpha changes are universal but minor.
- **Layouts, navigation, functionality** — only color values change.
- **Badge size, shape, and position** — untouched.
- **Sort button layout and glass effect** — untouched.
- **No new dependencies** — only existing Material 3 and ExtendedColors system.