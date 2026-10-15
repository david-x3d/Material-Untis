package dev.x3d.dayline.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.LessonBlock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LessonViewModel(
    private val repository: PeriodRepository,
) : ViewModel() {
    private val id = MutableStateFlow<Long?>(null)

    /** The whole block (e.g. both halves of a double period) that contains the lesson. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val block: StateFlow<LessonBlock?> = id.flatMapLatest { value ->
        if (value == null) {
            flowOf(null)
        } else {
            repository.lesson(value).flatMapLatest { lesson ->
                if (lesson == null) {
                    flowOf(null)
                } else {
                    repository.lessonsForDate(lesson.date).map { day ->
                        LessonBlock.merge(day).firstOrNull { value in it.ids } ?: LessonBlock(listOf(lesson))
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setId(value: Long) {
        id.value = value
    }
}
