# Getting Started

This guide walks you through setting up WebShell from scratch and shipping your first web-powered Android app.

---

## Prerequisites

Before you begin, make sure you have:

- **Android Studio** (Hedgehog 2023.1.1 or newer)
- **Android SDK** with API 26–35 installed
- A basic understanding of how Android projects are structured
- Your web app (HTML/CSS/JS), or you can use the starter files

---

## Step 1 — Get the Template

### Option A: Clone the repository

```bash
git clone https://github.com/s17labs/webshell.git MyApp
cd MyApp
```

### Option B: Download and copy

Download the ZIP, extract it, and rename the `template/` folder to your app name.

### Option C: Manual setup

If you want to drop this into an existing Android project:

1. Copy `NativeBridge.kt` and `MainActivity.kt` into your project's source directory
2. Copy the `assets/www/` folder into `app/src/main/`
3. Apply the `AndroidManifest.xml` and `build.gradle.kts` changes from Step 3 below

---

## Step 2 — Open in Android Studio

1. Open Android Studio
2. Choose **Open** (not "New Project")
3. Navigate to the `template/` folder and open it
4. Wait for Gradle sync to complete

If you see "Gradle sync failed", check that you have SDK API 35 installed via **SDK Manager → SDK Platforms**.

---

## Step 3 — Configure Your App Identity

These are the only **required** changes before your app is yours.

### `app/build.gradle.kts`

```kotlin
android {
    namespace = "com.yourcompany.yourapp"   // ← change this

    defaultConfig {
        applicationId = "com.yourcompany.yourapp"  // ← change this
        minSdk = 26
        targetSdk = 35
        versionCode = 1           // ← increment on each release
        versionName = "1.0.0"    // ← your version string
    }
}
```

> **Note on `namespace` vs `applicationId`**: `namespace` controls the R class and generated code. `applicationId` is what gets published to the Play Store. They're usually the same, but they don't have to be.

### `app/src/main/AndroidManifest.xml`

```xml
<application
    android:label="Your App Name"     <!-- ← change this -->
    android:icon="@mipmap/ic_launcher"
    ...>
```

To set a custom icon, replace the files in `res/mipmap-*/`. Android Studio's **Image Asset Studio** (right-click `res` → New → Image Asset) is the easiest way to generate all sizes.

---

## Step 4 — Add Your Web App

Your web app lives in:

```
app/src/main/assets/www/
```

This folder is bundled into the APK and served locally — no server needed.

### Minimal setup

At minimum you need:

```
assets/www/
├── index.html      ← entry point (always loaded first)
└── bridge.js       ← required for native bridge (don't remove)
```

### Replace the starter files

The template includes a working starter app in `assets/www/`. Delete or replace these files with your own:

- `app.js` → your application logic
- `style.css` → your styles
- `index.html` → your markup

Keep `bridge.js` — it's the JS-side of the native bridge. Just include it in your HTML:

```html
<script src="bridge.js"></script>
```

Include it **before** your own scripts.

### Subdirectories are fine

You can organise your web app however you want:

```
assets/www/
├── index.html
├── bridge.js
├── css/
│   └── main.css
├── js/
│   ├── app.js
│   └── utils.js
└── fonts/
    └── MyFont.woff2
```

Reference them with relative paths as you normally would in HTML.

---

## Step 5 — Run

1. Connect an Android device (API 26+) or launch an emulator
2. Click **Run** (▶) in Android Studio
3. Your app should open full-screen showing your `index.html`

---

## Step 6 — Verify the Bridge

Open `index.html` in a browser too. The bridge should gracefully no-op — you won't see errors because `bridge.js` checks for `window.Native` before calling it.

To test on device, add this to your `app.js`:

```javascript
NativeBridge.toast('Bridge works!');
```

You should see a native Android toast notification.

---

## Next Steps

- [Project Structure](project-structure.md) — understand what every file does
- [Bridge API](bridge-api.md) — add more native capabilities
- [Gotchas & Tips](gotchas-and-tips.md) — read this before shipping

---

## Using Java Instead of Kotlin

The template uses Kotlin by default. To use Java instead:

1. Rename `MainActivity.kt` → `MainActivity.java`
2. Rename `NativeBridge.kt` → `NativeBridge.java`
3. Convert the syntax (see Java equivalents in [Bridge API](bridge-api.md))
4. In `build.gradle.kts`, you can remove the Kotlin plugin if you want a pure Java project — though it's fine to leave it

The bridge API and AndroidManifest are identical either way.
