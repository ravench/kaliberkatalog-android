# Contributing

Kaliberkatalog is closed source ([LICENSE.md](LICENSE.md)). Only people Simon Giotta invites work on
it, under a written agreement with Simon Giotta. These notes are for them.

## Guidelines

- Calibre data comes from the shared [calibre-data](https://github.com/ravench/calibre-data)
  repository, a git submodule at `calibre-data/` (run `git submodule update --init` after
  cloning). Fix dimensions there, then move the submodule here and run
  `python3 tools/generate_data.py` and `python3 tools/app_icon.py`, whose launcher icon draws three
  of the outlines; commit the result. The generator is Kaliberfinder's with only the output path
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
