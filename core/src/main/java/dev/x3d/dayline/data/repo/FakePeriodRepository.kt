package dev.x3d.dayline.data.repo

import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.model.LessonStatus
import dev.x3d.dayline.domain.model.LessonType
import dev.x3d.dayline.domain.model.NamedRef
import dev.x3d.dayline.domain.model.School
import dev.x3d.dayline.domain.model.SyncState
import dev.x3d.dayline.domain.model.TimegridDay
import dev.x3d.dayline.domain.model.TimegridUnit
import dev.x3d.dayline.domain.model.UserSession
import dev.x3d.dayline.domain.model.WatchStatus
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakePeriodRepository(
    initialLessons: List<Lesson> = sampleLessons(),
) : PeriodRepository {
    private val school = MutableStateFlow<School?>(
        School("Example Gymnasium", "example", "ajax.webuntis.com", "Example Street 1"),
    )
    private val sessionState = MutableStateFlow<UserSession?>(
        UserSession("ajax.webuntis.com", "example", "Example Gymnasium", "student", 5, 1001, 12),
    )
    private val sync = MutableStateFlow(SyncState(lastSuccessAt = System.currentTimeMillis()))
    private val watch = MutableStateFlow(WatchStatus(connected = false))
    private val interval = MutableStateFlow(15)
    private val lessons = MutableStateFlow(initialLessons)

    override val session: StateFlow<UserSession?> = sessionState.asStateFlow()
    override val syncState: StateFlow<SyncState> = sync.asStateFlow()
    override val watchStatus: StateFlow<WatchStatus> = watch.asStateFlow()
    override val selectedSchool: StateFlow<School?> = school.asStateFlow()

    override suspend fun searchSchools(query: String): List<School> =
        listOf(school.value!!).filter { it.displayName.contains(query, ignoreCase = true) }

    override suspend fun saveSchool(school: School) {
        this.school.value = school
    }

    override suspend fun loginPassword(user: String, password: String) {
        sessionState.value = UserSession(
            host = school.value?.host ?: "ajax.webuntis.com",
            school = school.value?.loginName ?: "example",
            schoolDisplayName = school.value?.displayName ?: "Example Gymnasium",
            user = user,
            personType = 5,
            personId = 1001,
            klasseId = 12,
        )
    }

    override suspend fun loginSecret(user: String, secret: String) = loginPassword(user, secret)

    override suspend fun logout() {
        sessionState.value = null
        lessons.value = emptyList()
    }

    override suspend fun refresh(force: Boolean) {
        sync.update { it.copy(isSyncing = false, lastSuccessAt = System.currentTimeMillis(), fromCache = !force) }
    }

    override fun lessonsForDate(date: UntisDate): Flow<List<Lesson>> =
        lessons.map { list -> list.filter { it.date == date }.sortedBy { it.start.raw } }

    override fun lessonsInRange(start: UntisDate, end: UntisDate): Flow<List<Lesson>> =
        lessons.map { list -> list.filter { it.date in start..end }.sortedWith(compareBy({ it.date.yyyymmdd }, { it.start.raw })) }

    override fun lesson(id: Long): Flow<Lesson?> = lessons.map { list -> list.find { it.id == id } }

    override fun timegrid(): Flow<List<TimegridDay>> = flowOf(
        listOf(
            TimegridDay(
                weekday = 2,
                units = listOf(
                    TimegridUnit("1", UntisTime(800), UntisTime(845)),
                    TimegridUnit("2", UntisTime(850), UntisTime(935)),
                    TimegridUnit("3", UntisTime(955), UntisTime(1040)),
                ),
            ),
        ),
    )

    override suspend fun setSyncIntervalMinutes(minutes: Int) {
        interval.value = minutes
    }

    override fun syncIntervalMinutes(): Flow<Int> = interval

    companion object {
        fun sampleLessons(date: UntisDate = UntisDate.today()): List<Lesson> = listOf(
            Lesson(
                id = 1,
                date = date,
                start = UntisTime(800),
                end = UntisTime(845),
                subject = NamedRef(1, "M", "Mathematics"),
                teacher = NamedRef(10, "AB", "A. Baker"),
                room = NamedRef(3, "101", "Room 101"),
                klass = NamedRef(12, "9a", "Class 9a"),
                status = LessonStatus.NORMAL,
                type = LessonType.LESSON,
                info = null,
                substText = null,
                lessonText = null,
            ),
            Lesson(
                id = 2,
                date = date,
                start = UntisTime(850),
                end = UntisTime(935),
                subject = NamedRef(2, "En", "English"),
                teacher = NamedRef(11, "CD", "C. Diaz"),
                room = NamedRef(4, "204", "Room 204"),
                klass = NamedRef(12, "9a", "Class 9a"),
                status = LessonStatus.IRREGULAR,
                type = LessonType.LESSON,
                info = null,
                substText = "Covered by C. Diaz",
                lessonText = null,
            ),
            Lesson(
                id = 3,
                date = date,
                start = UntisTime(955),
                end = UntisTime(1040),
                subject = NamedRef(3, "PE", "Physical Education"),
                teacher = NamedRef(12, "EF", "E. Frost"),
                room = NamedRef(5, "GYM", "Gymnasium"),
                klass = NamedRef(12, "9a", "Class 9a"),
                status = LessonStatus.CANCELLED,
                type = LessonType.LESSON,
                info = "Sports day postponed",
                substText = null,
                lessonText = null,
            ),
        )
    }
}
