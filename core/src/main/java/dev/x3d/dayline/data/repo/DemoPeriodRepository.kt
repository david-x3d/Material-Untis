package dev.x3d.dayline.data.repo

import dev.x3d.dayline.data.prefs.UserPrefs
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.*
import dev.x3d.dayline.domain.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Switches the entire data source; demo writes never touch credentials, Room or WebUntis. */
@OptIn(ExperimentalCoroutinesApi::class)
class DemoPeriodRepository(
    private val real: PeriodRepository,
    private val prefs: UserPrefs,
    private val demo: PeriodRepository = FakePeriodRepository(initialLessons = DemoData.lessons()),
) : PeriodRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val enabled = MutableStateFlow(runBlocking { prefs.demoMode.first() })
    override val demoMode: StateFlow<Boolean> = enabled.asStateFlow()
    private val current get() = if (enabled.value) demo else real
    private fun <T> select(read: (PeriodRepository) -> Flow<T>): Flow<T> = enabled.flatMapLatest { read(if (it) demo else real) }
    private fun <T> state(read: (PeriodRepository) -> StateFlow<T>): StateFlow<T> =
        select(read).stateIn(scope, SharingStarted.Eagerly, read(current).value)

    override val session = state { it.session }
    override val selectedSchool = state { it.selectedSchool }
    override val syncState = state { it.syncState }
    override val watchStatus = state { it.watchStatus }
    override suspend fun setDemoMode(enabled: Boolean) {
        if (enabled == this.enabled.value) return
        prefs.setDemoMode(enabled)
        this.enabled.value = enabled
        if (enabled) demo.refreshAbsences()
    }
    override suspend fun searchSchools(query: String) = current.searchSchools(query)
    override suspend fun saveSchool(school: School) = current.saveSchool(school)
    override suspend fun loginPassword(user: String, password: String, accountType: AccountType?) = current.loginPassword(user, password, accountType)
    override suspend fun loginSecret(user: String, secret: String, accountType: AccountType?) = current.loginSecret(user, secret, accountType)
    override suspend fun logout() {
        if (enabled.value) setDemoMode(false) else real.logout()
    }
    override suspend fun refresh(force: Boolean) = current.refresh(force)
    override suspend fun loadWeek(monday: UntisDate) = current.loadWeek(monday)
    override fun absences() = select { it.absences() }
    override suspend fun refreshAbsences() = current.refreshAbsences()
    override suspend fun absenceReportOptions() = current.absenceReportOptions()
    override suspend fun reportAbsence(startDate: UntisDate, startTime: UntisTime, endDate: UntisDate, endTime: UntisTime, reasonId: Int, text: String) =
        current.reportAbsence(startDate, startTime, endDate, endTime, reasonId, text)
    override fun lessonsForDate(date: UntisDate) = select { it.lessonsForDate(date) }
    override fun lessonsInRange(start: UntisDate, end: UntisDate) = select { it.lessonsInRange(start, end) }
    override fun lesson(id: Long) = select { it.lesson(id) }
    override fun subjects() = select { it.subjects() }
    override fun timegrid() = select { it.timegrid() }
    override fun holidays() = select { it.holidays() }
    override fun syncIntervalMinutes() = real.syncIntervalMinutes()
    override suspend fun setSyncIntervalMinutes(minutes: Int) = real.setSyncIntervalMinutes(minutes)
}
