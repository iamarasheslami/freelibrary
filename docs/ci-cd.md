# CI/CD

How a change travels from a branch to a release, what is checked on the way, and what is still open.
The workflows live in `.github/workflows/`. Every action is pinned to a commit and every job has a time limit.

## The flow

```
branch ──► pull request ──► checks ──► auto-merge (squash) ──► main ──► tag vX.Y.Z ──► signed release
              │                │                                │
              │                │                                └─ CodeQL, dependency graph, nightly catalog sync
              │                └─ build, ktlint, all tests, lint, PR title, CodeQL
              └─ Dependabot opens weekly update PRs; patch and minor ones merge themselves once checks pass
```

## What runs, and when

| Workflow | Runs on | What it does | Blocks merging |
|---|---|---|---|
| CI, "Build, lint, and test" | every PR and push to main | catalog fetch, ktlint, shared and catalog-tool tests, app unit tests, debug and release (R8) build, debug APK as an artifact | yes |
| CI, "Android Lint" | every PR and push to main | lintDebug and report | yes |
| PR title | every PR | Conventional Commit title | yes |
| CodeQL | PR, main, weekly | static security analysis of Kotlin | no |
| Auto-merge | owner PRs | queues a squash merge | no |
| Dependabot auto-merge | Dependabot PRs | queues a squash merge for patch and minor updates; a major update gets a comment and waits for you | no |
| Dependency graph | main, weekly | sends the Gradle dependency graph so Dependabot alerts cover every library | no |
| Nightly catalog sync | daily 03:00 UTC | updates the catalog data, validates it, pushes it | no |
| Sweep merged branches | daily | deletes branches of merged PRs | no |
| Release | tag `vX.Y.Z` | checks, signs, verifies and publishes the APK with its checksum and provenance | no |

Required checks are the three that block merging. A pull request cannot merge while any of them fails.

## Dependency updates (Dependabot)

- Weekly, grouped, three open PRs at most, and a three-day wait after a release before it is proposed.
- Major updates, the Android Gradle Plugin, Kotlin, KSP and Hilt are not proposed (the frozen toolchain, see `dependabot.yml`).
- A patch or minor update merges itself once the required checks pass. If a check fails, the PR stays open and nothing reaches main.
- Stuck update: comment `@dependabot rebase` on the PR to rerun the checks on today's main. If it still fails and the new
  version is the cause, comment `@dependabot ignore this minor version` (or `this dependency`).
- Review the ignored majors and the toolchain by hand once every quarter.
- Updates to the GitHub Actions themselves are merged by hand on purpose: they change code that runs with the repository's
  secrets, and the default workflow token cannot always queue a merge for changes to `.github/workflows`. Before merging,
  check that CI is green and that each pinned SHA is the commit of the tagged release (the peeled `^{}` commit, not the
  annotated tag object).

## Security alerts

- The Dependabot alerts for `settings.gradle.kts` are the build's own plugin classpath (Android Gradle Plugin, Gradle and
  lint internals: netty, bouncycastle, logback and similar). None of it is packaged in the app.
- On 2026-11-04 the release runtime classpath was checked (`:app:dependencies --configuration releaseRuntimeClasspath`)
  and none of the flagged libraries are in it, so all 61 alerts were dismissed as "not used".
- An alert on a library that is in the app is different: fix it by updating the library, and do not dismiss it.

## Known failures that are not bugs

- Nightly Catalog Sync: if the push to the data repository fails with `remote: Internal Server Error`, GitHub had a
  problem. Nothing is lost; run the workflow again from the Actions tab.

## Checklist

Done:
- [x] Branch protection on main: squash only, linear history, no force pushes, three required checks
- [x] Conventional Commit titles enforced
- [x] Actions pinned to commit SHAs, least-privilege tokens, one run per branch (older runs cancelled)
- [x] ktlint, unit tests, Android Lint, release build with R8 on every PR
- [x] CodeQL, secret scanning, Dependabot alerts, private vulnerability reporting
- [x] Nightly catalog sync with validation before it publishes
- [x] Time limit on every job; Gradle cache is read-only for PRs
- [x] Tests of the shared and catalog-tool modules run in CI (they did not before)
- [x] Debug APK of every PR available as an artifact
- [x] Dependabot patch and minor updates merge themselves after the checks
- [x] Dependency graph submitted to GitHub
- [x] Release workflow: version, changelog and store text must agree; signature verified; SHA-256; build provenance; R8 mapping kept
- [x] CODEOWNERS

To do, by you (settings and secrets, not code):
- [ ] Release secrets, only when you want CI to build releases (see below)
- [ ] Settings, Actions, General: workflow permissions "Read repository contents" and "Allow GitHub Actions to create and approve pull requests" off
- [ ] Settings, Actions, General: "Require approval for first-time contributors" on

To do, later (each is its own small pull request):
- [ ] Instrumented smoke tests on an emulator (API 23, 29, 34) as a separate, non-blocking workflow; this replaces the manual emulator pass
- [x] Android Lint at zero warnings, with `warningsAsErrors = true` so new warnings fail the PR (a justified exception goes in `app/lint.xml` with a comment)
- [ ] Test coverage report (JaCoCo) with a floor that only goes up
- [ ] actionlint on the workflow files
- [ ] Merge queue, if several people ever merge at once (`merge_group` trigger on CI)
- [ ] OpenSSF Scorecard
- [ ] A bi-monthly APK rebuild (see the roadmap)

## Everyday tasks

**A change or a fix:** branch, commit with a `type: description` message, push, open the PR. Checks run; the PR merges itself.
**A broken main:** `git revert <commit>` on a branch and open a PR titled `revert: ...`. Never force-push.
**A patch release (0.1.1):** fix as above, then follow `RELEASING.md` (version, changelog, store text), tag, and the Release workflow does the rest.

## Release from CI

1. Create the four secrets once (PowerShell, from the repository folder; the keystore stays outside the repository):
   ```
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\release.jks")) | gh secret set RELEASE_KEYSTORE_BASE64
   gh secret set RELEASE_STORE_PASSWORD
   gh secret set RELEASE_KEY_ALIAS
   gh secret set RELEASE_KEY_PASSWORD
   ```
2. Prepare the release pull request as `RELEASING.md` says, merge it, then tag:
   `git tag -a v0.1.0 -m "FreeLibrary 0.1.0"` and `git push origin v0.1.0`.
3. The Release workflow publishes `FreeLibrary-0.1.0.apk`, its `.sha256`, and the attestation. Check a download with
   `gh attestation verify FreeLibrary-0.1.0.apk --repo iamarasheslami/freelibrary`.
4. Keep your own backup of the keystore. If the key is lost, installed apps can no longer be updated.
