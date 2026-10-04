# Contributing to FreeLibrary

Thank you for considering a contribution. FreeLibrary is built to stay free, private,
and accessible — contributions should align with that mission.

## Getting started

1. Fork the repository and clone your fork locally.
2. Install JDK 21 and the Android SDK (Android Studio sets up both).
3. Enable the repository's git hooks (see [One-time setup](#one-time-setup)).
4. Download the bundled catalog database. The tests need it and git does not store it:
   `./gradlew fetchCatalogDatabase` (the download is verified against a pinned SHA-256).
5. Create a branch from `main` named `<type>/<short-description>`, for example
   `feat/home-screen-data` or `fix/search-ranking`.
6. Make your change, commit it, and open a pull request against `main`.

On Windows, use `gradlew.bat` in place of `./gradlew`.

## One-time setup

After cloning, enable the repository's git hooks (a pre-commit check that runs ktlint
automatically) by running:

```
git config core.hooksPath .githooks
```

This is a local git setting and isn't stored in the repository itself, so each clone needs
to run it once.

## Checking your change locally

```
./gradlew ktlintFormat    # auto-fix style issues
./gradlew ktlintCheck     # style check (also runs before every commit)
./gradlew test            # unit tests (run fetchCatalogDatabase first)
./gradlew lintDebug       # Android Lint
```

## Commit messages and pull request titles

This project uses [Conventional Commits](https://www.conventionalcommits.org/). Allowed
types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`,
`revert`. Examples: `feat: add zoom control to reader screen`,
`fix: correct download progress bar state`.

Pull requests are squash-merged, so the pull request title becomes the single commit message
on `main`. A CI check rejects titles that don't follow the format. Keep each pull request to
one focused change.

## Pull request workflow

`main` is always kept in a working state and is protected by a repository ruleset, recorded
in `.github/rulesets/main.json`:

- No direct pushes: every change lands through a pull request from a short-lived branch.
- Required checks: **Build, lint, and test**, **Android Lint** and **Validate PR title**.
- Squash merge only, with a linear history; merged branches are deleted automatically.
- While the project has a single maintainer, pull requests opened by the maintainer are merged
  automatically once the required checks pass. Pull requests from anyone else are reviewed and
  merged by the maintainer.

To re-apply the ruleset after editing the file:

```
gh api --method PUT repos/<owner>/<repo>/rulesets/<ruleset-id> --input .github/rulesets/main.json
```

## Dependency updates

Dependabot opens one grouped pull request per week for Gradle dependencies and one for GitHub
Actions. Pull requests from Dependabot are never merged automatically, and their checks must
pass. Major versions and everything tied to the build toolchain (Android Gradle Plugin,
Kotlin and KSP, Hilt, AndroidX minor versions, Gradle wrapper minor versions) are deliberately
excluded: the project stays on one tested toolchain, and those upgrades are done by hand as a
single change.

## Code of conduct

Be respectful and constructive. This project serves people in difficult circumstances;
contributions should reflect that seriousness of purpose.

## Reporting issues

Please use the issue templates provided when filing a bug report or feature request.
