package org.freedomsuite.calendar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendars ORDER BY displayName ASC")
    fun observeCalendars(): Flow<List<CalendarEntity>>

    @Query("SELECT * FROM calendars ORDER BY displayName ASC LIMIT 1")
    suspend fun getDefaultCalendar(): CalendarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalendar(calendar: CalendarEntity)

    @Query("SELECT * FROM events ORDER BY startEpochMs ASC")
    fun observeEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE uid = :uid LIMIT 1")
    suspend fun getEvent(uid: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE uid = :uid")
    suspend fun deleteEvent(uid: String)

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<EventEntity>

    @Query("SELECT * FROM calendars")
    suspend fun getAllCalendars(): List<CalendarEntity>

    @Query(
        """
        SELECT * FROM events
        WHERE title LIKE '%' || :term || '%' OR
            COALESCE(description, '') LIKE '%' || :term || '%' OR
            COALESCE(location, '') LIKE '%' || :term || '%'
        ORDER BY startEpochMs DESC
        LIMIT 60
        """,
    )
    suspend fun searchEvents(term: String): List<EventEntity>
}
