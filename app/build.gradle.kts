plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.splitapk1"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.splitapk1"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dynamicFeatures += ":feature_browser"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.mozilla.geckoview)

    implementation(libs.mozilla.support.remotesettings) {
        exclude(group = "org.mozilla.telemetry", module = "glean-native")
    }
    implementation(libs.mozilla.appservices.remotesettings) {
        exclude(group = "org.mozilla.telemetry", module = "glean-native")
    }
}
