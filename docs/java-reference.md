# Java Reference

If you prefer Java over Kotlin, use these files in place of the Kotlin versions.
Everything else in the template (manifest, Gradle, assets) is identical.

---

## `MainActivity.java`

```java
package com.yourapp;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

public class MainActivity extends AppCompatActivity {

    // Exposed so NativeBridge can call evaluateJavascript on it
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        webView = new WebView(this);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);

        webView.addJavascriptInterface(new NativeBridge(this), "Native");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!url.startsWith("file://")) {
                    new NativeBridge(MainActivity.this).openUrl(url);
                    return true;
                }
                return false;
            }
        });

        webView.setBackgroundColor(Color.parseColor("#FFFFFF"));

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.loadUrl("file:///android_asset/www/index.html");

        setContentView(webView);
        setupBackNavigation();
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
}
```

---

## `NativeBridge.java`

```java
package com.yourapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class NativeBridge {

    private final MainActivity activity;

    public NativeBridge(MainActivity activity) {
        this.activity = activity;
    }

    private Context getContext() {
        return activity;
    }

    // ── UI ────────────────────────────────────────

    @JavascriptInterface
    public void showToast(String message) {
        activity.runOnUiThread(() ->
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show()
        );
    }

    // ── Device Info ───────────────────────────────

    @JavascriptInterface
    public String getDeviceInfo() {
        try {
            android.content.pm.PackageInfo info =
                getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            JSONObject obj = new JSONObject();
            obj.put("model", Build.MODEL);
            obj.put("manufacturer", Build.MANUFACTURER);
            obj.put("sdk", Build.VERSION.SDK_INT);
            obj.put("appVersion", info.versionName);
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    // ── Navigation ────────────────────────────────

    @JavascriptInterface
    public void openUrl(String url) {
        activity.runOnUiThread(() -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            getContext().startActivity(intent);
        });
    }

    // ── Haptics ───────────────────────────────────

    @JavascriptInterface
    public void vibrate(int durationMs) {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager manager = (VibratorManager)
                getContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(durationMs);
        }
    }

    // ── Sharing ───────────────────────────────────

    @JavascriptInterface
    public void share(String text) {
        activity.runOnUiThread(() -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            getContext().startActivity(Intent.createChooser(intent, null));
        });
    }

    // ── Generic Event Bus ─────────────────────────

    @JavascriptInterface
    public void emit(String event, String payload) {
        try {
            JSONObject data = new JSONObject(payload);
            switch (event) {
                // Add your custom events here:
                // case "myEvent": handleMyEvent(data); break;
            }
        } catch (JSONException e) {
            // Malformed payload — ignore
        }
    }
}
```

---

## Switching from Kotlin to Java in `build.gradle.kts`

If you want a **pure Java project** (no Kotlin at all), remove the Kotlin plugin:

```kotlin
// build.gradle.kts — remove this line:
alias(libs.plugins.kotlin.android)

// And remove from root build.gradle.kts:
alias(libs.plugins.kotlin.android) apply false
```

And remove from `libs.versions.toml`:
```toml
# Remove these:
kotlin = "2.0.21"
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

You can also remove the `kotlinOptions` block from `app/build.gradle.kts`.

If you're keeping the Kotlin plugin (because you might mix Kotlin and Java), no changes needed — Kotlin and Java interop seamlessly in the same project.
