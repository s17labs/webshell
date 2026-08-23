package com.yourapp

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

/**
 * WebShell — MainActivity
 *
 * The single Activity that hosts your web app in a full-screen WebView.
 *
 * You generally don't need to modify this file. The two files you'll
 * actually work with are:
 *   - assets/www/   → your HTML/CSS/JS app
 *   - NativeBridge.kt → native API methods callable from JS
 *
 * For configuration options, see:
 *   - docs/project-structure.md
 *   - docs/gotchas-and-tips.md
 */
class MainActivity : AppCompatActivity() {

    // Exposed so NativeBridge can call evaluateJavascript on it.
    // Used when native code needs to push data/events back to JS.
    internal lateinit var webView: WebView
    private lateinit var bridge: NativeBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw behind system bars for edge-to-edge display.
        // In your CSS, use env(safe-area-inset-*) to add padding.
        // Remove this line if you prefer the default system bar behaviour.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        bridge = NativeBridge(this)

        webView = WebView(this).apply {
            settings.apply {
                // Required: your web app won't work without this
                javaScriptEnabled = true

                // Enables localStorage and sessionStorage
                domStorageEnabled = true

                // Enables loading files from assets/www/
                // (file:///android_asset works regardless; this only governs
                //  the broader file system, so keep it off for security)
                allowFileAccess = false

                // Optional: disable zoom controls for a more app-like feel
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false

                // Optional: set a custom user agent
                // userAgentString = "$userAgentString WebShell/1.0"
            }

            // Attach the native bridge.
            // Accessible in JS as window.Native.*
            // Add your methods in NativeBridge.kt.
            addJavascriptInterface(bridge, "Native")

            webViewClient = object : WebViewClient() {
                // Return false to let the WebView handle the navigation.
                // Return true to cancel/block the navigation.
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val url = request.url
                    val scheme = url.scheme ?: return true // block if no scheme

                    // Only allow local asset navigation inside the WebView
                    if (scheme == "file") return false

                    // For safe external schemes, open in the system browser
                    if (scheme == "https" || scheme == "http" || scheme == "mailto" || scheme == "tel") {
                        bridge.openUrl(url.toString())
                    }
                    // All other schemes (intent://, javascript:, market://, etc.) are silently blocked
                    return true
                }
            }

            // Match your app's background to avoid white flash on load.
            // Change this to match your index.html's background color
            // (the starter theme uses #0F0F0F).
            setBackgroundColor(Color.parseColor("#0F0F0F"))

            // Enable Chrome DevTools inspection in debug builds.
            // NEVER ship with this enabled in production.
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            // Load the entry point of your web app.
            // To change the entry file, update the URL below.
            loadUrl("file:///android_asset/www/index.html")
        }

        setContentView(webView)
        setupBackNavigation()
    }

    /**
     * Handle the hardware/gesture back button.
     * Navigates back in WebView history if possible, otherwise exits the app.
     *
     * If your JS app handles all navigation internally (no actual page loads),
     * you may want to fire a JS event here instead of calling webView.goBack().
     * See docs/gotchas-and-tips.md for details.
     */
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}
