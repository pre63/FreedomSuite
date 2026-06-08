package org.freedomsuite.core.searchapi

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import org.freedomsuite.core.searchapi.SearchBridge.Columns

object SearchBridge {
    const val PERMISSION = "org.freedomsuite.permission.SEARCH"

    const val EXTRA_HIT_ID = "freedom_search_hit_id"
    const val EXTRA_HIT_SOURCE = "freedom_search_hit_source"
    const val EXTRA_MAIL_FOLDER = "freedom_search_mail_folder"
    const val EXTRA_MAIL_UID = "freedom_search_mail_uid"

    object Columns {
        const val ID = "id"
        const val SOURCE = "source"
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val SNIPPET = "snippet"
        const val TIMESTAMP_MS = "timestamp_ms"
        const val PACKAGE_NAME = "package_name"
        const val PAYLOAD = "payload"
    }

    enum class Source(val id: String, val authority: String, val packageName: String, val label: String) {
        MAIL("mail", "org.freedomsuite.inbox.search", "org.freedomsuite.inbox", "Mail"),
        PHOTO("photo", "org.freedomsuite.files.search", "org.freedomsuite.files", "Photos"),
        CALENDAR("calendar", "org.freedomsuite.calendar.search", "org.freedomsuite.calendar", "Calendar"),
        MESSAGE("message", "org.freedomsuite.messages.search", "org.freedomsuite.messages", "Messages"),
    }

    fun resultsUri(authority: String, query: String): Uri =
        Uri.parse("content://$authority/results").buildUpon()
            .appendQueryParameter("q", query)
            .build()
}

data class SearchHit(
    val id: String,
    val source: SearchBridge.Source,
    val title: String,
    val subtitle: String,
    val snippet: String,
    val timestampMs: Long,
    val packageName: String,
    val payload: String,
)

class UnifiedSearchClient(private val context: Context) {
    fun search(
        query: String,
        sources: Set<SearchBridge.Source> = SearchBridge.Source.entries.toSet(),
        limitPerSource: Int = 40,
    ): List<SearchHit> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val hits = mutableListOf<SearchHit>()
        sources.forEach { source ->
            if (resolvePackage(source.packageName) == null) return@forEach
            hits += querySource(source, trimmed, limitPerSource)
        }
        return hits.sortedByDescending { it.timestampMs }
    }

    fun grouped(
        query: String,
        sources: Set<SearchBridge.Source> = SearchBridge.Source.entries.toSet(),
    ): Map<SearchBridge.Source, List<SearchHit>> =
        search(query, sources).groupBy { it.source }

    fun openHit(hit: SearchHit): Boolean {
        val pkg = resolvePackage(hit.packageName) ?: hit.packageName
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launch.putExtra(SearchBridge.EXTRA_HIT_ID, hit.id)
        launch.putExtra(SearchBridge.EXTRA_HIT_SOURCE, hit.source.id)
        if (hit.source == SearchBridge.Source.MAIL) {
            val parts = hit.payload.split("|", limit = 2)
            if (parts.size == 2) {
                launch.putExtra(SearchBridge.EXTRA_MAIL_FOLDER, parts[0])
                launch.putExtra(SearchBridge.EXTRA_MAIL_UID, parts[1].toLongOrNull() ?: -1L)
            }
        }
        context.startActivity(launch)
        return true
    }

    private fun querySource(source: SearchBridge.Source, query: String, limit: Int): List<SearchHit> {
        val uri = SearchBridge.resultsUri(source.authority, query)
        val cursor = runCatching {
            context.contentResolver.query(uri, null, null, null, null)
        }.getOrNull() ?: return emptyList()

        return cursor.use { mapCursor(it, source) }.take(limit)
    }

    private fun mapCursor(cursor: Cursor, source: SearchBridge.Source): List<SearchHit> {
        val idIdx = cursor.getColumnIndex(Columns.ID)
        val titleIdx = cursor.getColumnIndex(Columns.TITLE)
        val subtitleIdx = cursor.getColumnIndex(Columns.SUBTITLE)
        val snippetIdx = cursor.getColumnIndex(Columns.SNIPPET)
        val tsIdx = cursor.getColumnIndex(Columns.TIMESTAMP_MS)
        val pkgIdx = cursor.getColumnIndex(Columns.PACKAGE_NAME)
        val payloadIdx = cursor.getColumnIndex(Columns.PAYLOAD)
        if (idIdx < 0 || titleIdx < 0) return emptyList()

        val results = mutableListOf<SearchHit>()
        while (cursor.moveToNext()) {
            results += SearchHit(
                id = cursor.getString(idIdx),
                source = source,
                title = cursor.getString(titleIdx).orEmpty(),
                subtitle = if (subtitleIdx >= 0) cursor.getString(subtitleIdx).orEmpty() else "",
                snippet = if (snippetIdx >= 0) cursor.getString(snippetIdx).orEmpty() else "",
                timestampMs = if (tsIdx >= 0) cursor.getLong(tsIdx) else 0L,
                packageName = if (pkgIdx >= 0) {
                    cursor.getString(pkgIdx).orEmpty()
                } else {
                    source.packageName
                },
                payload = if (payloadIdx >= 0) cursor.getString(payloadIdx).orEmpty() else "",
            )
        }
        return results
    }

    private fun resolvePackage(basePackage: String): String? =
        listOf(basePackage, "$basePackage.dev").firstOrNull { isInstalled(it) }

    private fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)
}
