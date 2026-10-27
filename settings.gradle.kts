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
include(":embedded-runtime")
// Build marker: v9 Red Light native channel artwork.
