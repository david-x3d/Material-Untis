package dev.x3d.dayline.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.Lesson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class LessonViewModel(
    private val repository: PeriodRepository,
) : ViewModel() {
    private val id = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val lesson: StateFlow<Lesson?> = id.flatMapLatest { value ->
        if (value == null) flowOf(null) else repository.lesson(value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setId(value: Long) {
        id.value = value
    }
}
