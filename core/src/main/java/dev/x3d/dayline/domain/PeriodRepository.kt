package dev.x3d.dayline.domain

import dev.x3d.dayline.domain.model.AbsenceReport
import dev.x3d.dayline.domain.model.AbsenceReportOptions
import dev.x3d.dayline.domain.model.AccountType
import dev.x3d.dayline.domain.model.Holiday
import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.model.NamedRef
import dev.x3d.dayline.domain.model.School
import dev.x3d.dayline.domain.model.SyncState
import dev.x3d.dayline.domain.model.TimegridDay
import dev.x3d.dayline.domain.model.UserSession
import dev.x3d.dayline.domain.model.WatchStatus
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PeriodRepository {
    val session: StateFlow<UserSession?>
    val syncState: StateFlow<SyncState>
    val watchStatus: StateFlow<WatchStatus>
    val selectedSchool: StateFlow<School?>

    suspend fun searchSchools(query: String): List<School>
    suspend fun saveSchool(school: School)
    /** [accountType] is what the user picked in onboarding; the server's answer wins when it is clear. */
    suspend fun loginPassword(user: String, password: String, accountType: AccountType? = null)
    suspend fun loginSecret(user: String, secret: String, accountType: AccountType? = null)
    suspend fun logout()
    suspend fun refresh(force: Boolean = false)

    /** Fetches Monday–Sunday of the week starting at [monday] into the cache. */
    suspend fun loadWeek(monday: UntisDate)

    /** Cached absences of the current school year (null until fetched once). */
    fun absences(): Flow<AbsenceReport?>
    suspend fun refreshAbsences()

    /** Whether and how this account may report absences ("Krankmeldung"). */
    suspend fun absenceReportOptions(): AbsenceReportOptions

    /** Sends an absence report to the school, then reloads the absence list. */
    suspend fun reportAbsence(
        startDate: UntisDate,
        startTime: UntisTime,
        endDate: UntisDate,
        endTime: UntisTime,
        reasonId: Int,
        text: String,
    )
    fun lessonsForDate(date: UntisDate): Flow<List<Lesson>>
    fun lessonsInRange(start: UntisDate, end: UntisDate): Flow<List<Lesson>>
    fun lesson(id: Long): Flow<Lesson?>
    fun timegrid(): Flow<List<TimegridDay>>

    /** Cached school holidays ("Ferien"), refreshed along with the timetable. */
    fun holidays(): Flow<List<Holiday>>

    /** Subjects in the cached timetable (short + long name), for the colour settings. */
    fun subjects(): Flow<List<NamedRef>>
    suspend fun setSyncIntervalMinutes(minutes: Int)
    fun syncIntervalMinutes(): Flow<Int>
}
