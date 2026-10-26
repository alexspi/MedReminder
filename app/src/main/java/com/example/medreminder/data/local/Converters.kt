package com.example.medreminder.data.local

import androidx.room.TypeConverter
import com.example.medreminder.data.local.entity.MedicationCourseEntity

class Converters {
    @TypeConverter
    fun fromDoseStatus(value: DoseStatus): String = value.name

    @TypeConverter
    fun toDoseStatus(value: String): DoseStatus = DoseStatus.valueOf(value)
}

/** Parse and format course time lists stored in [MedicationCourseEntity.timesOfDayCsv]. */
object CourseTimeCodec {
    fun encode(times: List<java.time.LocalTime>): String =
        times.sorted().joinToString(",") { it.toString() }

    fun decode(csv: String): List<java.time.LocalTime> =
        csv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map(java.time.LocalTime::parse)
}
