rootProject.name = "kaliberkatalog"

// Plain Kotlin without Android, a build of its own, so its tests also run where the Android SDK can't be had.
includeBuild("catalog")
