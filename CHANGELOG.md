# Changelog

All notable changes to FreeLibrary are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Home shows an explanation with a Try again button when the bundled catalog cannot be opened,
  for example because the phone has no room to copy it on the first start.
- Privacy policy (`PRIVACY.md`), security policy (`SECURITY.md`) and store listing text.

### Changed

- On Android 15 and newer the app keeps its content clear of the side bars, the display cutout and
  the keyboard.
- Release builds can be signed from a local `keystore.properties` file.

### Security

- Cleartext traffic is disabled for all hosts. GitHub Actions are pinned to commit SHAs and the
  repository is analysed with CodeQL.
- A backup can no longer restore a book file larger than a download may be (300 MB).
- The catalog sync refuses book ids that could change a file address, checks that each book file is the
  book the manifest names, gives every call time limits, and refuses answers larger than 64 MB.
- The nightly catalog workflow validates the export (manifest, files, dates, nothing missing) and stops
  before publishing anything inconsistent.

### Fixed

- Retrying a failed download the moment its failure was shown could be ignored.
- The filter chosen in Explore stayed on after visiting another tab. It is now dropped when the reader leaves Explore.
- Explore opens faster: its shelves are counted in the background shortly after the app starts, the shelves of
  one page are read at the same time, and the next page starts loading earlier. A failing shelf query shows as a
  load error instead of crashing the page.

## [0.1.0] - not yet released

First public version.

### Added

- A catalog of more than 78,000 public-domain books that ships inside the app and is kept current
  by a background sync.
- Home, Explore and Search with title and author search, typo tolerance, curated shelves, and
  language, format and genre filters.
- Book page with Read, Download, Save, Finished, Start over, Bookmarks and Delete actions.
- EPUB reader (page view or scrolling, right-to-left books, contents list, text size), PDF reader
  (zoom, dark-theme aware) and plain-text reader. Reading positions are saved automatically.
- Shelves (Continue Reading, Want to Read, Finished), bookmarks with notes, and a profile.
- Downloads with Project Gutenberg mirror fallback.
- Backup and restore of shelves, bookmarks, settings and downloaded books.
- Light and dark themes and a Small, Medium and Large interface size.
- No accounts, advertising, analytics or tracking.

[Unreleased]: https://github.com/iamarasheslami/freelibrary/compare/main...HEAD
[0.1.0]: https://github.com/iamarasheslami/freelibrary/releases
