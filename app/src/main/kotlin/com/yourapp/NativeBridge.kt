package com.yourapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import org.json.JSONObject

/**
 * WebShell — NativeBridge
 *
 * Every public method annotated with @JavascriptInterface is callable
 * from JavaScript as:
 *
 *   window.Native.methodName(args)
 *
 * Or via the bridge.js wrapper:
 *
 *   NativeBridge.methodName(args)
 *
 * ─────────────────────────────────────────────────────
 * IMPORTANT: Threading
 * ─────────────────────────────────────────────────────
 * @JavascriptInterface methods are called on a background thread.
 * Any UI operation (Toast, Dialog, view changes) MUST be wrapped in
 * activity.runOnUiThread { ... }
 *
 * Similarly, webView.evaluateJavascript() must run on the main thread:
 *   activity.webView.post { activity.webView.evaluateJavascript(...) }
 *
 * See docs/bridge-api.md and docs/gotchas-and-tips.md for full details.
 * ─────────────────────────────────────────────────────
 *
 * Adding new bridge methods:
 * 1. Add an @JavascriptInterface method here
 * 2. Add a corresponding wrapper in assets/www/bridge.js
 * 3. Call it in your web app via NativeBridge.yourMethod()
 */
class NativeBridge(private val activity: MainActivity) {

    private val context: Context get() = activity

    // ─────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────

    /**
     * Show a short native toast notification.
     * JS: NativeBridge.toast("Message")
     */
    @JavascriptInterface
    fun showToast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────────────
    // Device Info
    // ─────────────────────────────────────────────────────

    /**
     * Get basic device information.
     * Returns a JSON string: { model, manufacturer, sdk, appVersion }
     * JS: const info = NativeBridge.getDeviceInfo()
     */
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            JSONObject().apply {
                put("model", Build.MODEL)
                put("manufacturer", Build.MANUFACTURER)
                put("sdk", Build.VERSION.SDK_INT)
                put("appVersion", packageInfo.versionName)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("model", Build.MODEL)
                put("manufacturer", Build.MANUFACTURER)
                put("sdk", Build.VERSION.SDK_INT)
                put("appVersion", "unknown")
            }.toString()
        }
    }

    // ─────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────

    /**
     * Open a URL in the system browser.
     * JS: NativeBridge.openUrl("https://example.com")
     */
    @JavascriptInterface
    fun openUrl(url: String) {
        activity.runOnUiThread {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    // ─────────────────────────────────────────────────────
    // Haptics
    // ─────────────────────────────────────────────────────

    /**
     * Trigger a vibration of the given duration.
     * Requires: <uses-permission android:name="android.permission.VIBRATE" />
     * JS: NativeBridge.vibrate(200)
     */
    @JavascriptInterface
    fun vibrate(durationMs: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        // VibrationEffect is available from API 26, which matches our minSdk — no else branch needed
        vibrator.vibrate(
            VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    // ─────────────────────────────────────────────────────
    // Sharing
    // ─────────────────────────────────────────────────────

    /**
     * Open the native share sheet with the given text.
     * JS: NativeBridge.share("Check this out: https://example.com")
     */
    @JavascriptInterface
    fun share(text: String) {
        activity.runOnUiThread {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, null))
        }
    }

    // ─────────────────────────────────────────────────────
    // Generic Event Bus (JS → Native)
    // ─────────────────────────────────────────────────────

    /**
     * Generic event emitter from JS to native.
     * JS: NativeBridge.emit("eventName", { key: "value" })
     *
     * Add your event handlers in the when() block below.
     */
    @JavascriptInterface
    fun emit(event: String, payload: String) {
        try {
            val data = JSONObject(payload)
            when (event) {
                // Add your custom events here:
                // "myEvent" -> handleMyEvent(data)
            }
        } catch (e: Exception) {
            // Malformed payload — ignore or log
        }
    }
}
