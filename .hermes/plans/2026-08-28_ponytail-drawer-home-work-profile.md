# Ponytail, drawer placement, home clock, and work-shortcut profile fix — Implementation Plan

> **For Hermes:** Implement this plan directly; keep the solution minimal (YAGNI) and validate all user-visible behavior on the Android emulator.

**Goal:** Enable Ponytail globally in full mode, move Katapult’s search field to the bottom of the app drawer, tint only the Home clock `#5A5A9C`, and preserve a selected work-profile app when saving/launching Home shortcuts.

**Architecture:** The existing search drawer already has local query state and filtered `AppModel` results; rearrange its Compose layout only. Extend the persisted shortcut identity with `userSerial`, already present in `AppModel`, and pass it through Home’s selection, icon loading, and launch path. Do not introduce new search, profile, or clock configuration infrastructure.

**Tech stack:** Hermes plugins/config, Kotlin, Jetpack Compose, Android `LauncherApps`/`UserManager`, Gradle, ADB Android emulator.

---

### Task 1: Install and configure Ponytail globally

**Files / scope:** Hermes global plugin state only (not the Katapult repo).

1. Install `neptun-zuti/ponytail-hermes` using the Hermes plugin CLI, explicitly enabled.
2. Inspect the installed plugin README/source to identify its supported config key or command for mode selection.
3. Set the mode to `full` with `hermes config set` (never hand-edit configuration).
4. Verify with `hermes plugins list --plain --no-bundled` and the resolved config. Restart only if the plugin documentation requires it; report the need transparently if gateway restart cannot be safely performed in-session.

### Task 2: Trace current shortcut persistence and profile loss

**Files:**
- `app/src/main/java/com/gezimos/katapult/MainViewModel.kt`
- `app/src/main/java/com/gezimos/katapult/util/PrefsManager.kt`
- `app/src/main/java/com/gezimos/katapult/ui/HomeScreen.kt`

1. Inspect `saveShortcut`, getters, selection callback, Home launch path, and icon rendering.
2. Confirm the defect: selection saves only package/activity, so duplicate personal/work packages resolve as the personal profile on the Home screen.
3. Add the smallest backward-compatible `userSerial` persistence key per shortcut (default `0L` for prior saved shortcuts).
4. Thread `userSerial` through save/get, `launchApp` (constructing an `AppModel` or a minimal profile-aware shortcut launch), and the Home icon call.
5. Use profile-aware uniqueness (`packageName + userSerial`) for app-order / any selection lookup touched by this change so personal and work variants never collide.

### Task 3: Move search control to bottom of drawer

**Files:**
- `app/src/main/java/com/gezimos/katapult/ui/AllAppsScreen.kt`

1. Retain the existing empty-until-query behavior and existing list filtering.
2. Place result `LazyColumn` above the search input, and move the search input, divider, and bottom navigation padding to the bottom of the normal drawer.
3. Preserve auto-focus / keyboard opening and long-press context menu behavior.
4. Do not alter reorder-mode grid behavior.

### Task 4: Tint only Home clock

**Files:**
- `app/src/main/java/com/gezimos/katapult/ui/HomeScreen.kt`
- `app/src/main/java/com/gezimos/katapult/ui/ClockDisplay.kt` (only if the API needs a clock-specific color parameter)

1. Trace the clock/date rendering interface.
2. Supply `Color(0xFF5A5A9C)` for time/AM-PM clock text only.
3. Preserve the date’s existing color exactly.
4. Do not change global `LocalInk`, drawer colors, app labels, or dark-mode palette.

### Task 5: Build and emulator validation

1. Build `:app:assembleDebug`; fix all compile failures.
2. Install debug APK into personal and managed work profiles. Create/start a managed profile and install a launchable app there if the current AVD state lacks one.
3. Verify drawer opening: empty result region and bottom-anchored Search field.
4. Type an app query; verify a vertical filtered list appears above the bottom Search field and includes badged work results.
5. In the Home shortcut picker, select a work-profile app with a same-package personal counterpart; launch the resulting Home shortcut and assert the resumed activity is `u10` (or the managed-profile user ID), not user 0.
6. Capture screenshots for visual verification.

### Task 6: Signed release verification and delivery

1. Build `:app:assembleRelease` using the existing local release keystore environment convention.
2. Verify APK signature, `debuggable=false`, and release artifact size.
3. Install the release build and repeat the essential drawer placement/filtering and work-shortcut launch test.
4. Commit only source/configuration relevant to Katapult (never keystore/password). Deliver the signed APK to Telegram.

## Risks / decisions

- Managed-profile API visibility depends on a launcher’s `QUERY_ALL_PACKAGES` permission; the manifest already has it and the prior release verified profile enumeration.
- Existing users’ shortcuts lack a user serial. Defaulting them to serial 0 preserves current behavior; only new/updated selections write profile identity.
- The previously generated signing key is not the upstream CI key. The APK will be validly signed for sideloader testing but will not update an install signed by a different certificate.
