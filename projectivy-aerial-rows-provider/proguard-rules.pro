# Projectivy calls this service through AIDL and Android instantiates both exported components.
-keep class com.kanozseyer.aerialrows.projectivy.WallpaperProviderService { *; }
-keep class com.kanozseyer.aerialrows.projectivy.SettingsActivity { *; }
-keep class tv.projectivy.plugin.wallpaperprovider.api.** { *; }
-keep class * implements android.os.Parcelable { public static final android.os.Parcelable$Creator *; }
