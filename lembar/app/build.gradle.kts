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
        // Naikkan versionCode setiap kali mengunggah build baru ke Play Console
        // (Play menolak versionCode yang sudah pernah diunggah).
        versionCode = 1
        versionName = "1.0.0"
    }

    // Debug keystore tetap di repo supaya APK debug dari CI bisa menimpa versi sebelumnya
    // di HP tanpa uninstall (alasan yang sama dengan android/app/debug.keystore).
    //
    // Upload key Google Play TIDAK disimpan di repo. Di CI, keystore-nya didekode dari secret
    // LEMBAR_UPLOAD_KEYSTORE_BASE64 ke file sementara dan kredensialnya dibaca dari env var di
    // bawah (lihat .github/workflows/lembar-build.yml dan lembar/PLAY_STORE.md). Tanpa env var
    // itu, build rilis jatuh ke debug key: tetap bisa dicek, tapi tidak bisa diunggah ke Play.
    val uploadKeystorePath = System.getenv("LEMBAR_UPLOAD_KEYSTORE_PATH")

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (uploadKeystorePath != null) {
            create("upload") {
                storeFile = file(uploadKeystorePath)
                storePassword = System.getenv("LEMBAR_UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LEMBAR_UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("LEMBAR_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
            // R8: buang kode & resource yang tidak terpakai supaya ukuran unduhan di Play kecil.
            // Aplikasi ini tidak memakai refleksi, jadi aturan bawaan sudah cukup.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
