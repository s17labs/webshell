# Project Structure

A complete map of every file in the project and what it does.

---

## Top-Level Layout

```
webshell/
├── app/                        ← The Android app module
│   ├── build.gradle.kts        ← App-level build config
│   └── src/main/
│       ├── AndroidManifest.xml ← App manifest (name, permissions, etc.)
│       ├── assets/             ← Bundled static files
│       │   └── www/            ← Your entire web app lives here
│       ├── kotlin/             ← Android/Kotlin source files
│       │   └── com/yourapp/
│       │       ├── MainActivity.kt
│       │       └── NativeBridge.kt
│       └── res/                ← Android resources (icons, strings, etc.)
├── build.gradle.kts            ← Root-level build config
├── gradle.properties           ← Gradle and JVM settings
├── gradle/wrapper/            ← Gradle wrapper files
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── settings.gradle.kts         ← Project name and module declarations
└── gradlew                   ← Build script
```

---

## The `assets/www/` Directory

This is where your web app lives. Everything in this folder is bundled into your APK and accessible via `file:///android_asset/www/`.

```
assets/www/
├── index.html      ← Entry point. Always loaded on app start.
├── bridge.js       ← JS-side bridge wrapper. Always include this.
├── app.js          ← Starter app logic. Replace with yours.
└── style.css       ← Starter styles. Replace with yours.
```

### Rules for `assets/www/`

- **`index.html` is always the entry point.** `MainActivity` loads `file:///android_asset/www/index.html` on startup. Don't rename it unless you also update `MainActivity.kt`.
- **`bridge.js` must always be included.** It wraps `window.Native` safely. Without it, any call to `NativeBridge.*` in your JS will throw errors in the browser.
- **Subdirectories work fine.** You can organise into `css/`, `js/`, `fonts/`, `images/` — whatever structure you prefer.
- **No hot reload.** Changes to web files require a rebuild and redeploy. For faster iteration, see [Gotchas & Tips → Development Workflow](gotchas-and-tips.md#development-workflow).

---

## Kotlin Source Files

### `MainActivity.kt`

The single Activity that hosts the WebView. It:

- Creates a `WebView` and sets it as the full-screen content view
- Configures WebView settings (JS, DOM storage, file access, etc.)
- Attaches the `NativeBridge` as a JavaScript interface
- Sets up a `WebViewClient` to handle navigation
- Handles the back button to support WebView history navigation
- Optionally applies edge-to-edge / immersive display

**You generally don't need to edit this file** unless you want to:
- Change WebView settings (e.g. enable zoom, change user agent)
- Handle system events (e.g. `onNewIntent` for deep links)
- Add more Activity-level behaviour

### `NativeBridge.kt`

The bridge between your JavaScript and Android. Each `@JavascriptInterface`-annotated method becomes callable from JS as `window.Native.methodName()`.

**This is the file you'll edit most** as your app grows — you add new bridge methods here as you need new native capabilities.

Key rules for bridge methods:
- Must be annotated with `@JavascriptInterface`
- Can accept `String`, `Int`, `Boolean`, `Double` parameters
- Can return `String`, `Int`, `Boolean`, `Double`, or `void`
- To return complex data, serialize it to a JSON string and return that
- Run on a **background thread** — use `runOnUiThread {}` for any UI operations

See [Bridge API](bridge-api.md) for full details.

---

## Android Config Files

### `AndroidManifest.xml`

Declares your app to the Android system. Key things to set here:

| Attribute | Purpose |
|---|---|
| `android:label` | App name shown on home screen and recents |
| `android:icon` | App icon |
| `android:theme` | App theme (controls status bar, system bars) |
| `<uses-permission>` | Permissions your app needs |
| `android:hardwareAccelerated` | Must be `true` for smooth WebView rendering |

### `app/build.gradle.kts`

App-level build config. The things you'll touch:

```kotlin
android {
    namespace = "com.yourcompany.yourapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yourcompany.yourapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

You generally won't need to touch `dependencies {}` unless you're adding native Android libraries (e.g. for camera, location, etc.).

### `build.gradle.kts` (root)

Declares the Kotlin and Android Gradle plugin versions. You shouldn't need to change this unless you're upgrading plugin versions.

### `settings.gradle.kts`

Sets the project name and includes the `app` module. Change the project name here:

```kotlin
rootProject.name = "MyApp"
```

### `gradle.properties`

JVM and Gradle settings. The defaults are fine. Useful options:

```properties
# Increase memory if builds are slow
org.gradle.jvmargs=-Xmx2048m

# Enable build caching
org.gradle.caching=true
```

---

## `res/` Directory

Standard Android resources. The template includes only what's needed:

```
res/
├── mipmap-*/       ← App icon in multiple densities
├── values/
│   ├── strings.xml ← App name string resource
│   └── themes.xml  ← App theme
└── xml/
    └── network_security_config.xml  ← Network security policy (if needed)
```

You don't need to touch `res/` for most web-app use cases. The main exception is replacing the app icon files in `mipmap-*/`.

---

## What You Will and Won't Modify

| File | How often you'll touch it |
|---|---|
| `assets/www/*` | Constantly — this is your whole app |
| `NativeBridge.kt` | When you need new native features |
| `AndroidManifest.xml` | Once at setup, then when adding permissions |
| `app/build.gradle.kts` | Once at setup, then on version bumps |
| `MainActivity.kt` | Rarely — only for advanced configuration |
| `res/mipmap-*` | Once — to set your app icon |
| `settings.gradle.kts` | Once — to set your project name |
| `build.gradle.kts` (root) | Almost never |
