# Bottom-up drawer search and work-profile icon fix — Implementation Plan

> **For Hermes:** Implement directly, retaining the established minimal search-list design and validating a signed release on an Android emulator.

## Scope
1. Present search results from the bottom of the usable drawer, immediately above the bottom search field; the highest-relevance result is closest to the field.
2. Respect Titan 2 Elite's tall-but-narrow physical display and top-left front-camera cutout through safe status/cutout insets, without hard-coded device dimensions.
3. Ensure a work-profile WhatsApp icon uses Android's native badged `LauncherActivityInfo` icon, rather than the unbadged bundled WhatsApp resource.

## Root causes
- `LazyColumn` lays results from the top by default. Combined with an open keyboard, low-ranked matches can occupy limited space while the best result is farthest from the bottom search field.
- `IconUtility.loadIcon()` resolves `customIcons[packageName]` before the work-profile branch. WhatsApp is in that custom map, so its work entry is rendered from Katapult's unbadged `R.drawable.whatsapp`, bypassing `LauncherActivityInfo.getBadgedIcon()`.

## Minimal implementation
1. **`AllAppsScreen.kt`**
   - Rank existing case-insensitive label matches with exact match, then prefix match, then later substring match; retain label order within each rank.
   - Render the `LazyColumn` with `reverseLayout = true`, so the first/best row anchors directly above the bottom search box and successive matches grow upward.
   - Retain `statusBarsPadding()` on the outer surface so system cutout/status insets protect the topmost result; no hard-coded aspect-ratio branch.
2. **`IconUtility.kt`**
   - Keep explicit user icon overrides first.
   - For every non-main user serial, resolve activity icon via `LauncherApps` before considering bundled per-package artwork. This returns Android's native work-profile-badged icon for WhatsApp and all other work apps.
   - Preserve bundled artwork for personal-profile apps only.

## Validation
1. Debug build.
2. Android 35 emulator with a managed work profile. Confirm a broad query renders the first/best results bottom-up above Search; confirm the content respects the status area.
3. Install a WhatsApp-named test app in the work profile or inspect icon rendering path using a work-profile package; verify badged native path is used.
4. Build, sign, inspect, and install the release APK; repeat the drawer interaction smoke test.
5. Run the Ponytail milestone audit (findings only; no speculative cleanup) and deliver the signed APK.
