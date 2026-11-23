# Notes for Claude

- Follow [CONTRIBUTING.md](CONTRIBUTING.md): small pull requests, one per group of related
  changes, each with unit tests that CI runs.
- **Pull requests that change the GUI must show screenshots.** The *PR screens* steps of the app job
  (`.github/workflows/build.yml`) take them for you: they run the screen tour in
  `app/src/test/kotlin/ch/giotta/kaliberkatalog/PRScreensTest.kt` (Robolectric and Roborazzi) on the
  pull request and on its base branch and comment with the screens that look different, before and
  after. After pushing, check that the comment shows your change. If it says «No visible change» but
  your change is visible, the tour does not reach that screen: add a stop for it in the same pull
  request.
- The cloud container can't reach dl.google.com, so it has neither the Android SDK nor the Android
  Gradle plugin: run the `catalog/` tests there on their own (`gradle -p catalog test` with the
  container's Gradle, since the wrapper's download is blocked too), and let GitHub Actions build
  the app. Its proxy also answers 429 for repo.maven.apache.org at times; a local init script that
  puts `https://repo1.maven.org/maven2` first gets around it (don't commit that).
- The catalogue logic is ported from Kaliberfinder (iOS): `CartridgeCatalog.swift`, `Cartridge.swift`
  and their tests. Keep names and behaviour close to the Swift code so changes carry over.
