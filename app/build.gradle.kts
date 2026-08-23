plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    // ── Change these to match your app ────────────────
    namespace = "com.yourapp"
    // ──────────────────────────────────────────────────

    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // ── Change these to match your app ────────────
        applicationId = "com.yourapp"
        versionCode = 2
        versionName = "1.0.2"
        // ──────────────────────────────────────────────

        minSdk = 26
        targetSdk = 35
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    // WebView is part of the Android SDK — no extra dependency needed
}
