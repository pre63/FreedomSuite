package org.freedomsuite.messages.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Transaction
    @Query(
        """
        SELECT c.*,
            (SELECT m.body FROM messages m WHERE m.channelId = c.id ORDER BY m.createdAtEpochMs DESC LIMIT 1) AS latestPreview
        FROM channels c
        ORDER BY c.createdAtEpochMs DESC
        """,
    )
    fun observeAllWithPreview(): Flow<List<ChannelWithPreviewRow>>

    @Query("SELECT * FROM channels ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: ChannelEntity)

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChannelEntity?

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteById(id: String)
}

data class ChannelWithPreviewRow(
    val id: String,
    val name: String,
    val type: String,
    val createdAtEpochMs: Long,
    val memberCount: Int,
    val latestPreview: String?,
) {
    fun toChannelWithPreview(): ChannelWithPreview = ChannelWithPreview(
        channel = ChannelEntity(id, name, type, createdAtEpochMs, memberCount),
        latestPreview = latestPreview,
    )
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY createdAtEpochMs ASC")
    fun observeForChannel(channelId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT body FROM messages WHERE channelId = :channelId ORDER BY createdAtEpochMs DESC LIMIT 1")
    fun observeLatestPreview(channelId: String): Flow<String?>

    @Query("SELECT * FROM messages ORDER BY createdAtEpochMs ASC")
    suspend fun getAll(): List<MessageEntity>

    @Query(
        """
        SELECT m.id, m.channelId, m.body, m.createdAtEpochMs, m.authorLabel, c.name AS channelName
        FROM messages m
        INNER JOIN channels c ON c.id = m.channelId
        WHERE m.body LIKE '%' || :term || '%' OR
            c.name LIKE '%' || :term || '%' OR
            m.authorLabel LIKE '%' || :term || '%'
        ORDER BY m.createdAtEpochMs DESC
        LIMIT 60
        """,
    )
    suspend fun searchMessages(term: String): List<MessageSearchRow>
}

data class MessageSearchRow(
    val id: String,
    val channelId: String,
    val body: String,
    val createdAtEpochMs: Long,
    val authorLabel: String,
    val channelName: String,
)
