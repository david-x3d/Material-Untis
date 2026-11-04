package dev.x3d.dayline.data.repo

import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.Absence
import dev.x3d.dayline.domain.model.AbsenceReason
import dev.x3d.dayline.domain.model.AbsenceReport
import dev.x3d.dayline.domain.model.AbsenceReportOptions
import dev.x3d.dayline.domain.model.AccountType
import dev.x3d.dayline.domain.model.AbsenceStatus
import dev.x3d.dayline.domain.model.Holiday
import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.model.SchoolYear
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
        School("Gymnasium Musterstadt", "musterstadt", "example.webuntis.com", "Musterweg 1"),
    )
    private val sessionState = MutableStateFlow<UserSession?>(
        UserSession("example.webuntis.com", "musterstadt", "Gymnasium Musterstadt", "max.muster", 5, 1001, 12),
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

    override suspend fun loginPassword(user: String, password: String, accountType: AccountType?) {
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

    override suspend fun loginSecret(user: String, secret: String, accountType: AccountType?) = loginPassword(user, secret, accountType)

    override suspend fun logout() {
        sessionState.value = null
        lessons.value = emptyList()
    }

    override suspend fun refresh(force: Boolean) {
        sync.update { it.copy(isSyncing = false, lastSuccessAt = System.currentTimeMillis(), fromCache = !force) }
    }

    override suspend fun loadWeek(monday: UntisDate) = Unit

    private val absenceReport = MutableStateFlow<AbsenceReport?>(null)

    override fun absences(): Flow<AbsenceReport?> = absenceReport

    override suspend fun refreshAbsences() {
        if (absenceReport.value != null) return
        val today = UntisDate.today()
        val year = today.year - if (today.month < 8) 1 else 0
        absenceReport.value = AbsenceReport(
            schoolYear = SchoolYear("$year/${year + 1}", UntisDate(year * 10000 + 801), UntisDate((year + 1) * 10000 + 731)),
            absences = listOf(
                Absence(6, today.plusDays(-1), today.plusDays(-1), UntisTime(745), UntisTime(1310), "Krankheit", "Demo: Krankmeldung wartet auf Entschuldigung.", AbsenceStatus.OPEN, "offen"),
                Absence(5, today.plusDays(-3), today.plusDays(-3), UntisTime(940), UntisTime(1115), "Arzttermin", "Demo: Attest eingereicht.", AbsenceStatus.EXCUSED, "entschuldigt"),
                Absence(4, today.plusDays(-7), today.plusDays(-5), UntisTime(745), UntisTime(1535), "Krankheit", "Demo: Mehrtägige Erkrankung mit Fieber.", AbsenceStatus.EXCUSED, "entschuldigt"),
                Absence(3, today.plusDays(-8), today.plusDays(-8), UntisTime(745), UntisTime(805), "Verspätung", "Demo: Bus verpasst.", AbsenceStatus.UNEXCUSED, "nicht entschuldigt"),
                Absence(2, today.plusDays(-10), today.plusDays(-10), UntisTime(1225), UntisTime(1535), "Vorzeitig gegangen", null, AbsenceStatus.UNEXCUSED, "nicht entschuldigt"),
                Absence(1, today.plusDays(-14), today.plusDays(-14), UntisTime(745), UntisTime(1310), "Beurlaubung", "Demo: Familienangelegenheit, genehmigt.", AbsenceStatus.EXCUSED, "entschuldigt"),
            ),
            fetchedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun absenceReportOptions() = AbsenceReportOptions(
        canReport = true,
        reasons = listOf(AbsenceReason(1, "Krankheit"), AbsenceReason(2, "Arzttermin"), AbsenceReason(3, "Sonstiger Grund")),
        defaultReasonId = 1,
        reasonAndTextHidden = false,
    )

    override suspend fun reportAbsence(
        startDate: UntisDate,
        startTime: UntisTime,
        endDate: UntisDate,
        endTime: UntisTime,
        reasonId: Int,
        text: String,
    ) {
        require(endDate >= startDate) { "Enddatum muss nach dem Startdatum liegen." }
        require(endDate != startDate || endTime > startTime) { "Endzeit muss nach der Startzeit liegen." }
        val reason = absenceReportOptions().reasons.firstOrNull { it.id == reasonId }
        requireNotNull(reason) { "Bitte einen gültigen Grund wählen." }
        refreshAbsences()
        absenceReport.update { report ->
            report?.copy(
                absences = listOf(Absence(
                    (report.absences.maxOfOrNull { it.id } ?: 0) + 1,
                    startDate, endDate, startTime, endTime, reason.name, text.ifBlank { null },
                    AbsenceStatus.OPEN, "offen – Demo",
                )) + report.absences,
                fetchedAt = System.currentTimeMillis(),
            )
        }
    }

    override fun lessonsForDate(date: UntisDate): Flow<List<Lesson>> =
        lessons.map { list -> list.filter { it.date == date }.sortedBy { it.start.raw } }

    override fun lessonsInRange(start: UntisDate, end: UntisDate): Flow<List<Lesson>> =
        lessons.map { list -> list.filter { it.date in start..end }.sortedWith(compareBy({ it.date.yyyymmdd }, { it.start.raw })) }

    override fun lesson(id: Long): Flow<Lesson?> = lessons.map { list -> list.find { it.id == id } }

    override fun subjects(): Flow<List<NamedRef>> =
        lessons.map { list -> list.mapNotNull { it.subject }.distinctBy { it.shortName }.sortedBy { it.shortName } }

    override fun timegrid(): Flow<List<TimegridDay>> = lessons.map { list ->
        list.groupBy { it.date.toLocalDate().dayOfWeek.value + 1 }.map { (weekday, periods) ->
            TimegridDay(weekday, periods.map { it.start to it.end }.distinct().sortedBy { it.first.raw }
                .mapIndexed { index, (start, end) -> TimegridUnit("${index + 1}", start, end) })
        }
    }

    override fun holidays(): Flow<List<Holiday>> = flowOf(DemoData.holidays())

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
