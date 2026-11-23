pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kaliberkatalog"

// Plain Kotlin without Android, a build of its own, so its tests also run where the Android SDK can't be had
// (`./gradlew -p catalog test`).
includeBuild("catalog")
include(":app")
