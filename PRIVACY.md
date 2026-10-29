# Privacy Policy

FreeLibrary is a free, open-source (GPLv3) reader for public-domain books. It has no accounts, no analytics, no advertising and no tracking. This document describes exactly what the app stores and which network requests it makes.

_Last updated: 2026-10-27_

## What the app does not do

- It does not collect, transmit or sell personal data.
- It does not contain analytics, crash-reporting, advertising or attribution SDKs.
- It does not require an account, and it does not ask for your name, email, location, contacts, camera, microphone or storage permissions.

## What stays on your device

Your shelves (Saved, Currently reading, Finished), reading progress, bookmarks, theme and reader settings, and downloaded books are stored only in the app's private storage on your device. Android's automatic cloud backup is disabled for the app. The only way data leaves the device is the backup file you create yourself with Settings → Backup; where that file is saved is your choice.

Uninstalling the app, or clearing its storage in Android settings, deletes all of this data.

## Network requests

The app requests the INTERNET and ACCESS_NETWORK_STATE permissions and connects only to:

| Host | Purpose |
| --- | --- |
| `raw.githubusercontent.com` | Catalog updates (public JSON files from the project's data repository) |
| `gutenberg.org` | Book files and covers you choose to open or download |
| `gutenberg.pglaf.org`, `aleph.gutenberg.org`, `www.mirrorservice.org`, `mirrors.xmission.com` | Project Gutenberg mirrors, tried in this order only if a download from `gutenberg.org` fails; `gutenberg.pglaf.org` is also asked for a book's cover picture when `gutenberg.org` cannot be reached |

Every request is made over HTTPS. Requests carry a User-Agent of the form `FreeLibrary/<version> (+https://github.com/iamarasheslami/freelibrary)` and your IP address is visible to these hosts as with any web request; they are operated by third parties under their own privacy policies (GitHub and Project Gutenberg). No identifier, account or device information is added by the app.

The built-in reader displays books in a WebView with JavaScript disabled and network loads blocked, so book content cannot make requests of its own.

## Children

The app collects no data from anyone, including children.

## Changes

Changes to this policy are published in this file; the history is visible in the repository.

## Contact

Open an issue at https://github.com/iamarasheslami/freelibrary/issues. For security problems, follow [SECURITY.md](SECURITY.md).
