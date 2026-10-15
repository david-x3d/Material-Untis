package dev.x3d.dayline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.data.prefs.UserPrefs
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.NamedRef
import dev.x3d.dayline.domain.model.SubjectColors
import dev.x3d.dayline.domain.model.SyncState
import dev.x3d.dayline.domain.model.UserSession
import dev.x3d.dayline.domain.model.WatchStatus
import dev.x3d.dayline.sync.SyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val session: UserSession? = null,
    val interval: Int = 15,
    val watch: WatchStatus = WatchStatus(),
    val sync: SyncState = SyncState(),
    val dynamicColor: Boolean = false,
    val subjectColors: SubjectColors = SubjectColors(),
    val subjects: List<NamedRef> = emptyList(),
)

class SettingsViewModel(
    private val repository: PeriodRepository,
    private val syncScheduler: SyncScheduler,
    private val prefs: UserPrefs,
) : ViewModel() {
    private val base = combine(
        repository.session,
        repository.syncIntervalMinutes(),
        repository.watchStatus,
        repository.syncState,
    ) { session, interval, watch, sync -> SettingsUiState(session, interval, watch, sync) }

    val state: StateFlow<SettingsUiState> = combine(
        base,
        prefs.dynamicColor,
        prefs.subjectColors,
        repository.subjects(),
    ) { state, dynamic, colors, subjects ->
        state.copy(dynamicColor = dynamic, subjectColors = colors, subjects = subjects)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(session = repository.session.value))

    fun setInterval(minutes: Int) {
        viewModelScope.launch {
            repository.setSyncIntervalMinutes(minutes)
            syncScheduler.schedulePeriodic(minutes)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefs.setDynamicColor(enabled) }
    }

    fun setCustomColors(custom: Boolean) = updateColors { it.copy(custom = custom) }

    /** New random auto colours. */
    fun reroll() = updateColors { it.copy(seed = it.seed + 1) }

    fun pickColor(subject: String, argb: Int) = updateColors { it.copy(custom = true, picked = it.picked + (subject to argb)) }

    fun resetColor(subject: String) = updateColors { it.copy(picked = it.picked - subject) }

    private fun updateColors(change: (SubjectColors) -> SubjectColors) {
        viewModelScope.launch {
            prefs.setSubjectColors(change(state.value.subjectColors))
            // Re-send today's plan so the watch shows the new colours too.
            runCatching { repository.refresh(force = false) }
        }
    }

    /** Fetches the timetable now and sends it to the watch. */
    fun syncNow() {
        viewModelScope.launch { runCatching { repository.refresh(force = true) } }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            syncScheduler.cancel()
            repository.logout()
            onDone()
        }
    }
}
