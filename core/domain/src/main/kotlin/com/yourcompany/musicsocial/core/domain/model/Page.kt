package com.yourcompany.musicsocial.core.domain.model

/**
 * Cursor (keyset) pagination request. Cursors are opaque to callers; the data layer encodes
 * them (e.g. "created_at,id" of the last row). Keyset pagination stays fast on large tables,
 * unlike OFFSET, and does not skip or duplicate rows when new items arrive mid-scroll.
 */
data class PageRequest(
    val limit: Int = DEFAULT_LIMIT,
    val cursor: String? = null,
) {
    init {
        require(limit in 1..MAX_LIMIT) { "limit must be between 1 and $MAX_LIMIT" }
    }

    companion object {
        const val DEFAULT_LIMIT = 30
        const val MAX_LIMIT = 100
    }
}

data class Page<out T>(
    val items: List<T>,
    /** Cursor for the next page, or null when this is the last page. */
    val nextCursor: String?,
) {
    val isLastPage: Boolean get() = nextCursor == null
}
