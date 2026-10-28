plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.motionmouse"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.motionmouse"
        minSdk = 28            // BluetoothHidDevice needs API 28 (Android 9)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// No third-party libraries needed.
