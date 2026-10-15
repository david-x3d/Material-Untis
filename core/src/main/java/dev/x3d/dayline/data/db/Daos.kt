package dev.x3d.dayline.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE date = :date ORDER BY startTime ASC")
    fun observeDate(date: Int): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE date BETWEEN :start AND :end ORDER BY date ASC, startTime ASC")
    fun observeRange(start: Int, end: Int): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    fun observeId(id: Long): Flow<LessonEntity?>

    @Query("SELECT * FROM lessons WHERE date BETWEEN :start AND :end")
    suspend fun listRange(start: Int, end: Int): List<LessonEntity>

    /** Every subject seen in the cached timetable, for the colour settings. */
    @Query("SELECT subjectShort, MAX(subjectLong) AS subjectLong FROM lessons WHERE subjectShort != '' GROUP BY subjectShort ORDER BY subjectShort")
    fun observeSubjects(): Flow<List<SubjectRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LessonEntity>)

    @Query("DELETE FROM lessons WHERE date BETWEEN :start AND :end")
    suspend fun deleteRange(start: Int, end: Int)

    @Query("DELETE FROM lessons")
    suspend fun deleteAll()
}

data class SubjectRow(val subjectShort: String, val subjectLong: String)

@Dao
interface TimegridDao {
    @Query("SELECT * FROM timegrid_units ORDER BY weekday ASC, startTime ASC")
    fun observeAll(): Flow<List<TimegridEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TimegridEntity>)

    @Query("DELETE FROM timegrid_units")
    suspend fun deleteAll()
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM meta WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): MetaEntity?

    @Query("SELECT * FROM meta WHERE `key` = :key LIMIT 1")
    fun observe(key: String): Flow<MetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: MetaEntity)

    @Query("DELETE FROM meta")
    suspend fun deleteAll()
}
