package org.freedomsuite.files.provider

import android.content.ContentProvider
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.freedomsuite.core.searchapi.SearchBridge
import org.freedomsuite.core.searchapi.SearchCursor
import org.freedomsuite.core.searchapi.SearchHit
import org.freedomsuite.core.searchapi.SearchQueryExpander
import org.freedomsuite.files.data.FilesDatabase

class FilesSearchProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (uriMatcher.match(uri) != RESULTS) return null
        val query = uri.getQueryParameter("q") ?: return null
        val ctx = context?.applicationContext ?: return null
        val dao = FilesDatabase.getInstance(ctx).filesDao()
        val terms = SearchQueryExpander.expand(query)
        val seen = mutableSetOf<String>()
        val hits = mutableListOf<SearchHit>()

        runBlocking {
            terms.forEach { term ->
                dao.searchImagesByTerm(term).forEach { file ->
                    if (!seen.add(file.id)) return@forEach
                    val analysis = dao.getAnalysis(file.id)
                    hits += SearchHit(
                        id = file.id,
                        source = SearchBridge.Source.PHOTO,
                        title = file.displayName,
                        subtitle = analysis?.objectLabels.orEmpty().ifBlank { "Photo" },
                        snippet = analysis?.ocrText?.take(120).orEmpty(),
                        timestampMs = file.createdAtEpochMs,
                        packageName = SearchBridge.Source.PHOTO.packageName,
                        payload = file.id,
                    )
                }
            }
        }
        return SearchCursor.fromHits(hits.sortedByDescending { it.timestampMs })
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.dir/search_result"

    override fun insert(uri: Uri, values: android.content.ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: android.content.ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        private const val RESULTS = 1
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(SearchBridge.Source.PHOTO.authority, "results", RESULTS)
        }
    }
}
