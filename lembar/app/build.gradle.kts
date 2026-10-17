plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "id.my.timecraft.lembar"
    compileSdk = 36

    base {
        archivesName = "Lembar"
    }

    buildFeatures {
        compose = true
    }

    defaultConfig {
        // Memakai domain milik sendiri (timecraft.my.id) supaya tidak bentrok di Play Store.
        // Tidak bisa diubah lagi setelah upload pertama ke Play Console.
        applicationId = "id.my.timecraft.lembar"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    // Debug keystore tetap di repo supaya APK debug dari CI bisa menimpa versi sebelumnya
    // di HP tanpa uninstall (alasan yang sama dengan android/app/debug.keystore).
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    // Sama dengan android/: BOM 2026.04.01 cocok dengan compileSdk 36 / AGP 9.0.1.
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
}
