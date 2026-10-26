package com.example.medreminder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.medreminder.data.local.entity.MedicationCourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationCourseDao {
    @Query("SELECT * FROM medication_courses ORDER BY isActive DESC, name ASC")
    fun observeAll(): Flow<List<MedicationCourseEntity>>

    @Query("SELECT * FROM medication_courses WHERE id = :id")
    suspend fun getById(id: Long): MedicationCourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: MedicationCourseEntity): Long

    @Update
    suspend fun update(course: MedicationCourseEntity)

    @Query("DELETE FROM medication_courses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
