package dev.x3d.dayline.domain

import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.model.School
import dev.x3d.dayline.domain.model.SyncState
import dev.x3d.dayline.domain.model.TimegridDay
import dev.x3d.dayline.domain.model.UserSession
import dev.x3d.dayline.domain.model.WatchStatus
import dev.x3d.dayline.domain.time.UntisDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PeriodRepository {
    val session: StateFlow<UserSession?>
    val syncState: StateFlow<SyncState>
    val watchStatus: StateFlow<WatchStatus>
    val selectedSchool: StateFlow<School?>

    suspend fun searchSchools(query: String): List<School>
    suspend fun saveSchool(school: School)
    suspend fun loginPassword(user: String, password: String)
    suspend fun loginSecret(user: String, secret: String)
    suspend fun logout()
    suspend fun refresh(force: Boolean = false)
    fun lessonsForDate(date: UntisDate): Flow<List<Lesson>>
    fun lessonsInRange(start: UntisDate, end: UntisDate): Flow<List<Lesson>>
    fun lesson(id: Long): Flow<Lesson?>
    fun timegrid(): Flow<List<TimegridDay>>
    suspend fun setSyncIntervalMinutes(minutes: Int)
    fun syncIntervalMinutes(): Flow<Int>
}
