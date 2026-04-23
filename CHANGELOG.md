# Changelog

All notable changes to WebShell will be documented here.

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
