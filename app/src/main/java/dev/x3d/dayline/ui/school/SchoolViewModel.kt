package dev.x3d.dayline.ui.school

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.data.rpc.WebUntisClient
import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.School
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SchoolUiState(
    val query: String = "",
    val host: String = "",
    val schoolName: String = "",
    val results: List<School> = emptyList(),
    val searching: Boolean = false,
    val error: String? = null,
)

class SchoolViewModel(
    private val repository: PeriodRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SchoolUiState())
    val state: StateFlow<SchoolUiState> = _state
    private var searchJob: Job? = null

    fun onQuery(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            if (value.trim().length < 2) {
                _state.update { it.copy(results = emptyList(), searching = false, error = null) }
                return@launch
            }
            _state.update { it.copy(searching = true, error = null) }
            try {
                val results = repository.searchSchools(value)
                _state.update { it.copy(results = results, searching = false) }
            } catch (_: PeriodException) {
                _state.update { it.copy(searching = false, results = emptyList(), error = "search") }
            }
        }
    }

    fun onHost(value: String) = _state.update { it.copy(host = value) }
    fun onSchoolName(value: String) = _state.update { it.copy(schoolName = value) }

    fun select(school: School, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.saveSchool(school)
            onDone()
        }
    }

    fun saveManual(onDone: () -> Unit) {
        val host = WebUntisClient.normalizeHost(_state.value.host)
        val name = _state.value.schoolName.trim()
        if (host.isBlank() || name.isBlank()) return
        select(School(displayName = name, loginName = name, host = host), onDone)
    }
}
