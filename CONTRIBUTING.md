# Contributing

Kaliberkatalog is closed source ([LICENSE.md](LICENSE.md)). Only people Simon Giotta invites work on
it, under a written agreement with Simon Giotta. These notes are for them.

## Guidelines

- Calibre data comes from the shared [calibre-data](https://github.com/ravench/calibre-data)
  repository, a git submodule at `calibre-data/` (run `git submodule update --init` after
  cloning). Fix dimensions there, then move the submodule here and run
  `python3 tools/generate_data.py` and `python3 tools/app_icon.py`, whose launcher icon draws the
  GP 11's outline; commit the result. The data generator is Kaliberfinder's with only the output path
  changed, so both apps read the same JSON.
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
  `app/build.gradle.kts`, and write what changed for the app's users in
  `store/<language>/release_notes.txt`, in a pull request. Then run Actions, Release, Run workflow
  on main; the run's `app-release` artifact is the signed bundle to upload. In the Play Console's
  release, each language's notes go between its tags, `en` in both `<en-GB>` and `<en-US>`.
- The store listing's texts are in `store/<language>/`, a file per field of Google Play's listing:
  the title, the short and the full description, and the feature graphic's alt text; beside them
  the release notes (`tools/test_store_listing.py` checks the limits of all of them).
  German is the default language; `en` goes to en-US and en-GB, the others to fr-FR, it-IT and es-ES.
- The listing's phone screenshots, icon and feature graphic: run Actions, Store screens, Run
  workflow on main. The run's `store-screens` artifact has the screenshots in a folder per language,
  numbered in the listing's order (`StoreScreensTest`, 1080 x 1920 px), and beside them `icon.png`
  (512 x 512 px) and `feature-graphic.png` (1024 x 500 px), which serve every language.
- Once, to write the listing through the Google Play Developer API: a service account that may
  edit it, with its key in the secrets.
  1. Google Cloud console, in any project: enable the «Google Play Android Developer API», create a
     service account (it needs no roles) and a JSON key for it, which downloads.
  2. Play Console, Users and permissions: invite the service account's email address, with the
     app's permission to manage the store presence. Google can take up to a day to let a newly
     invited account in; until then the API answers 401 or 403.
  3. Put the key into the secrets, then delete the file:

     ```sh
     gh secret set PLAY_SERVICE_ACCOUNT_JSON -R ravench/kaliberkatalog-android < key.json
     ```

- To put the texts and images on Google Play: run Store screens on main with «Google Play listing»
  set to «dry run», which lists what differs, then to «upload», which writes the texts of `store/`
  and the run's images in one edit (`tools/play_listing.py`). The API finds the app by its package
  name but can't create it: create it in the Play Console first; it needs no uploaded bundle. The API
  has no field for the feature graphic's alt text, the App content declarations, the category or the
  countries: set those in the Play Console. If it holds changes not yet sent for review, the upload
  joins them (Publishing overview). While changes are in review, the upload stops with Google's
  error rather than cancel that review.
