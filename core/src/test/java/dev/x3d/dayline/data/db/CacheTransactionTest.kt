package dev.x3d.dayline.data.db

import android.content.Context
import androidx.room.Room
import dev.x3d.dayline.data.mapper.LessonMapper
import dev.x3d.dayline.data.rpc.PeriodDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CacheTransactionTest {
    @Test
    fun failedLessonReplacementPreservesCachedLessons() = runBlocking {
        val db = database()
        try {
            val old = LessonMapper.toEntity(LessonMapper.toDomain(PeriodDto(
                id = 1, date = 20261103, startTime = 800, endTime = 845,
            )))
            db.lessonDao().upsertAll(listOf(old))
            rejectInserts(db, "lessons")
            expectFailure { db.lessonDao().replaceRange(20261102, 20261108, listOf(old.copy(id = 2))) }
            assertEquals(listOf(old), db.lessonDao().listRange(20261102, 20261108))
        } finally {
            db.close()
        }
    }

    @Test
    fun failedTimegridReplacementPreservesCachedUnits() = runBlocking {
        val db = database()
        try {
            val old = TimegridEntity(id = 1, weekday = 1, name = "1", startTime = 800, endTime = 845)
            db.timegridDao().insertAll(listOf(old))
            rejectInserts(db, "timegrid_units")
            expectFailure { db.timegridDao().replaceAll(listOf(old.copy(id = 2))) }
            assertEquals(listOf(old), db.timegridDao().observeAll().first())
        } finally {
            db.close()
        }
    }

    private fun database() = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication() as Context, PeriodDatabase::class.java,
    ).allowMainThreadQueries().build()

    private fun rejectInserts(db: PeriodDatabase, table: String) {
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_insert BEFORE INSERT ON $table BEGIN SELECT RAISE(ABORT, 'test failure'); END",
        )
    }

    private suspend fun expectFailure(block: suspend () -> Unit) {
        try {
            block()
        } catch (_: android.database.sqlite.SQLiteException) {
            return
        }
        throw AssertionError("Expected insert failure")
    }
}
