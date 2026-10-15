package dev.x3d.dayline.ui.absences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.Absence
import dev.x3d.dayline.domain.model.AbsenceReport
import dev.x3d.dayline.domain.model.AbsenceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

class AbsencesViewModel(
    private val repository: PeriodRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(AbsenceFilter.ALL)
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<Throwable?>(null)

    val state: StateFlow<AbsencesUiState> = combine(repository.absences(), filter, loading, error) { report, f, l, e ->
        AbsencesUiState(report, f, l, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AbsencesUiState(loading = true))

    init {
        refresh()
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
