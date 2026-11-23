plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ch.giotta.kaliberkatalog"
    compileSdk = 36

    defaultConfig {
        applicationId = "ch.giotta.kaliberkatalog"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // The catalog build's bytecode.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true // the catalogue in the assets
            // PRScreensTest writes its screens to app/build/screens (the PR screens in build.yml).
            all { it.systemProperty("roborazzi.test.record", "true") }
        }
    }
}

dependencies {
    implementation("ch.giotta.kaliberkatalog:catalog") // the included build catalog/
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    debugImplementation(libs.compose.ui.test.manifest) // the empty activity of the Compose tests
}
