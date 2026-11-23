# Contributing

Kaliberkatalog is closed source ([LICENSE.md](LICENSE.md)). Only people Simon Giotta invites work on
it, under a written agreement with Simon Giotta. These notes are for them.

## Guidelines

- Calibre data comes from the shared [calibre-data](https://github.com/ravench/calibre-data)
  repository, a git submodule at `calibre-data/` (run `git submodule update --init` after
  cloning). Fix dimensions there, then move the submodule here and run
  `python3 tools/generate_data.py`; commit the result. The generator is Kaliberfinder's with only
  the output path changed, so both apps read the same JSON.
- Binaries, the Gradle wrapper's jar too, are in Git LFS: install git-lfs before cloning.
- `catalog/` is plain Kotlin without Android, a Gradle build of its own: `./gradlew -p catalog test`
  runs its tests anywhere, also where the Android SDK can't be installed.
- `app/` is the Android app (Jetpack Compose). `./gradlew :app:testDebugUnitTest` runs its tests on
  the JVM with Robolectric, the screen tour for the pull request screenshots included.
- Texts: German in `tools/translations.py` with English, French, Italian and Spanish; then run
  `python3 tools/strings.py`, which writes the `strings.xml` files (English is the default for
  other languages) and commit them. German is Swiss: ss, «», decimal point, «du».
- Every change comes with unit tests; CI runs them on every push.
- Keep pull requests small and focused, and describe what you changed and how you tested it.

## Releases

Google Play signs the app with its own key (Play App Signing). The bundle uploaded to it is signed
with an upload key, which only the repository's secrets hold.

- Once: make the upload key, for example on a Mac with Android Studio, and put it into the secrets.
  keytool asks for a password, then for the name on the certificate. Keep the file and its password
  safe; a lost upload key can be reset from the Play Console (App integrity).

  ```sh
  "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -genkeypair \
    -keystore ~/kaliberkatalog-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
  base64 -i ~/kaliberkatalog-upload.jks | gh secret set UPLOAD_KEYSTORE_BASE64 -R ravench/kaliberkatalog-android
  gh secret set UPLOAD_KEYSTORE_PASSWORD -R ravench/kaliberkatalog-android
  ```

- Each release: raise `versionCode` (Google Play takes each number once) and `versionName` in
  `app/build.gradle.kts` in a pull request. Then run Actions, Release, Run workflow on main; the
  run's `app-release` artifact is the signed bundle to upload.
