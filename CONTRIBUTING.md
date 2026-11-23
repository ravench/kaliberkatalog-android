# Contributing

Kaliberkatalog is closed source ([LICENSE.md](LICENSE.md)). Only people Simon Giotta invites work on
it, under a written agreement with Simon Giotta. These notes are for them.

## Guidelines

- Calibre data comes from the shared [calibre-data](https://github.com/ravench/calibre-data)
  repository, a git submodule at `calibre-data/` (run `git submodule update --init` after
  cloning). Fix dimensions there, then move the submodule here and run
  `python3 tools/generate_data.py`; commit the result. The generator is Kaliberfinder's with only
  the output path changed, so both apps read the same JSON.
- `catalog/` is plain Kotlin without Android, a Gradle build of its own: `./gradlew :catalog:test`
  runs its tests anywhere, also where the Android SDK can't be installed.
- Every change comes with unit tests; CI runs them on every push.
- Keep pull requests small and focused, and describe what you changed and how you tested it.
