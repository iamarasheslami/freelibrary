# Withdrawing books from the catalog

A book is withdrawn when it has to leave the catalog on purpose: Project Gutenberg takes it down, a rights holder
objects, or a bad batch was published. The nightly sync only ever adds and updates, so a withdrawal is a separate,
deliberate step.

## What a withdrawal does

- The book leaves `manifest.json`'s `books` list and its `books/<id>.json` file is deleted.
- Its id joins the manifest's `withdrawn` list. The list is permanent: ids stay in it, and the checks refuse to
  publish a manifest that drops one (unless the book is listed again).
- The nightly sync and the full catalog rebuild skip withdrawn ids, so a book does not come back by itself.
- An app that still has the book handles it on its next sync. If the reader has nothing of it (no reading
  place, bookmark, highlight, shelf state or download), the book is deleted from the app's catalog. If the reader
  has any of those, the book stays, is marked in the `withdrawn_books` table, and no longer appears in browsing,
  search or filter counts; it still shows on the reader's own lists. A book listed again is unmarked on the next
  sync. The reader's own shelves, progress, bookmarks, highlights and downloaded files are never deleted.

## How to withdraw books

1. Clone the data repository next to this one (`git clone https://github.com/iamarasheslami/freelibrary-catalog-data`)
   and copy its `manifest.json` into an empty folder `../published`; that copy is what is published now.
2. From this repository, run the tool with the clone's folder and the ids:
   `./gradlew :catalog-tool:withdrawBooks -PexportArgs="../freelibrary-catalog-data 12345 67890"`
   It lists what it withdrew, and fails when an id is not in the catalog or is not a book id.
3. Check the result against what is published:
   `./gradlew :catalog-tool:validateExport -PexportArgs="../freelibrary-catalog-data --previous ../published"`
4. Commit and push in the data repository with a message such as `chore: withdraw books 12345, 67890`.
   The next nightly run checks the export again before it publishes anything.

## Restoring a book by mistake

Put the book's file back in `books/`, add it to `manifest.json`'s `books` list, and remove its id from `withdrawn` in the
same commit. The checks accept this because the book is listed again.
