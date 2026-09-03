package dev.x3d.dayline.domain.model

import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime

data class School(
    val displayName: String,
    val loginName: String,
    val host: String,
    val address: String = "",
)

data class NamedRef(
    val id: Int,
    val shortName: String,
    val longName: String,
    val originalShortName: String? = null,
)

enum class LessonStatus {
    NORMAL,
    CANCELLED,
    IRREGULAR,
}

enum class LessonType {
    LESSON,
    OFFICE_HOUR,
    STANDBY,
    BREAK_SUPERVISION,
    EXAM,
    UNKNOWN,
}

data class Lesson(
    val id: Long,
    val date: UntisDate,
    val start: UntisTime,
    val end: UntisTime,
    val subject: NamedRef?,
    val teacher: NamedRef?,
    val room: NamedRef?,
    val klass: NamedRef?,
    val status: LessonStatus,
    val type: LessonType,
    val info: String?,
    val substText: String?,
    val lessonText: String?,
) {
    val subjectLabel: String get() = subject?.longName?.ifBlank { subject.shortName } ?: "—"
    val subjectShort: String get() = subject?.shortName?.ifBlank { subjectLabel } ?: "—"
    val roomLabel: String get() = room?.shortName?.ifBlank { room?.longName.orEmpty() } ?: "—"
    val teacherLabel: String get() = teacher?.shortName ?: "—"
    val isCancelled: Boolean get() = status == LessonStatus.CANCELLED
    val isIrregular: Boolean get() = status == LessonStatus.IRREGULAR
}

data class TimegridUnit(
    val name: String,
    val start: UntisTime,
    val end: UntisTime,
)

data class TimegridDay(
    val weekday: Int,
    val units: List<TimegridUnit>,
)

data class UserSession(
    val host: String,
    val school: String,
    val schoolDisplayName: String,
    val user: String,
    val personType: Int,
    val personId: Int,
    val klasseId: Int?,
) {
    val isStudent: Boolean get() = personType == PERSON_STUDENT

    companion object {
        const val PERSON_TEACHER = 2
        const val PERSON_STUDENT = 5
    }
}

enum class AuthKind {
    PASSWORD,
    SECRET,
}

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSuccessAt: Long? = null,
    val lastError: String? = null,
    val offline: Boolean = false,
    val fromCache: Boolean = false,
)

data class WatchStatus(
    val connected: Boolean = false,
    val lastPushedAt: Long? = null,
    val message: String? = null,
)
