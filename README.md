# FreeLibrary

A free, offline-first ebook reader for Android, built for older phones and for places where
connections are slow, expensive or restricted. Browse and search a catalog of more than 78,000
public-domain books, download the ones you want, and read them without a connection.

FreeLibrary has no accounts, no advertising, no analytics and no tracking. Everything you do in
the app stays on your phone.

## Features

- **Large catalog, fully offline.** The book catalog ships inside the app and is kept current by a
  small background sync, so browsing and searching work without a connection.
- **Search and discovery.** Title and author search with typo tolerance, curated shelves, genre
  and language filters, and a Home page of featured shelves.
- **Three readers.** EPUB (page-by-page or scrolling, right-to-left books supported), PDF
  (pinch to zoom, dark-theme aware) and plain text. Reading positions are saved automatically.
- **Your shelves.** Continue Reading, Want to Read and Finished, with bookmarks and notes.
- **Private by design.** No account, no tracking. The only network traffic is the catalog sync and
  the books and covers you ask for.
- **Backup and restore.** Export your shelves, bookmarks, settings and downloaded books to a single
  file and import them on another phone.
- **Made for small, old devices.** Runs on Android 6.0 and newer, with a Small, Medium and Large
  interface size and light and dark themes.

## Requirements

- Android 6.0 (API 23) or newer.
- Free storage for the app (the bundled catalog database alone is about 170 MB) and for the books
  you download.

## Privacy and network use

FreeLibrary asks for the `INTERNET` and `ACCESS_NETWORK_STATE` permissions and nothing else. It
contacts only these hosts:

| Host | Purpose |
| --- | --- |
| `raw.githubusercontent.com` | Catalog updates (public data repository) |
| `gutenberg.org` | Books and covers |
| `gutenberg.pglaf.org`, `aleph.gutenberg.org`, `www.mirrorservice.org`, `mirrors.xmission.com` | Project Gutenberg mirrors, used only when a download from `gutenberg.org` fails; `gutenberg.pglaf.org` also supplies cover pictures when `gutenberg.org` cannot be reached |

All traffic uses HTTPS; cleartext traffic is disabled. The app collects no personal data, sends no
telemetry and has no crash reporting service. The reading state it keeps (shelves, positions,
bookmarks, your optional name and picture) is stored in the app's private storage and is not
included in Android cloud backups. Use the in-app backup to move it between phones.

## Building from source

Requirements: JDK 21 and the Android SDK (Android Studio installs both).

```
git clone https://github.com/iamarasheslami/freelibrary.git
cd freelibrary
./gradlew fetchCatalogDatabase   # downloads the bundled catalog and verifies its SHA-256
./gradlew assembleDebug
```

On Windows, use `gradlew.bat`. Run the checks before opening a pull request:

```
./gradlew ktlintCheck testDebugUnitTest lintDebug
```

## Project layout

| Module | Contents |
| --- | --- |
| `app` | The Android application (Kotlin, Views, MVVM) |
| `shared` | The catalog data contract shared by the app and the catalog tool |
| `catalog-tool` | Command-line tool that builds the bundled catalog and the nightly updates |

The app uses Hilt for dependency injection, Room for storage, Retrofit and OkHttp for networking,
Navigation Component with Safe Args, Paging 3 for long lists and Coil for cover images.

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](./CONTRIBUTING.md) first. To report a security
problem, follow [SECURITY.md](./SECURITY.md) and do not open a public issue.

## Data sources and credits

Book metadata, text and covers come from [Project Gutenberg](https://www.gutenberg.org). The
Project Gutenberg name and trademark belong to the Project Gutenberg Literary Archive Foundation;
FreeLibrary is an independent app and is not affiliated with or endorsed by it. Third-party
components are listed in [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md).

## License

FreeLibrary is licensed under the [GNU General Public License v3.0](./LICENSE). Any modified or
redistributed version must stay open source under the same terms.
