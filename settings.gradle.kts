pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "CouchyLauncher"
include(":app")
include(":macrobenchmark")
include(":projectivy-aerial-rows-provider")
include(":projectivy-kodi-channel-bridge")
