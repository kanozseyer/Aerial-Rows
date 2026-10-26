plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kanozseyer.aerialrows.kodibridge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kanozseyer.aerialrows.kodibridge"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        base.archivesName = "Aerial-Rows-Kodi-Bridge-$versionName"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.tvprovider:tvprovider:1.0.0")
}
