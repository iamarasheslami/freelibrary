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

- Settings: the Storage page lists the books downloaded to the phone with their number and total
  size, and each one can be deleted for good after a confirmation. Clear reading history moved there
  from the Privacy page, which is removed.
- Page view or scroll view is chosen for each book, not for all readers at once. In scroll view a
  swipe to the side moves to the next or the previous chapter (or part), as it does in page view.
- EPUB and text books can be searched: the search icon in the reader's top bar opens a search box,
  and the results list shows the chapter or part and a line of text for every place found. A tap
  opens that place with the words highlighted.
- The readers show the top and bottom bars at first. A tap on the middle of the page hides them, so
  only the book is left, and the next tap shows them again; with a screen reader on, the bars stay. The top bar has small
  icons instead of the three-dot menu and no book name. The bottom bar shows the chapter (or part)
  on the left and the page on the right, without Previous and Next buttons.
- The Book page buttons have a wider gap between them and line up with the page's side padding.
- The Explore tab has an icon like the other tabs, and the filter button sits to the right of the
  search button. Explore shows a "Loading new categories" line with a spinner under the last
  slider while it loads more. The Book page buttons have no captions, so all of them fit one row;
  press and hold one to see its name.
- Slider headings use a larger display serif in the accent colour, slider cards no longer show the
  author, "View more" is now "View all", and the Continue Reading slider has bigger covers.
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
- Book covers are loaded from the Project Gutenberg mirror `gutenberg.pglaf.org` when `gutenberg.org` cannot be
  reached, so they show on networks that block the official site. The app checks once at start (and every ten minutes
  after that) whether gutenberg.org answers, so a blocked site does not hold the covers back. The generated cover is still
  shown when neither answers.
- A book updated by the catalog sync was saved in many separate steps. If the app was stopped half way, the book could
  stay without its shelves, authors or formats and was never fetched again, and while a sync ran the Home sliders
  changed (and reloaded their covers) after every step. Each book is now saved in one transaction.
- The nightly catalog update gave a new date to every book Gutenberg's feed merely touched, so each night the apps
  downloaded and rewrote dozens of unchanged books and Sync now reported them as updates. A book that is the same in
  everything but the date now keeps its file and its date.
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
