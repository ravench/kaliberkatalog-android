# Notes for Claude

- Follow [CONTRIBUTING.md](CONTRIBUTING.md): small pull requests, one per group of related
  changes, each with unit tests that CI runs.
- The cloud container can't reach dl.google.com, so it has neither the Android SDK nor the Android
  Gradle plugin: run the `catalog/` tests there, and let GitHub Actions build the app. Its proxy
  also answers 429 for repo.maven.apache.org at times; a local init script that puts
  `https://repo1.maven.org/maven2` first gets around it (don't commit that).
- The catalogue logic is ported from Kaliberfinder (iOS): `CartridgeCatalog.swift`, `Cartridge.swift`
  and their tests. Keep names and behaviour close to the Swift code so changes carry over.
