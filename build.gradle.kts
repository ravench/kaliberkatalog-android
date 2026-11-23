plugins {
    alias(libs.plugins.android.application) apply false
    // The Kotlin that AGP's built-in Kotlin support compiles with: the Compose compiler must be the same version.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
