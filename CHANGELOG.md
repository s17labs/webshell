# Changelog

All notable changes to WebShell will be documented here.

---

## [Unreleased]

### Added
- GitHub Actions CI (`.github/workflows/ci.yml`): builds a debug APK on every push/PR, validates the Gradle wrapper, caches Gradle, uploads the APK as an artifact
- CI status badge in README

---

## [1.0.2] — Portability & Hygiene Fixes

### Fixed
- Actually removed the misnamed `gridle/` directory (claimed in 1.0.1 but still present)
- Removed machine-specific paths from `gradle.properties` (`org.gradle.java.home`, `android.aapt2FromMavenOverride`) — set `JAVA_HOME` and `sdk.dir` in your own environment instead
- Untracked `local.properties` from git (machine-specific; stays in `.gitignore`)

### Changed
- Added `INTERNET` permission to the manifest — required for fetch/XHR/API calls; remove if fully offline
- Removed unnecessary `android.enableJetifier=true`
- `allowFileAccess` now `false` (hardening; asset loading unaffected)
- WebView background now matches the starter app's dark theme (`#0F0F0F`) instead of white
- Corrected themes.xml comment (windowBackground is black, not transparent)
- Expanded `.gitignore` (build outputs, IDE dirs, Kotlin caches)

---

## [1.0.1] — Build Fixes

### Fixed
- Missing Gradle wrapper files (`gradle/wrapper/`) - added `gradle-wrapper.jar` and `gradle-wrapper.properties`
- Duplicate plugin declarations in root `build.gradle.kts`
- Removed deprecated `allprojects` block conflicting with settings repositories
- Configured Java 17 for Gradle in `gradle.properties` (required by AGP 8.7.3)
- Added missing launcher icon resources in `res/mipmap-*/`
- Added `buildConfig = true` in app build configuration for `BuildConfig.DEBUG`
- Removed incorrect `gridle/` directory (was misnamed)

### Changed
- Updated project structure to use root level instead of `template/` subfolder
- README and docs updated to reflect actual project layout

---

## [1.0.0] — Initial Release

### Added
- Full-screen WebView shell (`MainActivity.kt`)
- JS ↔ Native bridge (`NativeBridge.kt` + `bridge.js`)
- Built-in bridge methods: toast, getDeviceInfo, openUrl, vibrate, share, emit
- Back button handling with WebView history support
- Edge-to-edge display support
- Hardware acceleration enabled by default
- Chrome DevTools debugging in debug builds
- Starter web app (`index.html`, `app.js`, `style.css`)
- ProGuard rules to preserve `@JavascriptInterface` methods
- Full documentation: getting started, project structure, bridge API, gotchas, examples
- Java reference for all Kotlin source files
