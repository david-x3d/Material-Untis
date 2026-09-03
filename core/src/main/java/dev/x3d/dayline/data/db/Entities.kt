package dev.x3d.dayline.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: Long,
    val date: Int,
    val startTime: Int,
    val endTime: Int,
    val subjectId: Int,
    val subjectShort: String,
    val subjectLong: String,
    val teacherId: Int,
    val teacherShort: String,
    val teacherLong: String,
    val teacherOriginal: String?,
    val roomId: Int,
    val roomShort: String,
    val roomLong: String,
    val classId: Int,
    val classShort: String,
    val status: String,
    val type: String,
    val info: String?,
    val substText: String?,
    val lessonText: String?,
)

@Entity(tableName = "timegrid_units")
data class TimegridEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekday: Int,
    val name: String,
    val startTime: Int,
    val endTime: Int,
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val longValue: Long?,
    val stringValue: String?,
)
