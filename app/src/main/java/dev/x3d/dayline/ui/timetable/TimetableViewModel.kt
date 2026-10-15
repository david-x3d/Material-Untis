package dev.x3d.dayline.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.LessonBlock
import dev.x3d.dayline.domain.model.UserSession
import dev.x3d.dayline.domain.time.UntisDate
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeekData(
    val monday: UntisDate,
    val days: List<UntisDate>,
    val blocksByDay: Map<UntisDate, List<PlacedBlock>>,
    val axis: TimeAxis,
) {
    val isEmpty: Boolean get() = blocksByDay.values.all { it.isEmpty() }
}

class TimetableViewModel(
    private val repository: PeriodRepository,
) : ViewModel() {
    val session: StateFlow<UserSession?> = repository.session

    private val loadedWeeks = mutableSetOf<UntisDate>()
    private val loadingWeeks = MutableStateFlow<Set<UntisDate>>(emptySet())
    val loading: StateFlow<Set<UntisDate>> = loadingWeeks.asStateFlow()

    private val errorEvents = MutableSharedFlow<Throwable>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val errors: SharedFlow<Throwable> = errorEvents

    fun mondayFor(offset: Int, today: LocalDate = LocalDate.now()): UntisDate =
        UntisDate.from(today.with(DayOfWeek.MONDAY).plusWeeks(offset.toLong()))

    fun week(monday: UntisDate): Flow<WeekData> =
        combine(repository.lessonsInRange(monday, monday.plusDays(6)), repository.timegrid()) { lessons, grid ->
            val byDay = lessons.groupBy { it.date }
            // Mon–Fri always; weekend days only when they have lessons.
            val days = (0L..6L).map { monday.plusDays(it) }.filterIndexed { index, day ->
                index < 5 || byDay[day].orEmpty().isNotEmpty()
            }
            WeekData(
                monday = monday,
                days = days,
                blocksByDay = days.associateWith { day -> placeLanes(LessonBlock.merge(byDay[day].orEmpty())) },
                axis = TimeAxis(buildSlots(grid, lessons)),
            )
        }

    /** Loads a week once per screen visit; the cache is shown meanwhile. */
    fun ensureLoaded(monday: UntisDate) {
        if (loadedWeeks.add(monday)) load(monday)
    }

    fun refresh(monday: UntisDate) = load(monday)

    private fun load(monday: UntisDate) {
        if (monday in loadingWeeks.value) return
        viewModelScope.launch {
            loadingWeeks.update { it + monday }
            try {
                if (monday == mondayFor(0)) {
                    // Also updates sync state and the watch.
                    repository.refresh(force = true)
                } else {
                    repository.loadWeek(monday)
                }
            } catch (e: PeriodException) {
                loadedWeeks.remove(monday)
                errorEvents.tryEmit(e)
            } finally {
                loadingWeeks.update { it - monday }
            }
        }
    }
}
