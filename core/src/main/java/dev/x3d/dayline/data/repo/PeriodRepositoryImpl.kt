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
import dev.x3d.dayline.domain.model.AuthKind
import dev.x3d.dayline.domain.model.Lesson
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

    override suspend fun loginPassword(user: String, password: String) {
        login(user, password, AuthKind.PASSWORD, storeSecret = password)
    }

    override suspend fun loginSecret(user: String, secret: String) {
        val otp = Totp.generate(secret)
        login(user, otp, AuthKind.SECRET, storeSecret = secret)
    }

    private suspend fun login(user: String, passwordOrOtp: String, kind: AuthKind, storeSecret: String) {
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
        )
        credentials.session = session
        sessionState.value = session
        refresh(force = true)
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
        val current = sessionState.value ?: credentials.session ?: throw PeriodException.Auth("Not signed in")
        syncStateFlow.update { it.copy(isSyncing = true, lastError = null) }
        try {
            val (weekStart, weekEnd) = UntisDate.weekRange()
            val cachedImport = metaDao.get(META_IMPORT)?.longValue
            val latest = runCatching { client.getLatestImportTime() }.getOrNull()
            val skipFetch = !force && latest != null && latest == cachedImport &&
                lessonDao.listRange(weekStart.yyyymmdd, weekEnd.yyyymmdd).isNotEmpty()
            if (!skipFetch) {
                val periods = client.getTimetable(
                    personId = current.personId,
                    personType = current.personType,
                    startDate = weekStart.yyyymmdd,
                    endDate = weekEnd.yyyymmdd,
                )
                val lessons = periods.map(LessonMapper::toDomain)
                lessonDao.deleteRange(weekStart.yyyymmdd, weekEnd.yyyymmdd)
                lessonDao.upsertAll(lessons.map(LessonMapper::toEntity))
                runCatching {
                    val grid = client.getTimegridUnits().map(LessonMapper::toTimegrid)
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

    private suspend fun pushWatch(now: Long) {
        val today = UntisDate.today()
        val lessons = lessonDao.listRange(today.yyyymmdd, today.yyyymmdd).map(LessonMapper::toDomain)
        val payload = LessonMapper.toWatchPayload(today, now, lessons)
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
    }
}
