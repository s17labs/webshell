# 🐚 WebShell

[![CI](https://github.com/s17labs/webshell/actions/workflows/ci.yml/badge.svg)](https://github.com/s17labs/webshell/actions/workflows/ci.yml)

**A minimal Android WebView wrapper kit for building native Android apps with plain HTML, CSS, and JavaScript.**

No React. No Electron. No Capacitor. No bloat. Just a clean, well-documented Android shell that loads your web app and gives it access to native device features through a simple JS bridge.

> Built for developers who know the web and want a clean path to shipping an Android app — without dragging in a full cross-platform framework.

---

## What Is This?

WebShell is a **template + library + guide** for building Android apps where:

- Your UI and logic live in **HTML/CSS/JS** (inside `assets/www/`)
- An Android `WebView` hosts the web app full-screen
- A **JS ↔ Native bridge** lets your web code call Android APIs
- The Android shell is **tiny** — just two Kotlin (or Java) files

Think of it like Capacitor or Cordova, but stripped down to only what you actually need, with zero magic.

---

## Requirements

| Item | Version |
|---|---|
| Java | 17 (required for AGP 8.7.3) |
| Android `minSdk` | 26 (Android 8.0) |
| Android `targetSdk` | 35 |
| Android Studio | Hedgehog or newer |
| Language | Kotlin **or** Java (both supported) |
| Build system | Gradle with Kotlin DSL (`.kts`) |

---

## Quick Start

### 1. Clone or copy the template

```bash
git clone https://github.com/s17labs/webshell.git MyApp
cd MyApp
```

### 2. Make three small edits to Android files

| File | What to change |
|---|---|
| `app/build.gradle.kts` | Set `applicationId`, `versionCode`, `versionName` |
| `app/src/main/AndroidManifest.xml` | Set app name, icon, permissions |
| `app/src/main/res/values/strings.xml` | Set app display name |

Full details in [Getting Started →](docs/getting-started.md)

### 3. Drop in your web app

Put your HTML/CSS/JS files in:

```
app/src/main/assets/www/
```

Include `bridge.js` in your HTML and you're connected to native.

### 4. Build and run

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`. Install it on your device or emulator.

---

## Project Layout

```
webshell/
├── README.md                   ← You are here
├── docs/
│   ├── getting-started.md    ← Setup walkthrough
│   ├── project-structure.md  ← Where everything lives
│   ├── bridge-api.md        ← Full JS ↔ Native bridge docs
│   ├── gotchas-and-tips.md   ← Important warnings & edge cases
│   ├── examples.md         ← Code examples
│   └── java-reference.md   ← Java equivalents for all Kotlin source files
├── app/                      ← The Android app module
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/www/         ← YOUR WEB APP GOES HERE
│       │   ├── index.html
│       │   ├── bridge.js       ← The JS-side bridge (include this)
│       │   ├── app.js        ← Starter app (replace with yours)
│       │   └── style.css    ← Starter styles (replace with yours)
│       ├── kotlin/com/yourapp/
│       │   ├── MainActivity.kt
│       │   └── NativeBridge.kt
│       └── res/
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/          ← Gradle wrapper files
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── settings.gradle.kts
└── gradlew                   ← Build script (run with: ./gradlew assembleDebug)
```

---

## How the Bridge Works (Quick Summary)

```
Your JS                     Android (Kotlin/Java)
──────                      ─────────────────────
window.Native.showToast()  →  NativeBridge.showToast()
window.Native.getInfo()    →  NativeBridge.getInfo() → returns JSON string
NativeBridge.on('event')   ←  webView.evaluateJavascript(...)
```

`bridge.js` wraps `window.Native` with safe fallbacks so your app also runs in a desktop browser for development. Full docs in [Bridge API →](docs/bridge-api.md)

---

## Docs

- [Getting Started](docs/getting-started.md) — step-by-step setup from scratch
- [Project Structure](docs/project-structure.md) — what lives where and why
- [Bridge API](docs/bridge-api.md) — full reference for JS ↔ Native communication
- [Gotchas & Tips](docs/gotchas-and-tips.md) — threading, back button, storage, security, and more
- [Examples](docs/examples.md) — real usage patterns
- [Java Reference](docs/java-reference.md) — Java equivalents for all Kotlin source files

---

## What's Included vs. What's Not

### ✅ Included
- Full-screen WebView shell (Kotlin + Java versions)
- JS ↔ Native bridge with thread safety handling
- `bridge.js` — browser-safe wrapper for the native interface
- Back button handling (WebView history navigation)
- `localStorage` / `sessionStorage` support
- Hardware acceleration enabled
- Edge-to-edge / immersive display support
- Starter HTML/CSS/JS app in `assets/www/`
- Complete documentation

### ❌ Not Included (by design)
- Camera/microphone access (requires runtime permissions — see [Gotchas](docs/gotchas-and-tips.md))
- Push notifications
- File system access beyond assets
- Auto-update mechanism
- Any JS framework — bring your own (or none)

---

## Philosophy

WebShell exists because most cross-platform tools solve problems you don't have while adding problems you didn't want. If you just need an Android app that renders your web UI and can call a few native APIs, you don't need Capacitor or Cordova. You need a WebView and a bridge.

This kit keeps the Android side as thin and readable as possible so you always understand exactly what's happening. There's no CLI, no plugin system, no config format to learn. It's just Android code that you own and can change.

---

## License

MIT — use it, fork it, ship with it.
