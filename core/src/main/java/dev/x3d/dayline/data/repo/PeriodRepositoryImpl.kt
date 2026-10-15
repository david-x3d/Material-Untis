package dev.x3d.dayline.data.repo

import dev.x3d.dayline.data.db.LessonDao
import dev.x3d.dayline.data.db.MetaDao
import dev.x3d.dayline.data.db.MetaEntity
import dev.x3d.dayline.data.db.TimegridDao
import dev.x3d.dayline.data.db.TimegridEntity
import dev.x3d.dayline.data.mapper.LessonMapper
import dev.x3d.dayline.data.prefs.CredentialStore
import dev.x3d.dayline.data.prefs.UserPrefs
import dev.x3d.dayline.data.rpc.SchoolSearchClient
import dev.x3d.dayline.data.rpc.Totp
import dev.x3d.dayline.data.rpc.WebUntisClient
import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.data.mapper.AbsenceMapper
import dev.x3d.dayline.data.rpc.AbsenceDto
import dev.x3d.dayline.data.rpc.ReportAbsenceRequest
import dev.x3d.dayline.domain.model.AbsenceReason
import dev.x3d.dayline.domain.model.AbsenceReport
import dev.x3d.dayline.domain.model.AbsenceReportOptions
import dev.x3d.dayline.domain.model.AccountType
import dev.x3d.dayline.domain.time.UntisTime
import dev.x3d.dayline.domain.model.AuthKind
import dev.x3d.dayline.domain.model.SchoolYear
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.model.NamedRef
import dev.x3d.dayline.domain.model.SubjectColors
import dev.x3d.dayline.domain.model.School
import dev.x3d.dayline.domain.model.SyncState
import dev.x3d.dayline.domain.model.TimegridDay
import dev.x3d.dayline.domain.model.UserSession
import dev.x3d.dayline.domain.model.WatchPayload
import dev.x3d.dayline.domain.model.WatchStatus
import dev.x3d.dayline.domain.time.UntisDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PeriodRepositoryImpl(
    private val client: WebUntisClient,
    private val schoolSearch: SchoolSearchClient,
    private val lessonDao: LessonDao,
    private val timegridDao: TimegridDao,
    private val metaDao: MetaDao,
    private val credentials: CredentialStore,
    private val prefs: UserPrefs,
    private val watchPusher: WatchPusher?,
    private val json: Json,
) : PeriodRepository {

    fun interface WatchPusher {
        suspend fun push(payload: WatchPayload)
    }

    private val mutex = Mutex()
    private val sessionState = MutableStateFlow(credentials.session)
    private val schoolState = MutableStateFlow(credentials.school)
    private val syncStateFlow = MutableStateFlow(SyncState())
    private val watchState = MutableStateFlow(WatchStatus())

    override val session: StateFlow<UserSession?> = sessionState.asStateFlow()
    override val selectedSchool: StateFlow<School?> = schoolState.asStateFlow()
    override val syncState: StateFlow<SyncState> = syncStateFlow.asStateFlow()
    override val watchStatus: StateFlow<WatchStatus> = watchState.asStateFlow()

    init {
        credentials.school?.let { school ->
            client.connection = WebUntisClient.Connection(school.host, school.loginName)
        }
        client.reauthenticator = WebUntisClient.Reauthenticator { silentReauth() }
    }

    override suspend fun searchSchools(query: String): List<School> = schoolSearch.search(query)

    override suspend fun saveSchool(school: School) {
        credentials.school = school
        schoolState.value = school
        client.connection = WebUntisClient.Connection(school.host, school.loginName)
    }

    override suspend fun loginPassword(user: String, password: String, accountType: AccountType?) {
        login(user, password, AuthKind.PASSWORD, storeSecret = password, hint = accountType)
    }

    override suspend fun loginSecret(user: String, secret: String, accountType: AccountType?) {
        val otp = Totp.generate(secret)
        login(user, otp, AuthKind.SECRET, storeSecret = secret, hint = accountType)
    }

    private suspend fun login(user: String, passwordOrOtp: String, kind: AuthKind, storeSecret: String, hint: AccountType?) {
        val school = credentials.school ?: throw PeriodException.Auth("Select a school first")
        val auth = client.authenticate(school.host, school.loginName, user, passwordOrOtp)
        credentials.saveCredentials(user, storeSecret, kind)
        val session = UserSession(
            host = school.host,
            school = school.loginName,
            schoolDisplayName = school.displayName,
            user = user,
            personType = auth.personType,
            personId = auth.personId,
            klasseId = auth.klasseId,
        ).withLinkedStudent(hint)
        credentials.session = session
        sessionState.value = session
        refresh(force = true)
    }

    /**
     * Parent accounts act for a child: the web app lists them as `persons` (type 5) in embedded.do.
     * Teachers and students act for themselves.
     */
    private suspend fun UserSession.withLinkedStudent(hint: AccountType?): UserSession {
        if (personType == UserSession.PERSON_STUDENT) {
            return copy(accountType = AccountType.STUDENT, studentId = personId)
        }
        if (personType == UserSession.PERSON_TEACHER) return copy(accountType = AccountType.TEACHER)
        val context = runCatching { client.getWebContext(refresh = true) }.getOrNull()
        val child = context?.persons?.firstOrNull { it.type == UserSession.PERSON_STUDENT }
        return when {
            child != null -> copy(accountType = AccountType.PARENT, studentId = child.id, studentName = child.displayName)
            hint != null -> copy(accountType = hint)
            else -> this
        }
    }

    /** Sessions saved before linked students existed get their child looked up once. */
    private suspend fun currentSession(): UserSession {
        val current = sessionState.value ?: credentials.session ?: throw PeriodException.Auth("Not signed in")
        if (current.studentId != null || current.personType == UserSession.PERSON_TEACHER) return current
        val updated = current.withLinkedStudent(hint = null)
        if (updated != current) {
            credentials.session = updated
            sessionState.value = updated
        }
        return updated
    }

    private suspend fun silentReauth() {
        val school = credentials.school ?: throw PeriodException.SessionExpired()
        val user = credentials.username ?: throw PeriodException.SessionExpired()
        val secret = credentials.secretOrPassword() ?: throw PeriodException.SessionExpired()
        val password = when (credentials.authKind) {
            AuthKind.SECRET -> Totp.generate(secret)
            AuthKind.PASSWORD -> secret
            null -> throw PeriodException.SessionExpired()
        }
        client.authenticate(school.host, school.loginName, user, password)
    }

    override suspend fun logout() {
        runCatching { client.logout() }
        credentials.clearAll()
        prefs.clear()
        lessonDao.deleteAll()
        timegridDao.deleteAll()
        metaDao.deleteAll()
        sessionState.value = null
        schoolState.value = null
        syncStateFlow.value = SyncState()
        watchState.value = WatchStatus()
    }

    override suspend fun refresh(force: Boolean) = mutex.withLock {
        val current = runCatching { currentSession() }.getOrElse {
            sessionState.value ?: credentials.session ?: throw PeriodException.Auth("Not signed in")
        }
        syncStateFlow.update { it.copy(isSyncing = true, lastError = null) }
        try {
            val (weekStart, weekEnd) = UntisDate.weekRange()
            val cachedImport = metaDao.get(META_IMPORT)?.longValue
            val latest = runCatching { client.getLatestImportTime() }.getOrNull()
            val skipFetch = !force && latest != null && latest == cachedImport &&
                lessonDao.listRange(weekStart.yyyymmdd, weekEnd.yyyymmdd).isNotEmpty()
            if (!skipFetch) {
                fetchRange(current, weekStart, weekEnd)
                if (latest != null) {
                    metaDao.put(MetaEntity(META_IMPORT, latest, null))
                    prefs.setLastImportTime(latest)
                }
            }
            val now = System.currentTimeMillis()
            prefs.setLastSyncAt(now)
            pushWatch(now)
            syncStateFlow.update {
                it.copy(
                    isSyncing = false,
                    lastSuccessAt = now,
                    lastError = null,
                    offline = false,
                    fromCache = skipFetch,
                )
            }
        } catch (e: PeriodException.Network) {
            val now = System.currentTimeMillis()
            pushWatch(now)
            syncStateFlow.update {
                it.copy(
                    isSyncing = false,
                    lastError = "Offline — showing saved timetable",
                    offline = true,
                    fromCache = true,
                )
            }
        } catch (e: PeriodException) {
            syncStateFlow.update {
                it.copy(isSyncing = false, lastError = e.message, offline = false)
            }
            throw e
        }
    }

    private suspend fun fetchRange(session: UserSession, start: UntisDate, end: UntisDate) {
        val (elementId, elementType) = session.timetableElement
        val periods = client.getTimetable(
            personId = elementId,
            personType = elementType,
            startDate = start.yyyymmdd,
            endDate = end.yyyymmdd,
        )
        val lessons = periods.map(LessonMapper::toDomain)
        lessonDao.deleteRange(start.yyyymmdd, end.yyyymmdd)
        lessonDao.upsertAll(lessons.map(LessonMapper::toEntity))
        runCatching {
            val grid = client.getTimegridUnits().map(LessonMapper::toTimegrid)
            if (grid.isEmpty()) return@runCatching
            timegridDao.deleteAll()
            timegridDao.insertAll(
                grid.flatMap { day ->
                    day.units.map { unit ->
                        TimegridEntity(
                            weekday = day.weekday,
                            name = unit.name,
                            startTime = unit.start.raw,
                            endTime = unit.end.raw,
                        )
                    }
                },
            )
        }
    }

    override suspend fun loadWeek(monday: UntisDate) = mutex.withLock {
        val current = currentSession()
        fetchRange(current, monday, monday.plusDays(6))
    }

    override fun absences(): Flow<AbsenceReport?> =
        metaDao.observe(META_ABSENCES).map { entity ->
            val raw = entity?.stringValue ?: return@map null
            runCatching { json.decodeFromString(AbsenceCache.serializer(), raw) }.getOrNull()?.toReport()
        }

    override suspend fun refreshAbsences() {
        val current = currentSession()
        val year = runCatching { client.getCurrentSchoolYear() }.getOrNull()
        val (start, end) = if (year != null) {
            year.startDate to year.endDate
        } else {
            fallbackSchoolYear()
        }
        val absences = client.getStudentAbsences(current.studentId ?: current.personId, start, end)
        val cache = AbsenceCache(
            yearName = year?.name.orEmpty(),
            startDate = start,
            endDate = end,
            fetchedAt = System.currentTimeMillis(),
            absences = absences,
        )
        metaDao.put(MetaEntity(META_ABSENCES, null, json.encodeToString(AbsenceCache.serializer(), cache)))
    }

    override suspend fun absenceReportOptions(): AbsenceReportOptions {
        val meta = client.getAbsenceMeta()
        return AbsenceReportOptions(
            canReport = meta.canReportAbsence,
            reasons = meta.absenceReasons.map { AbsenceReason(it.id, it.name) },
            defaultReasonId = meta.defaultAbsenceReason?.id ?: meta.absenceReasons.firstOrNull()?.id,
            reasonAndTextHidden = meta.isAbsenceReasonAndTextHidden,
        )
    }

    override suspend fun reportAbsence(
        startDate: UntisDate,
        startTime: UntisTime,
        endDate: UntisDate,
        endTime: UntisTime,
        reasonId: Int,
        text: String,
    ) {
        val current = currentSession()
        val studentId = current.studentId ?: current.personId
        client.reportAbsence(
            ReportAbsenceRequest(
                startDate = startDate.yyyymmdd,
                startTime = startTime.raw,
                endDate = endDate.yyyymmdd,
                endTime = endTime.raw,
                text = text,
                reasonId = reasonId,
                studentId = studentId,
            ),
        )
        runCatching { refreshAbsences() }
    }

    /** 1 Aug – 31 Jul around today, if the server has no current school year. */
    private fun fallbackSchoolYear(): Pair<Int, Int> {
        val today = LocalDate.now()
        val startYear = if (today.monthValue >= 8) today.year else today.year - 1
        return UntisDate.from(LocalDate.of(startYear, 8, 1)).yyyymmdd to
            UntisDate.from(LocalDate.of(startYear + 1, 7, 31)).yyyymmdd
    }

    @Serializable
    private data class AbsenceCache(
        val yearName: String,
        val startDate: Int,
        val endDate: Int,
        val fetchedAt: Long,
        val absences: List<AbsenceDto>,
    ) {
        fun toReport() = AbsenceReport(
            schoolYear = SchoolYear(yearName, UntisDate(startDate), UntisDate(endDate)),
            absences = AbsenceMapper.sort(absences.map(AbsenceMapper::toDomain)),
            fetchedAt = fetchedAt,
        )
    }

    private suspend fun pushWatch(now: Long) {
        val today = UntisDate.today()
        val lessons = lessonDao.listRange(today.yyyymmdd, today.yyyymmdd).map(LessonMapper::toDomain)
        // The watch is always dark, so it gets the dark-theme variant of each subject colour.
        val colors = runCatching { prefs.subjectColors.first() }.getOrDefault(SubjectColors())
        val payload = LessonMapper.toWatchPayload(today, now, lessons) { subject -> colors.argb(subject, dark = true) }
        try {
            watchPusher?.push(payload)
            watchState.value = WatchStatus(connected = true, lastPushedAt = now)
        } catch (_: Exception) {
            watchState.value = WatchStatus(connected = false, lastPushedAt = watchState.value.lastPushedAt, message = "Watch not reachable")
        }
    }

    override fun lessonsForDate(date: UntisDate): Flow<List<Lesson>> =
        lessonDao.observeDate(date.yyyymmdd).map { list -> list.map(LessonMapper::toDomain) }

    override fun lessonsInRange(start: UntisDate, end: UntisDate): Flow<List<Lesson>> =
        lessonDao.observeRange(start.yyyymmdd, end.yyyymmdd).map { list -> list.map(LessonMapper::toDomain) }

    override fun lesson(id: Long): Flow<Lesson?> =
        lessonDao.observeId(id).map { it?.let(LessonMapper::toDomain) }

    override fun subjects(): Flow<List<NamedRef>> =
        lessonDao.observeSubjects().map { rows -> rows.map { NamedRef(0, it.subjectShort, it.subjectLong.ifEmpty { it.subjectShort }) } }

    override fun timegrid(): Flow<List<TimegridDay>> =
        timegridDao.observeAll().map { entities ->
            entities.groupBy { it.weekday }.map { (day, units) ->
                TimegridDay(
                    weekday = day,
                    units = units.map {
                        dev.x3d.dayline.domain.model.TimegridUnit(
                            name = it.name,
                            start = dev.x3d.dayline.domain.time.UntisTime.parse(it.startTime),
                            end = dev.x3d.dayline.domain.time.UntisTime.parse(it.endTime),
                        )
                    },
                )
            }
        }

    override suspend fun setSyncIntervalMinutes(minutes: Int) {
        prefs.setSyncIntervalMinutes(minutes)
    }

    override fun syncIntervalMinutes(): Flow<Int> = prefs.syncIntervalMinutes

    fun markWatchStatus(status: WatchStatus) {
        watchState.value = status
    }

    companion object {
        private const val META_IMPORT = "latest_import_time"
        private const val META_ABSENCES = "absences"
    }
}
