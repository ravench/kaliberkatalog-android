plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ch.giotta.kaliberkatalog"
    // Compose 1.13 needs 37.1 or later. Compiling against it changes no behaviour; targetSdk does that.
    compileSdk { version = release(37) { minorApiLevel = 2 } }

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

    buildTypes {
        getByName("release") {
            // Signed with the upload key where UPLOAD_KEYSTORE names one: release.yml's from the repository's secrets, or
            // build.yml's made for its run. Google Play signs the app itself (Play App Signing); elsewhere the bundle
            // is unsigned.
            providers.environmentVariable("UPLOAD_KEYSTORE").orNull?.let { keystore ->
                signingConfig = signingConfigs.create("upload") {
                    storeFile = file(keystore)
                    storePassword = providers.environmentVariable("UPLOAD_KEYSTORE_PASSWORD").get()
                    keyAlias = "upload"
                    keyPassword = storePassword // a PKCS12 keystore, keytool's kind, has one password
                }
            }
        }
    }

    buildFeatures { compose = true }

    androidResources {
        // The app's languages in the system settings (Android 13 and newer), from the values-* folders.
        generateLocaleConfig = true
        // The libraries' texts in these languages only, as tools/strings.py writes them: Android picks the first of
        // the phone's languages that the app has, so Dutch, then German gets German rather than English and Dutch.
        localeFilters += listOf("en", "de", "fr", "it", "es")
    }

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
