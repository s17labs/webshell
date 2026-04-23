# WebShell ProGuard rules

# ── Keep the NativeBridge intact ──────────────────────────────────────────────
# ProGuard will rename or remove methods it thinks are unused.
# Since NativeBridge methods are called by name from JavaScript, they MUST be kept.
# If you add new @JavascriptInterface methods, they're covered by this rule.
-keepclassmembers class com.yourapp.NativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep the class itself
-keep class com.yourapp.NativeBridge { *; }

# ── Standard Android rules ────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep BuildConfig for debug checks
-keep class com.yourapp.BuildConfig { *; }
