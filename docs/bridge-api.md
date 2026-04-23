# Bridge API

The bridge is how your JavaScript talks to Android and vice versa. This document covers everything about how it works, what's built in, and how to extend it.

---

## How It Works

WebView exposes a special mechanism called `addJavascriptInterface`. It lets you attach a Java/Kotlin object to `window` in JavaScript. Any method annotated with `@JavascriptInterface` on that object becomes callable from JS.

```
JS: window.Native.showToast("hello")
        ↓
Android: NativeBridge.showToast("hello")
```

WebShell attaches `NativeBridge` to `window.Native`. The `bridge.js` file wraps this with safe fallbacks so calls don't crash when running in a browser.

---

## `bridge.js` — The JS-Side Wrapper

Always include `bridge.js` before your app scripts:

```html
<script src="bridge.js"></script>
```

This gives you a global `NativeBridge` object with two layers:

1. **Safety check** — every method first checks `typeof window.Native !== 'undefined'`. If you're running in a browser (for development), it falls back gracefully instead of throwing.
2. **Convenience wrappers** — methods that parse JSON responses, normalize arguments, and provide sensible browser fallbacks.

---

## Built-In Bridge Methods

These are available out of the box.

### `NativeBridge.toast(message)`

Shows a native Android toast notification.

```javascript
NativeBridge.toast('Saved!');
```

**Browser fallback:** `console.log('[Toast]', message)`

---

### `NativeBridge.getDeviceInfo()`

Returns a plain object with basic device information.

```javascript
const info = NativeBridge.getDeviceInfo();
console.log(info.model);        // e.g. "Pixel 7"
console.log(info.manufacturer); // e.g. "Google"
console.log(info.sdk);          // e.g. 34
console.log(info.appVersion);   // e.g. "1.0.0"
```

**Browser fallback:** Returns `{ model: 'browser', sdk: 0, manufacturer: 'web', appVersion: 'dev' }`

---

### `NativeBridge.isNative()`

Returns `true` if running inside the Android WebView, `false` if in a browser.

```javascript
if (NativeBridge.isNative()) {
    // Android-only behaviour
} else {
    // Browser development fallback
}
```

---

### `NativeBridge.on(event, callback)`

Registers a callback that Android can trigger from the native side.

```javascript
NativeBridge.on('filePicked', (data) => {
    console.log('File selected:', data);
});
```

On the Android side, fire it with:

```kotlin
webView.post {
    webView.evaluateJavascript("window.__bridge_filePicked('${payload}')", null)
}
```

See [Calling JS from Android](#calling-js-from-android) for full details.

---

### `NativeBridge.emit(event, payload)`

Sends a named event with a JSON payload from JS to Android.

```javascript
NativeBridge.emit('userAction', { type: 'tap', target: 'submitBtn' });
```

On the Android side, handle it in `NativeBridge.emit()`:

```kotlin
@JavascriptInterface
fun emit(event: String, payload: String) {
    val data = JSONObject(payload)
    when (event) {
        "userAction" -> handleUserAction(data)
    }
}
```

---

## Adding Your Own Bridge Methods

### 1. Add the method in `NativeBridge.kt`

```kotlin
@JavascriptInterface
fun vibrate(durationMs: Int) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs.toLong())
    }
}
```

> **Threading:** `@JavascriptInterface` methods run on a **background thread**, not the main UI thread. Any UI operation (Toast, Dialog, view changes) must be wrapped in `runOnUiThread {}`. See [Threading](#threading) below.

### 2. Add the wrapper in `bridge.js`

```javascript
vibrate: (ms = 200) => {
    if (NativeBridge.isNative()) window.Native.vibrate(ms);
},
```

### 3. Use it in your web app

```javascript
NativeBridge.vibrate(300);
```

---

## Supported Parameter and Return Types

The JavaScript interface only directly supports these types across the bridge:

| Type | JS → Android | Android → JS (return value) |
|---|---|---|
| `String` | ✅ | ✅ |
| `Int` / `int` | ✅ | ✅ |
| `Boolean` / `boolean` | ✅ | ✅ |
| `Double` / `double` | ✅ | ✅ |
| `void` | — | ✅ |
| Objects / Arrays | ❌ (use JSON string) | ❌ (use JSON string) |

### Passing complex data

Serialize to JSON on both sides:

**JS → Android:**

```javascript
// JS side
const payload = JSON.stringify({ name: 'Alice', score: 42 });
window.Native.saveScore(payload);
```

```kotlin
// Kotlin side
@JavascriptInterface
fun saveScore(json: String) {
    val obj = JSONObject(json)
    val name = obj.getString("name")
    val score = obj.getInt("score")
    // ...
}
```

**Android → JS (return value):**

```kotlin
@JavascriptInterface
fun getSettings(): String {
    return JSONObject().apply {
        put("theme", "dark")
        put("fontSize", 16)
    }.toString()
}
```

```javascript
// JS side
const settings = JSON.parse(window.Native.getSettings());
```

---

## Calling JS from Android

Android can call JavaScript in the WebView using `evaluateJavascript`. This is how you push data or events from native to your web app.

### From an Activity or Service

```kotlin
// Must run on the main thread
runOnUiThread {
    webView.evaluateJavascript("window.__bridge_dataReady('${jsonPayload}')", null)
}
```

Or use `webView.post {}` which also dispatches to the main thread:

```kotlin
webView.post {
    webView.evaluateJavascript("window.__bridge_locationUpdate('${lat},${lng}')", null)
}
```

### From inside NativeBridge

`NativeBridge` doesn't hold a reference to the WebView by default (to keep it clean). If you need to fire events back to JS from inside a bridge method, pass a callback reference or hold a weak reference to the activity:

```kotlin
class NativeBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun startScan() {
        // Start something async, then push result back
        doAsyncWork { result ->
            activity.runOnUiThread {
                activity.webView.evaluateJavascript(
                    "window.__bridge_scanResult('${result}')", null
                )
            }
        }
    }
}
```

This requires making `webView` accessible in `MainActivity`. Add `internal var webView: WebView? = null` and assign it after creation.

### Registering event listeners in JS

```javascript
NativeBridge.on('scanResult', (data) => {
    console.log('Scan result:', data);
});
```

`bridge.js` stores these under `window.__bridge_<eventName>`, which is what `evaluateJavascript` calls.

---

## Threading

This is the most important thing to understand about the bridge.

**`@JavascriptInterface` methods are called on a background thread.** This means:

- ✅ You can do network calls, file I/O, database reads directly
- ❌ You **cannot** touch the UI directly — this will crash or silently fail

Wrap any UI operation in `runOnUiThread {}`:

```kotlin
@JavascriptInterface
fun showToast(message: String) {
    // ❌ Wrong — crashes on some devices
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    // ✅ Correct
    (context as? Activity)?.runOnUiThread {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
```

Similarly, `evaluateJavascript` must be called on the main thread:

```kotlin
// ❌ Wrong — evaluateJavascript requires main thread
webView.evaluateJavascript("...", null)

// ✅ Correct
runOnUiThread {
    webView.evaluateJavascript("...", null)
}
```

---

## Java Equivalents

If you're using Java instead of Kotlin, the bridge works identically. The annotations and types are the same; only the syntax changes.

**NativeBridge.java:**

```java
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import android.content.Context;

public class NativeBridge {
    private final Context context;

    public NativeBridge(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void showToast(String message) {
        ((Activity) context).runOnUiThread(() ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }

    @JavascriptInterface
    public String getDeviceInfo() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("model", Build.MODEL);
            obj.put("sdk", Build.VERSION.SDK_INT);
            obj.put("manufacturer", Build.MANUFACTURER);
            return obj.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }
}
```

**MainActivity.java (relevant part):**

```java
webView.addJavascriptInterface(new NativeBridge(this), "Native");
```

---

## Security Note

`addJavascriptInterface` only applies to content loaded from trusted origins. The template loads from `file:///android_asset/` which means only files you bundled in the APK can access the bridge.

**Never** enable `setAllowUniversalAccessFromFileURLs(true)` — this would let any malicious web content you load access your bridge. The template does not set this.

If your app loads remote URLs at any point, be very careful what those pages can access. Consider restricting bridge exposure to only your known origins using a custom `WebViewClient.shouldOverrideUrlLoading`.

See [Gotchas & Tips → Security](gotchas-and-tips.md#security) for more.
