package com.example.medreminder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.medreminder.data.local.DoseStatus
import com.example.medreminder.data.local.entity.DoseEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseEventDao {
    @Query(
        """
        SELECT * FROM dose_events
        WHERE scheduledAtEpochMillis >= :dayStartMillis
          AND scheduledAtEpochMillis < :dayEndMillis
        ORDER BY scheduledAtEpochMillis ASC
        """,
    )
    fun observeForDay(dayStartMillis: Long, dayEndMillis: Long): Flow<List<DoseEventEntity>>

    @Query("DELETE FROM dose_events WHERE courseId = :courseId")
    suspend fun deleteByCourseId(courseId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<DoseEventEntity>)

    @Query("SELECT * FROM dose_events WHERE id = :id")
    suspend fun getById(id: Long): DoseEventEntity?

    @Update
    suspend fun update(event: DoseEventEntity)

    @Query(
        """
        SELECT * FROM dose_events
        WHERE status = :status AND scheduledAtEpochMillis > :minMillis
        ORDER BY scheduledAtEpochMillis ASC
        """,
    )
    suspend fun getByStatusAfter(minMillis: Long, status: DoseStatus): List<DoseEventEntity>

    @Query(
        """
        SELECT * FROM dose_events
        WHERE scheduledAtEpochMillis >= :rangeStart
          AND scheduledAtEpochMillis < :rangeEnd
        ORDER BY scheduledAtEpochMillis ASC
        """,
    )
    fun observeInRange(rangeStart: Long, rangeEnd: Long): Flow<List<DoseEventEntity>>

    @Query(
        """
        SELECT * FROM dose_events
        WHERE scheduledAtEpochMillis >= :rangeStart
          AND scheduledAtEpochMillis < :rangeEnd
          AND status IN ('TAKEN', 'SKIPPED')
        ORDER BY scheduledAtEpochMillis DESC
        """,
    )
    fun observeTakenOrSkippedInRange(rangeStart: Long, rangeEnd: Long): Flow<List<DoseEventEntity>>
}
