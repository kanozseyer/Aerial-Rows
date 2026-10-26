plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
}

android {
    namespace = "com.kanozseyer.aerialrows.projectivy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kanozseyer.aerialrows.wallpaper"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        base.archivesName = "Aerial-Rows-Projectivy-$versionName"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        aidl = true
        buildConfig = true
    }

    buildTypes {
        debug {
            versionNameSuffix = "-dev"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.annotation:annotation:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    // 1.2.0 requires compileSdk 35 / AGP 8.6. This provider only uses the
    // long-stable GuidedStep APIs, so keep it compatible with the host repo's
    // compileSdk 34 / AGP 8.5.2 toolchain.
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    testImplementation("junit:junit:4.13.2")
}
