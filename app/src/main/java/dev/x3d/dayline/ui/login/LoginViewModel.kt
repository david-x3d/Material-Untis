package dev.x3d.dayline.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val user: String = "",
    val password: String = "",
    val secret: String = "",
    val useSecret: Boolean = false,
    val loading: Boolean = false,
    val error: Boolean = false,
    val schoolLabel: String = "",
)

class LoginViewModel(
    private val repository: PeriodRepository,
    private val syncScheduler: SyncScheduler,
    private val qrDraftStore: QrDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        LoginUiState(schoolLabel = repository.selectedSchool.value?.displayName.orEmpty()),
    )
    val state: StateFlow<LoginUiState> = _state

    init {
        qrDraftStore.latest?.let { applyQr(it.user, it.secret) }
    }

    fun consumeQr() {
        val draft = qrDraftStore.latest ?: return
        qrDraftStore.latest = null
        viewModelScope.launch {
            repository.saveSchool(draft.school)
            applyQr(draft.user, draft.secret)
            _state.update { it.copy(schoolLabel = draft.school.displayName) }
        }
    }

    fun onUser(value: String) = _state.update { it.copy(user = value, error = false) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = false) }
    fun onSecret(value: String) = _state.update { it.copy(secret = value, error = false) }
    fun setUseSecret(value: Boolean) = _state.update { it.copy(useSecret = value, error = false) }

    fun applyQr(user: String, secret: String) {
        _state.update { it.copy(user = user, secret = secret, useSecret = true, error = false) }
    }

    fun submit(onDone: () -> Unit) {
        val current = _state.value
        if (current.user.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false) }
            try {
                if (current.useSecret) {
                    repository.loginSecret(current.user.trim(), current.secret.trim())
                } else {
                    repository.loginPassword(current.user.trim(), current.password)
                }
                syncScheduler.schedulePeriodic(15)
                syncScheduler.runNow()
                _state.update { it.copy(loading = false) }
                onDone()
            } catch (_: PeriodException) {
                _state.update { it.copy(loading = false, error = true) }
            }
        }
    }
}
