package com.freelibrary.app.data.repository

import javax.inject.Inject

/**
 * One place the catalog's books come from. [folder] is where its manifest and book files live in the
 * catalog-data repository, relative to the repository root: empty for Project Gutenberg, whose files
 * sit at the root, and "sources/<name>/" (with the slash) for every other source. Each source has its
 * own row in the sources table, so book numbers from different sources never clash.
 */
data class CatalogSource(
    val name: String,
    val attribution: String,
    val folder: String,
) {
    init {
        require(folder.isEmpty() || (folder.endsWith("/") && !folder.startsWith("/") && !folder.contains(".."))) {
            "A source folder is empty or a relative path ending in a slash, not \"$folder\""
        }
    }

    /** Where this source's manifest is, relative to the root of the catalog-data repository. */
    val manifestPath: String get() = "${folder}manifest.json"

    /** Where the file of the book numbered [externalId] is, relative to the root of the repository. */
    fun bookPath(externalId: String): String {
        require(isValidExternalId(externalId)) { "Not a valid book id: \"$externalId\"" }
        return "${folder}books/$externalId.json"
    }

    companion object {
        private val VALID_EXTERNAL_ID = Regex("[A-Za-z0-9_-]{1,40}")

        /**
         * Whether [id] can safely be part of a file address. The ids come from a manifest fetched over the
         * network, so anything that could change the address (a dot, a slash, a colon) is refused.
         */
        fun isValidExternalId(id: String): Boolean = VALID_EXTERNAL_ID.matches(id)

        val GUTENBERG =
            CatalogSource(
                name = "Project Gutenberg",
                attribution = "Public domain works via Project Gutenberg (https://www.gutenberg.org)",
                folder = "",
            )
    }
}

/**
 * The sources the app syncs, in order. The first one is Project Gutenberg: without its manifest the
 * sync does not run at all, while any other source that cannot be reached only counts as a failure.
 */
class CatalogSources(
    val all: List<CatalogSource>,
) {
    @Inject
    constructor() : this(listOf(CatalogSource.GUTENBERG))

    init {
        require(all.isNotEmpty()) { "There must be at least one source" }
        require(all.map { it.name }.toSet().size == all.size) { "Two sources have the same name" }
        require(all.map { it.folder }.toSet().size == all.size) { "Two sources share a folder" }
    }
}
