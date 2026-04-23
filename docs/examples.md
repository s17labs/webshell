# Examples

Real-world patterns for common tasks. Each example shows both the Android (Kotlin) and JS sides.

---

## 1. Vibration

**`NativeBridge.kt`**
```kotlin
@JavascriptInterface
fun vibrate(durationMs: Int) {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs.toLong())
    }
}
```

Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

**`bridge.js`**
```javascript
vibrate: (ms = 200) => {
    if (NativeBridge.isNative()) window.Native.vibrate(ms);
},
```

**Usage:**
```javascript
NativeBridge.vibrate(150); // short tap feedback
```

---

## 2. Clipboard

**`NativeBridge.kt`**
```kotlin
@JavascriptInterface
fun copyToClipboard(text: String) {
    (context as? Activity)?.runOnUiThread {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("webshell_clip", text)
        clipboard.setPrimaryClip(clip)
    }
}

@JavascriptInterface
fun readClipboard(): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
}
```

**`bridge.js`**
```javascript
copyToClipboard: (text) => {
    if (NativeBridge.isNative()) {
        window.Native.copyToClipboard(text);
    } else {
        navigator.clipboard?.writeText(text);
    }
},
readClipboard: () => {
    if (NativeBridge.isNative()) return window.Native.readClipboard();
    return '';
},
```

**Usage:**
```javascript
NativeBridge.copyToClipboard('Hello, World!');
```

---

## 3. Share Sheet

Open the native Android share dialog to share text or a URL.

**`NativeBridge.kt`**
```kotlin
@JavascriptInterface
fun share(text: String) {
    (context as? Activity)?.runOnUiThread {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}
```

**`bridge.js`**
```javascript
share: (text) => {
    if (NativeBridge.isNative()) {
        window.Native.share(text);
    } else {
        navigator.share?.({ text }) ?? navigator.clipboard?.writeText(text);
    }
},
```

**Usage:**
```javascript
NativeBridge.share('Check out this cool thing: https://example.com');
```

---

## 4. Open External URL in Browser

Prevent links from loading in your WebView and open them in Chrome instead.

**`NativeBridge.kt`**
```kotlin
@JavascriptInterface
fun openUrl(url: String) {
    (context as? Activity)?.runOnUiThread {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
```

**`bridge.js`**
```javascript
openUrl: (url) => {
    if (NativeBridge.isNative()) {
        window.Native.openUrl(url);
    } else {
        window.open(url, '_blank');
    }
},
```

**Usage:**
```javascript
NativeBridge.openUrl('https://example.com');
```

---

## 5. Reading and Writing Files

Read and write files in the app's private storage.

**`NativeBridge.kt`**
```kotlin
@JavascriptInterface
fun writeFile(filename: String, content: String): Boolean {
    return try {
        File(context.filesDir, filename).writeText(content)
        true
    } catch (e: Exception) {
        false
    }
}

@JavascriptInterface
fun readFile(filename: String): String {
    return try {
        File(context.filesDir, filename).readText()
    } catch (e: Exception) {
        ""
    }
}

@JavascriptInterface
fun deleteFile(filename: String): Boolean {
    return File(context.filesDir, filename).delete()
}
```

**`bridge.js`**
```javascript
writeFile: (filename, content) => {
    if (NativeBridge.isNative()) return window.Native.writeFile(filename, content);
    // Browser fallback: use localStorage
    try { localStorage.setItem('file_' + filename, content); return true; }
    catch { return false; }
},
readFile: (filename) => {
    if (NativeBridge.isNative()) return window.Native.readFile(filename);
    return localStorage.getItem('file_' + filename) ?? '';
},
```

**Usage:**
```javascript
NativeBridge.writeFile('settings.json', JSON.stringify({ theme: 'dark' }));
const settings = JSON.parse(NativeBridge.readFile('settings.json') || '{}');
```

---

## 6. Showing a Native Alert Dialog

**`NativeBridge.kt`**
```kotlin
@JavascriptInterface
fun alert(title: String, message: String, confirmLabel: String = "OK") {
    (context as? Activity)?.runOnUiThread {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(confirmLabel) { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
```

**`bridge.js`**
```javascript
alert: (title, message, confirmLabel = 'OK') => {
    if (NativeBridge.isNative()) {
        window.Native.alert(title, message, confirmLabel);
    } else {
        window.alert(`${title}\n${message}`);
    }
},
```

**Usage:**
```javascript
NativeBridge.alert('Error', 'Something went wrong. Please try again.');
```

---

## 7. Confirmation Dialog with Callback

Dialogs are async on Android — you need to push the result back to JS.

**`NativeBridge.kt`**
```kotlin
@JavascriptInterface
fun confirm(title: String, message: String) {
    (context as? Activity)?.runOnUiThread {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                (context as? MainActivity)?.webView?.post {
                    (context as? MainActivity)?.webView
                        ?.evaluateJavascript("window.__bridge_confirmResult(true)", null)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                (context as? MainActivity)?.webView?.post {
                    (context as? MainActivity)?.webView
                        ?.evaluateJavascript("window.__bridge_confirmResult(false)", null)
                }
            }
            .show()
    }
}
```

**`bridge.js`**
```javascript
confirm: (title, message) => new Promise((resolve) => {
    if (NativeBridge.isNative()) {
        NativeBridge.on('confirmResult', (result) => resolve(result));
        window.Native.confirm(title, message);
    } else {
        resolve(window.confirm(`${title}\n${message}`));
    }
}),
```

**Usage:**
```javascript
const confirmed = await NativeBridge.confirm('Delete item', 'Are you sure?');
if (confirmed) deleteItem();
```

---

## 8. Dark Mode Detection

**`NativeBridge.kt`**
```kotlin
@JavascriptInterface
fun isDarkMode(): Boolean {
    val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return uiMode == Configuration.UI_MODE_NIGHT_YES
}
```

**`bridge.js`**
```javascript
isDarkMode: () => {
    if (NativeBridge.isNative()) return window.Native.isDarkMode();
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
},
```

**Usage:**
```javascript
if (NativeBridge.isDarkMode()) {
    document.body.classList.add('dark');
}
```

---

## 9. App Version Info

**`NativeBridge.kt`**
```kotlin
@JavascriptInterface
fun getAppVersion(): String {
    return try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        JSONObject().apply {
            put("versionName", info.versionName)
            put("versionCode", if (Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            })
        }.toString()
    } catch (e: Exception) {
        "{}"
    }
}
```

**`bridge.js`**
```javascript
getAppVersion: () => {
    if (NativeBridge.isNative()) return JSON.parse(window.Native.getAppVersion());
    return { versionName: 'dev', versionCode: 0 };
},
```

**Usage:**
```javascript
const version = NativeBridge.getAppVersion();
document.getElementById('version').textContent = `v${version.versionName}`;
```

---

## 10. Handling Orientation Changes

When the device rotates, Android recreates the Activity by default, which reloads the WebView and loses JS state.

**Option A: Lock orientation** (simplest)

In `AndroidManifest.xml`:
```xml
<activity
    android:screenOrientation="portrait"
    ...>
```

**Option B: Handle config changes yourself** (preserves state)

In `AndroidManifest.xml`:
```xml
<activity
    android:configChanges="orientation|screenSize|keyboardHidden"
    ...>
```

And in `MainActivity.kt`:
```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // Notify JS of orientation change
    val orientation = if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
    webView.post {
        webView.evaluateJavascript("window.__bridge_orientationChange('${orientation}')", null)
    }
}
```

```javascript
NativeBridge.on('orientationChange', (orientation) => {
    console.log('Now:', orientation); // 'portrait' or 'landscape'
});
```
