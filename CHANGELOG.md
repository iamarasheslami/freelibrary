# Changelog

All notable changes to FreeLibrary are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.1.0] - 2026-11-06

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
- Profile: the reading card shows a gentle streak, the number of days in a row on which a book was opened. The streak is
  still alive while today has not been read yet, nothing is said about a streak that has ended except the best run, and
  Settings, Clear reading history, resets it. The days are kept only on the phone (database version 12, with a migration
  that fills them in from the books already opened or finished).
- Readers: a Page choice in the text panel (Match the app, Sepia, Dim night) for the EPUB, text and PDF readers. Sepia is a
  warm light page and Dim night a soft dark one, each in either app theme. The reading text colours pick the shade that suits
  the page, and a test checks that every one keeps a contrast of 7 to 1 on both pages. Reset to defaults puts the page back.
- Profile: an avatar creator, opened from Edit profile with Create an avatar. The reader builds a round cartoon avatar from
  original artwork: skin tone, five face shapes, five noses, sixteen hair styles (short, long, curly, braids, afro, buns and
  more) in eight colours, seven eye styles in eight eye colours, five mouths, seven glasses, eight hats (beanie, cap, bucket,
  beret, headscarf, cowboy and more) in eight colours, earrings in one or both ears, eleven outfits in eight colours, and a
  background colour, with a Surprise me button. Every choice shows a small preview already wearing it. The avatar is saved
  like a chosen photo, so it appears on Profile and in backups; the creator opens again with the last avatar.
- Profile: a reading card under the name shows the books finished this year and this month, with a ring that fills as
  the reader nears a yearly goal. Tapping the card sets or removes the goal (1 to 365 books). It is all worked out on the
  phone from the dates books were marked finished.
- Settings: a Help and feedback row opens the project's GitHub issues page in the browser.
- Debug builds keep a plain text log of the app's network requests (`files/network-log.txt`: which part asked, host
  and path, status or error, time) to help find failing covers, syncs and downloads. Release builds never write it.
- Home shows an explanation with a Try again button when the bundled catalog cannot be opened,
  for example because the phone has no room to copy it on the first start.
- Privacy policy (`PRIVACY.md`), security policy (`SECURITY.md`) and store listing text.
- The Apache License 2.0 text ships inside the app, and `THIRD_PARTY_NOTICES.md` lists the libraries it covers.
- The catalog can withdraw a book on purpose, for example because of a rights problem. A withdrawn book leaves browsing and
  search on the next sync. If the reader has a reading place, bookmark, highlight, shelf state or download of it, the book
  stays on the reader's own lists and nothing of theirs is deleted (database version 13, with a migration).

### Changed

- Backgrounds: the light theme has a paper grain, the dark theme a soft Pine Shadow vignette, and Home, Explore and
  Profile have a Moss to Slate Green wash at the top. All of it is static, the readers are untouched, and a test checks
  that text keeps its contrast on every one of them.
- Covers: a cover that is already showing or loading is no longer requested again when its row is bound again. A debug
  network log showed 367 of 429 cover requests being cancelled and restarted on Home.
- Settings, Appearance: a Reset to defaults button for the theme, app size, reader fonts, reading text colour and
  reader text size.
- The readers' text panel (the text size button) now also has a font drop-down (ten fonts: Literata, Noto Serif, Lora,
  Source Serif 4, Crimson Pro, EB Garamond, Libre Baskerville, Open Sans, Atkinson Hyperlegible, OpenDyslexic) and a text
  colour drop-down with ten colours, each in a shade that stays readable on the light and on the dark page. An EPUB keeps its own font unless
  one is chosen.
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
- Books are downloaded only from Project Gutenberg and its mirrors, and every redirect is checked against that list.
- The system copies none of the app's data to the cloud or to a new phone. The reader's own backup file is the only way to
  move data.

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

[Unreleased]: https://github.com/iamarasheslami/freelibrary/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/iamarasheslami/freelibrary/releases/tag/v0.1.0
