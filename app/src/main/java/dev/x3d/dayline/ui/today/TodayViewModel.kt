package dev.x3d.dayline.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.model.LessonStatus
import dev.x3d.dayline.domain.model.SyncState
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val lessons: List<Lesson> = emptyList(),
    val next: Lesson? = null,
    val sync: SyncState = SyncState(),
    val now: UntisTime = UntisTime.now(),
)

class TodayViewModel(
    private val repository: PeriodRepository,
) : ViewModel() {
    /** Re-evaluated every 30 s so countdowns move and the day rolls over at midnight. */
    private val clock = flow {
        while (true) {
            emit(UntisDate.today())
            delay(30_000)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TodayUiState> = combine(
        clock.distinctUntilChanged().flatMapLatest { repository.lessonsForDate(it) },
        repository.syncState,
        clock,
    ) { lessons, sync, _ ->
        val now = UntisTime.now()
        TodayUiState(
            lessons = lessons.sortedBy { it.start.raw },
            next = nextLesson(lessons, now),
            sync = sync,
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun refresh() {
        viewModelScope.launch { runCatching { repository.refresh(force = true) } }
    }

    init {
        viewModelScope.launch { runCatching { repository.refresh(force = false) } }
    }

    companion object {
        fun nextLesson(lessons: List<Lesson>, now: UntisTime): Lesson? {
            val active = lessons.filter { it.status != LessonStatus.CANCELLED }
            // The running lesson first, then the next one.
            return active.firstOrNull { it.start <= now && it.end > now }
                ?: active.firstOrNull { it.start > now }
        }
    }
}
