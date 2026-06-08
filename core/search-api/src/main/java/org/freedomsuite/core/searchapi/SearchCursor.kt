package org.freedomsuite.core.searchapi

import android.database.MatrixCursor
import org.freedomsuite.core.searchapi.SearchBridge.Columns

object SearchCursor {
    fun fromHits(hits: List<SearchHit>): MatrixCursor {
        val cursor = MatrixCursor(
            arrayOf(
                Columns.ID,
                Columns.SOURCE,
                Columns.TITLE,
                Columns.SUBTITLE,
                Columns.SNIPPET,
                Columns.TIMESTAMP_MS,
                Columns.PACKAGE_NAME,
                Columns.PAYLOAD,
            ),
        )
        hits.forEach { hit ->
            cursor.addRow(
                arrayOf(
                    hit.id,
                    hit.source.id,
                    hit.title,
                    hit.subtitle,
                    hit.snippet,
                    hit.timestampMs,
                    hit.packageName,
                    hit.payload,
                ),
            )
        }
        return cursor
    }
}
