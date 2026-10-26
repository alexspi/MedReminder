package com.example.medreminder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.medreminder.data.local.dao.DoseEventDao
import com.example.medreminder.data.local.dao.MedicationCourseDao
import com.example.medreminder.data.local.dao.UserProfileDao
import com.example.medreminder.data.local.entity.DoseEventEntity
import com.example.medreminder.data.local.entity.MedicationCourseEntity
import com.example.medreminder.data.local.entity.UserProfileEntity

@Database(
    entities = [
        MedicationCourseEntity::class,
        DoseEventEntity::class,
        UserProfileEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationCourseDao(): MedicationCourseDao
    abstract fun doseEventDao(): DoseEventDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medication_courses ADD COLUMN referenceOptionId TEXT")
                db.execSQL("ALTER TABLE medication_courses ADD COLUMN referenceTradeName TEXT")
                db.execSQL("ALTER TABLE medication_courses ADD COLUMN referenceInnName TEXT")
                db.execSQL("ALTER TABLE medication_courses ADD COLUMN referenceRegNumber TEXT")
                db.execSQL("ALTER TABLE medication_courses ADD COLUMN referenceStatus TEXT")
                db.execSQL("ALTER TABLE medication_courses ADD COLUMN referenceFormsSummary TEXT")
                db.execSQL("ALTER TABLE medication_courses ADD COLUMN referenceSourceLabel TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id INTEGER NOT NULL PRIMARY KEY,
                        fullName TEXT NOT NULL,
                        age TEXT NOT NULL,
                        heightCm TEXT NOT NULL,
                        weightKg TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "med_reminder.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { instance = it }
            }
    }
}
