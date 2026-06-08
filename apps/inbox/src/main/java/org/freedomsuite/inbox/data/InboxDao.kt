package org.freedomsuite.inbox.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxDao {
    @Query("SELECT * FROM messages WHERE folder = :folder ORDER BY dateEpochMs DESC")
    fun observeFolder(folder: String): Flow<List<MailMessageEntity>>

    @Query(
        """
        SELECT * FROM messages
        WHERE folder = :folder AND (
            subject LIKE '%' || :query || '%' OR
            "from" LIKE '%' || :query || '%' OR
            body LIKE '%' || :query || '%' OR
            snippet LIKE '%' || :query || '%'
        )
        ORDER BY dateEpochMs DESC
        """,
    )
    fun observeFolderSearch(folder: String, query: String): Flow<List<MailMessageEntity>>

    @Query("SELECT * FROM messages WHERE folder = :folder AND uid = :uid LIMIT 1")
    suspend fun getByFolderAndUid(folder: String, uid: Long): MailMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MailMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MailMessageEntity)

    @Query("DELETE FROM messages WHERE folder = :folder AND uid = :uid")
    suspend fun deleteByFolderAndUid(folder: String, uid: Long)

    @Query("DELETE FROM messages WHERE folder = :folder")
    suspend fun clearFolder(folder: String)

    @Query("UPDATE messages SET isRead = 1 WHERE folder = :folder AND uid = :uid")
    suspend fun markRead(folder: String, uid: Long)

    @Query(
        """
        SELECT * FROM messages
        WHERE subject LIKE '%' || :term || '%' OR
            "from" LIKE '%' || :term || '%' OR
            body LIKE '%' || :term || '%' OR
            snippet LIKE '%' || :term || '%'
        ORDER BY dateEpochMs DESC
        LIMIT 60
        """,
    )
    suspend fun searchAllFolders(term: String): List<MailMessageEntity>
}
