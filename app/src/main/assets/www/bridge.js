/**
 * WebShell — bridge.js
 *
 * The JavaScript-side wrapper for the Android native bridge.
 *
 * Include this script BEFORE your own app scripts:
 *   <script src="bridge.js"></script>
 *
 * Then use NativeBridge.* anywhere in your app:
 *   NativeBridge.toast('Hello!')
 *   NativeBridge.vibrate(200)
 *   const info = NativeBridge.getDeviceInfo()
 *
 * ─────────────────────────────────────────────────────────────
 * How it works
 * ─────────────────────────────────────────────────────────────
 * When running inside the Android WebView, window.Native is
 * injected by NativeBridge.kt via addJavascriptInterface.
 *
 * When running in a desktop browser (for development), window.Native
 * is undefined. Every method in this file checks isNative() first
 * and falls back gracefully so your app still works in a browser.
 *
 * To push events FROM Android TO JavaScript, Android calls:
 *   webView.evaluateJavascript("window.__bridge_eventName(payload)", null)
 *
 * Register listeners with NativeBridge.on():
 *   NativeBridge.on('myEvent', (data) => { ... })
 * ─────────────────────────────────────────────────────────────
 */

const NativeBridge = (() => {

    // ─────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────

    /**
     * Returns true if running inside the Android WebView with the
     * native bridge available.
     */
    function isNative() {
        return typeof window.Native !== 'undefined';
    }

    /**
     * Register a listener for an event pushed from Android.
     *
     * Android fires events by calling:
     *   webView.evaluateJavascript("window.__bridge_<event>(<payload>)", null)
     *
     * @param {string} event - Event name (matches the __bridge_ suffix Android calls)
     * @param {function} callback - Called with the parsed payload
     */
    function on(event, callback) {
        window[`__bridge_${event}`] = callback;
    }

    // ─────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────

    /**
     * Show a native toast notification.
     * @param {string} message
     */
    function toast(message) {
        if (isNative()) {
            window.Native.showToast(String(message));
        } else {
            console.log('[NativeBridge.toast]', message);
        }
    }

    // ─────────────────────────────────────────────────
    // Device Info
    // ─────────────────────────────────────────────────

    /**
     * Get basic device information.
     * @returns {{ model: string, manufacturer: string, sdk: number, appVersion: string }}
     */
    function getDeviceInfo() {
        if (isNative()) {
            try {
                return JSON.parse(window.Native.getDeviceInfo());
            } catch {
                return {};
            }
        }
        return {
            model: 'browser',
            manufacturer: 'web',
            sdk: 0,
            appVersion: 'dev'
        };
    }

    // ─────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────

    /**
     * Open a URL in the system browser (not in the WebView).
     * @param {string} url
     */
    function openUrl(url) {
        if (isNative()) {
            window.Native.openUrl(url);
        } else {
            window.open(url, '_blank');
        }
    }

    // ─────────────────────────────────────────────────
    // Haptics
    // ─────────────────────────────────────────────────

    /**
     * Vibrate the device.
     * @param {number} [durationMs=200]
     */
    function vibrate(durationMs = 200) {
        if (isNative()) {
            window.Native.vibrate(durationMs);
        } else {
            navigator.vibrate?.(durationMs);
        }
    }

    // ─────────────────────────────────────────────────
    // Sharing
    // ─────────────────────────────────────────────────

    /**
     * Open the native share sheet.
     * @param {string} text - Text or URL to share
     */
    function share(text) {
        if (isNative()) {
            window.Native.share(text);
        } else {
            if (navigator.share) {
                navigator.share({ text });
            } else {
                navigator.clipboard?.writeText(text);
                console.log('[NativeBridge.share] Copied to clipboard:', text);
            }
        }
    }

    // ─────────────────────────────────────────────────
    // Generic Event Bus (JS → Native)
    // ─────────────────────────────────────────────────

    /**
     * Emit a named event with a JSON payload to Android.
     * Handle it in NativeBridge.kt's emit() method.
     *
     * @param {string} event
     * @param {object} [payload={}]
     */
    function emit(event, payload = {}) {
        if (isNative()) {
            window.Native.emit(event, JSON.stringify(payload));
        } else {
            console.log('[NativeBridge.emit]', event, payload);
        }
    }

    // ─────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────

    return {
        isNative,
        on,
        toast,
        getDeviceInfo,
        openUrl,
        vibrate,
        share,
        emit,
    };

})();
