package dev.x3d.dayline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.domain.PeriodRepository
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
)

class SettingsViewModel(
    private val repository: PeriodRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        repository.session,
        repository.syncIntervalMinutes(),
        repository.watchStatus,
        repository.syncState,
    ) { session, interval, watch, sync ->
        SettingsUiState(session, interval, watch, sync)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(session = repository.session.value))

    fun setInterval(minutes: Int) {
        viewModelScope.launch {
            repository.setSyncIntervalMinutes(minutes)
            syncScheduler.schedulePeriodic(minutes)
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
