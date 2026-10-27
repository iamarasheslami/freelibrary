# Releasing

How a version of FreeLibrary is built and published. Only the maintainer holds the signing key,
so releases are built on the maintainer's machine (see "Release builds" in
[CONTRIBUTING.md](CONTRIBUTING.md) for the key setup).

## Versioning

- `versionName` follows Semantic Versioning (`MAJOR.MINOR.PATCH`).
- `versionCode` is a whole number that increases by at least one with every published build.
  Android uses it to decide whether an APK is an update, so it must never go down.
- The git tag for a release is `v<versionName>`, for example `v0.1.0`. Tags named
  `catalog-<date>` belong to the bundled catalog database and are unrelated.

## Checklist

1. Start from a clean, current `main`:
   `git checkout main`, `git pull`, `git status` (nothing to commit).
2. Check that the catalog pinned in `app/build.gradle.kts` (`releaseTag` and `expectedSha256` in
   `fetchCatalogDatabase`) is the one you intend to ship.
3. In one pull request titled `chore: prepare release <versionName>`:
   - raise `versionName` and `versionCode` in `app/build.gradle.kts`;
   - move the `Unreleased` entries in `CHANGELOG.md` under the new version and its date;
   - add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` with a short, plain
     description of what changed (store listings read this file, 500 characters at most).
4. After the pull request is merged, update `main` and run, one at a time:
   `./gradlew clean`, `./gradlew ktlintCheck`, `./gradlew testDebugUnitTest`,
   `./gradlew lintDebug`, `./gradlew assembleRelease`.
5. Check the signed APK with `apksigner verify --verbose --print-certs <apk>` (from the Android
   SDK `build-tools` folder). The certificate must be the release key, not the debug key.
6. Install the APK on a real phone over the previous release if one exists (an update, not only
   a fresh install) and go through the main flows: first start, search, download, read, backup.
7. Tag and publish:
   - `git tag -a v<versionName> -m "FreeLibrary <versionName>"`
   - `git push origin v<versionName>`
   - create the GitHub release from that tag, attach the APK and publish its SHA-256
     (`Get-FileHash <apk> -Algorithm SHA256` in PowerShell) in the release notes.

## Keys

- Keep the keystore and its passwords outside the repository, in at least two backups.
- Never reuse the debug key for anything that is published.
- If the key is lost, existing installs can no longer be updated.
