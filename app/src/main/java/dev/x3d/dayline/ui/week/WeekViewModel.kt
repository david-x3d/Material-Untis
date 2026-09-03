package dev.x3d.dayline.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.time.UntisDate
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class WeekUiState(
    val days: List<UntisDate> = emptyList(),
    val lessonsByDate: Map<Int, List<Lesson>> = emptyMap(),
)

class WeekViewModel(
    repository: PeriodRepository,
) : ViewModel() {
    private val range = UntisDate.weekRange()

    val state: StateFlow<WeekUiState> = repository.lessonsInRange(range.first, range.second)
        .map { lessons ->
            val days = (0L..6L).map { range.first.plusDays(it) }
            WeekUiState(
                days = days,
                lessonsByDate = lessons.groupBy { it.date.yyyymmdd },
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WeekUiState(days = (0L..6L).map { range.first.plusDays(it) }),
        )
}
