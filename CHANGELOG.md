# Changelog

All notable changes to WebShell will be documented here.

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
