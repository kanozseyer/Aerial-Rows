plugins {
    id("com.android.application")
}

android {
    namespace = "com.spocky.projengmenu.embedded"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.spocky.projengmenu.embedded.build"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
        debug { isMinifyEnabled = false }
    }
}

dependencies {
    // Projectivy already ships AndroidX Preference. Compile against its public API
    // without packaging another copy into the injected runtime DEX.
    compileOnly("androidx.preference:preference:1.2.1")
}
