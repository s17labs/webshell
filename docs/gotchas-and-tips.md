# Gotchas & Tips

The things that will bite you if you don't know about them. Read this before you ship.

---

## Threading (The Big One)

### Bridge methods run on a background thread

Every `@JavascriptInterface` method is invoked on a background (non-UI) thread. If you try to do UI operations directly, you'll get crashes or silent failures.

```kotlin
// ❌ This will crash on some devices
@JavascriptInterface
fun badToast(msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

// ✅ Wrap UI work in runOnUiThread
@JavascriptInterface
fun goodToast(msg: String) {
    (context as? Activity)?.runOnUiThread {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
```

### `evaluateJavascript` must run on the main thread

```kotlin
// ❌ Called from a background thread — will crash
webView.evaluateJavascript("window.__bridge_event('data')", null)

// ✅ Use post() or runOnUiThread
webView.post {
    webView.evaluateJavascript("window.__bridge_event('data')", null)
}
```

### Safe pattern for async bridge operations

If a bridge method starts async work and needs to push results back:

```kotlin
@JavascriptInterface
fun fetchData(url: String) {
    Thread {
        val result = doNetworkCall(url) // background OK
        activity.runOnUiThread {
            activity.webView.evaluateJavascript(
                "window.__bridge_fetchResult('${result.escapeForJs()}')", null
            )
        }
    }.start()
}
```

---

## Back Button Handling

By default, the hardware back button will close your app even if the WebView has history to go back through (e.g., the user navigated between views in your JS app).

Override this in `MainActivity`:

```kotlin
@Deprecated("Deprecated in Java")
override fun onBackPressed() {
    if (webView.canGoBack()) {
        webView.goBack()
    } else {
        super.onBackPressed()
    }
}
```

Or with the newer `OnBackPressedCallback` API (API 33+):

```kotlin
onBackPressedDispatcher.addCallback(this) {
    if (webView.canGoBack()) {
        webView.goBack()
    } else {
        isEnabled = false
        onBackPressedDispatcher.onBackPressed()
    }
}
```

If you're using a single-page app with JS-side routing and you handle all navigation in JS (no actual WebView page loads), you may want to pass the back event to JS instead:

```kotlin
webView.post {
    webView.evaluateJavascript("window.__bridge_backPressed()", null)
}
```

---

## Storage

### What works out of the box

With `domStorageEnabled = true` (set in the template):

- `localStorage` ✅
- `sessionStorage` ✅
- `IndexedDB` ✅
- Cookies ✅

These persist across app restarts (except `sessionStorage`).

### Where data is stored

WebView storage lives in the app's private data directory. It's wiped when the user clears app data or uninstalls the app. It is **not** backed up by Android's auto-backup by default.

### Don't use WebSQL

WebSQL is deprecated and disabled in modern Android WebView. Use `localStorage` or `IndexedDB` instead.

### Large data / files

For anything larger than a few MB, use the native side:

```kotlin
@JavascriptInterface
fun saveFile(filename: String, content: String) {
    val file = File(context.filesDir, filename)
    file.writeText(content)
}

@JavascriptInterface
fun readFile(filename: String): String {
    val file = File(context.filesDir, filename)
    return if (file.exists()) file.readText() else ""
}
```

---

## Escaping Data in `evaluateJavascript`

When you push data from Android to JS, the payload goes inside a JavaScript string literal. If the data contains quotes, backslashes, or newlines, it will break your JS.

Always escape:

```kotlin
fun String.escapeForJs(): String {
    return this
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}

// Usage
webView.post {
    webView.evaluateJavascript(
        "window.__bridge_result('${result.escapeForJs()}')", null
    )
}
```

Or better, wrap your payload in a JSON string and parse it in JS:

```kotlin
// Kotlin
val safe = JSONObject().put("data", rawData).toString()
webView.evaluateJavascript("window.__bridge_result(${safe})", null)
```

```javascript
// JS
NativeBridge.on('result', (obj) => {
    console.log(obj.data); // already parsed, no escape issues
});
```

---

## Runtime Permissions

WebView itself does not trigger Android permission dialogs. If you need permissions (camera, microphone, location, storage), you must request them from the native side.

### Pattern: Request from native, pass result to JS

```kotlin
// In MainActivity
private val locationPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    webView.post {
        webView.evaluateJavascript(
            "window.__bridge_locationPermission(${granted})", null
        )
    }
}

// In NativeBridge
@JavascriptInterface
fun requestLocation() {
    activity.runOnUiThread {
        activity.locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
```

```javascript
// In JS
NativeBridge.on('locationPermission', (granted) => {
    if (granted) startUsingLocation();
    else showPermissionDeniedMessage();
});

NativeBridge.emit('requestLocation', {});
```

Don't forget to add the permission to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### Common permissions and where to declare them

| Feature | Permission |
|---|---|
| Internet (remote URLs) | `android.permission.INTERNET` |
| Fine location | `android.permission.ACCESS_FINE_LOCATION` |
| Coarse location | `android.permission.ACCESS_COARSE_LOCATION` |
| Camera | `android.permission.CAMERA` |
| Vibrate | `android.permission.VIBRATE` (no dialog needed) |
| Read media images (API 33+) | `android.permission.READ_MEDIA_IMAGES` |
| Post notifications (API 33+) | `android.permission.POST_NOTIFICATIONS` |

---

## Security

### The bridge is only exposed to your bundled files

`addJavascriptInterface` with `file:///android_asset/` as the origin means only content you bundled in the APK can access `window.Native`. This is the safe default.

### Never set `setAllowUniversalAccessFromFileURLs(true)`

This lets any file:// URL access any other file:// URL, including your bridge. Do not set it.

### Never set `setAllowFileAccessFromFileURLs(true)`

Similar risk — this lets JS in file:// pages make cross-origin file requests. Leave it off.

### If you load remote URLs

If your app ever loads remote URLs (e.g. an OAuth flow, a help page), those pages will also have access to `window.Native` unless you guard against it.

Safe option — only expose bridge to your known origin:

```kotlin
webViewClient = object : WebViewClient() {
    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        val isTrusted = url.startsWith("file:///android_asset/")
        // Remove bridge for untrusted pages
        if (!isTrusted) {
            view.removeJavascriptInterface("Native")
        }
    }
}
```

### Don't put secrets in `assets/`

Your `assets/www/` files are bundled in the APK and easily extractable. Do not hardcode API keys, private keys, or passwords in your web app files. Use Android's `BuildConfig` for any secrets you need to inject at build time, or fetch them from a server.

---

## WebView Chrome Debugging

You can inspect your WebView in Chrome DevTools from desktop while the app runs on a connected device or emulator.

Enable it in `MainActivity`:

```kotlin
// Enable only in debug builds
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

Then on desktop: open Chrome and go to `chrome://inspect/#devices`. Your app's WebView will appear. Full console, network, DOM inspection — everything you'd expect.

> **Important:** Never ship with `setWebContentsDebuggingEnabled(true)` in production. This lets anyone with Chrome and a USB cable inspect and modify your app's web content. Wrap it in `if (BuildConfig.DEBUG)`.

---

## Mixed Content (HTTP vs HTTPS)

If you're loading remote resources (fonts, APIs) that are HTTP (not HTTPS), you may hit mixed content blocking.

For development only, you can allow it:

```kotlin
settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
```

In production, use HTTPS. If you control the server, enforce HTTPS. If you don't, consider loading the resource via the native side and passing it to JS.

---

## Slow Initial Load

On first launch, WebView can take a moment to initialize. To prevent the user seeing a white flash before your HTML loads:

```kotlin
// Set background to match your app's background color
webView.setBackgroundColor(Color.parseColor("#121212"))

// Or show a splash screen (see SplashScreen API for Android 12+)
```

On Android 12+, use the `SplashScreen` API for a proper animated splash.

---

## Text Selection and Long Press

By default, WebView allows text selection (blue handles, copy menu). If your app is more native-app-like and you don't want this:

```javascript
/* In your CSS */
* {
    -webkit-user-select: none;
    user-select: none;
}

/* Re-enable for text inputs */
input, textarea {
    -webkit-user-select: text;
    user-select: text;
}
```

---

## Viewport and System Bar Insets

For a truly edge-to-edge experience (drawing under the status bar and navigation bar), set this in `MainActivity`:

```kotlin
WindowCompat.setDecorFitsSystemWindows(window, false)
```

Then in your CSS, handle the safe areas:

```css
body {
    padding-top: env(safe-area-inset-top);
    padding-bottom: env(safe-area-inset-bottom);
    padding-left: env(safe-area-inset-left);
    padding-right: env(safe-area-inset-right);
}
```

`env(safe-area-inset-*)` is supported in Android WebView (Chromium) on API 28+. For API 26–27, you may need to calculate insets via the bridge:

```kotlin
@JavascriptInterface
fun getSystemInsets(): String {
    val insets = ViewCompat.getRootWindowInsets(window.decorView)
    val bars = insets?.getInsets(WindowInsetsCompat.Type.systemBars())
    return JSONObject().apply {
        put("top", bars?.top ?: 0)
        put("bottom", bars?.bottom ?: 0)
    }.toString()
}
```

---

## Performance Tips

- **Enable hardware acceleration** — the template does this by default via `android:hardwareAccelerated="true"` in the manifest. Don't remove it.
- **Avoid heavy CSS animations** on lower-end devices. WebView is hardware-accelerated but still not a native view. Test on a mid-range API 26 device, not just an emulator.
- **Minimise bridge calls in tight loops.** Each JS ↔ native call has overhead (thread hop, JNI). Batch data where possible.
- **Use `requestAnimationFrame`** for your animation loops — it syncs to the display's refresh rate.
- **Lazy-load heavy assets.** `assets/www/` is local, so loads are fast, but large images still block rendering. Lazy-load anything not visible on first paint.

---

## Development Workflow

Changes to web files (`assets/www/`) require a full rebuild and redeploy every time. Here are some ways to speed up iteration:

### Option 1: Chrome DevTools (recommended)

Enable `setWebContentsDebuggingEnabled(true)` as described above. Reload the page with `window.location.reload()` or from the DevTools console without rebuilding. You can also live-edit CSS and JS directly in DevTools.

### Option 2: Load from a local server (dev mode only)

In a debug build, point the WebView at your dev server instead of assets:

```kotlin
val startUrl = if (BuildConfig.DEBUG) {
    "http://10.0.2.2:3000" // 10.0.2.2 is localhost from the emulator
} else {
    "file:///android_asset/www/index.html"
}
webView.loadUrl(startUrl)
```

`10.0.2.2` is the special address for the emulator to reach your host machine's localhost. For a real device, use your machine's LAN IP.

Don't forget to add `<uses-permission android:name="android.permission.INTERNET" />` for this to work, and add a `network_security_config.xml` to allow cleartext traffic to `10.0.2.2` in debug.

### Option 3: ADB push (for quick iteration on device)

You can push updated assets directly to a debug-mode app without a full rebuild:

```bash
adb push ./www/app.js /sdcard/Android/data/com.yourapp/files/www/app.js
```

This requires some changes to `MainActivity` to load from the device filesystem in debug mode.

---

## WebView Version and Compatibility

Android WebView is powered by Chromium and is updated independently of the Android OS via the Play Store. This means you generally get a modern, up-to-date browser engine even on older Android versions.

However, on API 26–27 (Android 8.0–8.1), the WebView version can be older than on API 28+. Things to watch out for on those versions:

- CSS Grid is fully supported but some newer features (`:has()`, `@container`) may not work
- `env(safe-area-inset-*)` may not be available (see above)
- Some ES2020+ JavaScript features may need polyfilling if you need to support very old WebView builds

Test on a real API 26 device if you care about the low end of your `minSdk`.
