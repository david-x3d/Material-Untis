package dev.x3d.dayline.ui.absences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.Absence
import dev.x3d.dayline.domain.model.AbsenceReport
import dev.x3d.dayline.domain.model.AbsenceReportOptions
import dev.x3d.dayline.domain.model.AbsenceStatus
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** OPEN = everything not excused yet (unexcused or still pending). */
enum class AbsenceFilter { ALL, EXCUSED, OPEN }

data class AbsencesUiState(
    val report: AbsenceReport? = null,
    val filter: AbsenceFilter = AbsenceFilter.ALL,
    val loading: Boolean = false,
    val error: Throwable? = null,
) {
    val all: List<Absence> get() = report?.absences.orEmpty()
    fun count(filter: AbsenceFilter): Int = when (filter) {
        AbsenceFilter.ALL -> all.size
        AbsenceFilter.EXCUSED -> all.count { it.status == AbsenceStatus.EXCUSED }
        AbsenceFilter.OPEN -> all.count { it.status != AbsenceStatus.EXCUSED }
    }

    val visible: List<Absence>
        get() = when (filter) {
            AbsenceFilter.ALL -> all
            AbsenceFilter.EXCUSED -> all.filter { it.status == AbsenceStatus.EXCUSED }
            AbsenceFilter.OPEN -> all.filter { it.status != AbsenceStatus.EXCUSED }
        }
}

/** Form state of the "report absence" sheet. */
data class ReportForm(
    val options: AbsenceReportOptions? = null,
    val loadingOptions: Boolean = false,
    val startDate: UntisDate = UntisDate.today(),
    val endDate: UntisDate = UntisDate.today(),
    val wholeDay: Boolean = true,
    val startTime: UntisTime = UntisTime(800),
    val endTime: UntisTime = UntisTime(1300),
    val reasonId: Int? = null,
    val text: String = "",
    val sending: Boolean = false,
    val error: Throwable? = null,
)

class AbsencesViewModel(
    private val repository: PeriodRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(AbsenceFilter.ALL)
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<Throwable?>(null)

    val state: StateFlow<AbsencesUiState> = combine(repository.absences(), filter, loading, error) { report, f, l, e ->
        AbsencesUiState(report, f, l, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AbsencesUiState(loading = true))

    private val form = MutableStateFlow<ReportForm?>(null)
    val reportForm: StateFlow<ReportForm?> = form

    private val sentEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sent: SharedFlow<Unit> = sentEvents

    init {
        refresh()
    }

    fun openReport() {
        form.value = ReportForm(loadingOptions = true)
        applyDayTimes(UntisDate.today(), UntisDate.today())
        viewModelScope.launch {
            try {
                val options = repository.absenceReportOptions()
                form.update { it?.copy(options = options, reasonId = options.defaultReasonId, loadingOptions = false) }
            } catch (e: PeriodException) {
                form.update { it?.copy(loadingOptions = false, error = e) }
            } catch (e: IllegalArgumentException) {
                form.update { it?.copy(loadingOptions = false, error = e) }
            }
        }
    }

    fun closeReport() {
        form.value = null
    }

    fun updateReport(change: (ReportForm) -> ReportForm) {
        val before = form.value ?: return
        val after = change(before)
        form.value = after
        if (after.wholeDay && (after.startDate != before.startDate || after.endDate != before.endDate || !before.wholeDay)) {
            applyDayTimes(after.startDate, after.endDate)
        }
    }

    /** "Whole day" = first lesson of the first day until the last lesson of the last day. */
    private fun applyDayTimes(start: UntisDate, end: UntisDate) {
        viewModelScope.launch {
            val first = repository.lessonsForDate(start).first().minByOrNull { it.start.raw }?.start
            val last = repository.lessonsForDate(end).first().maxByOrNull { it.end.raw }?.end
            form.update {
                if (it == null || !it.wholeDay) it else it.copy(startTime = first ?: UntisTime(745), endTime = last ?: UntisTime(1700))
            }
        }
    }

    fun sendReport() {
        val current = form.value ?: return
        val reasonId = current.reasonId ?: return
        if (current.sending) return
        viewModelScope.launch {
            form.update { it?.copy(sending = true, error = null) }
            try {
                repository.reportAbsence(
                    startDate = current.startDate,
                    startTime = current.startTime,
                    endDate = current.endDate,
                    endTime = current.endTime,
                    reasonId = reasonId,
                    text = current.text.trim(),
                )
                form.value = null
                sentEvents.tryEmit(Unit)
            } catch (e: PeriodException) {
                form.update { it?.copy(sending = false, error = e) }
            } catch (e: IllegalArgumentException) {
                form.update { it?.copy(sending = false, error = e) }
            }
        }
    }

    fun setFilter(value: AbsenceFilter) {
        filter.value = value
    }

    fun refresh() {
        if (loading.value) return
        viewModelScope.launch {
            loading.value = true
            error.value = null
            try {
                repository.refreshAbsences()
            } catch (e: PeriodException) {
                error.value = e
            } catch (e: IllegalArgumentException) {
                // Unexpected JSON from the server (SerializationException).
                error.value = e
            } finally {
                loading.value = false
            }
        }
    }
}
