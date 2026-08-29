# Katapult

## Project purpose
Katapult is a deliberately minimal Android launcher, adapted here as a personal build for the Unihertz Titan 2 Elite. Preserve a fast, keyboard-friendly, low-distraction Home screen while supporting both personal and managed-work profiles.

## Current customization
- The app drawer searches only after typing, ranks prefix matches first, and anchors results above the bottom search field.
- Apps, icons, launches, and Home shortcuts are profile-aware; managed-profile apps retain Android briefcase badging.
- Home time/AM-PM uses orange `#FC7703`; the date remains dark.
- Removed hidden apps, lockscreen, screensaver, donation, and in-app licence UI. Keep repository licence files.
- Double-tap on Home locks the device through the `TapToSleepAccessibilityService`; Gestures contains **Enable tap to sleep**, which opens Android Accessibility settings. This replaces double-tap brightness.

## Build and verification
- Source Android tooling first: `source ~/toolchain/env.sh`.
- Build debug: `./gradlew :app:assembleDebug --no-daemon`.
- Release APKs must be R8-minified, signed with the existing Katapult release key, signature-verified with `apksigner`, and confirmed non-debuggable with `apkanalyzer`.
- Do not package emulator or `androidTest` artifacts in release APKs. Test launcher changes on `emulator-5554` before release when emulator testing is requested.

## Guardrails
- Preserve core launcher behavior, profile handling, and bottom-anchored drawer search.
- Avoid reintroducing removed feature APIs, settings, manifest components, or persistence paths.
- Never print, commit, or modify signing credentials.
