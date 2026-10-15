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
    val accountType: AccountType = AccountType.fromPersonType(personType),
    /** The student whose timetable and absences are shown (the child, for parent accounts). */
    val studentId: Int? = if (personType == PERSON_STUDENT) personId else null,
    val studentName: String? = null,
) {
    val isStudent: Boolean get() = personType == PERSON_STUDENT

    /** Element for getTimetable: the linked student if there is one, else the account itself. */
    val timetableElement: Pair<Int, Int>
        get() = if (accountType == AccountType.PARENT && studentId != null) studentId to PERSON_STUDENT else personId to personType

    companion object {
        const val PERSON_TEACHER = 2
        const val PERSON_STUDENT = 5
        const val PERSON_PARENT = 12
    }
}

enum class AccountType {
    STUDENT,
    PARENT,
    TEACHER,
    ;

    companion object {
        fun fromPersonType(personType: Int): AccountType = when (personType) {
            UserSession.PERSON_TEACHER -> TEACHER
            UserSession.PERSON_PARENT -> PARENT
            else -> STUDENT
        }
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

data class SchoolYear(
    val name: String,
    val start: UntisDate,
    val end: UntisDate,
)

enum class AbsenceStatus {
    EXCUSED,
    UNEXCUSED,
    OPEN,
}

data class Absence(
    val id: Long,
    val startDate: UntisDate,
    val endDate: UntisDate,
    val start: UntisTime,
    val end: UntisTime,
    val reason: String?,
    val text: String?,
    val status: AbsenceStatus,
    /** Status text as the school wrote it, e.g. "entschuldigt". */
    val statusLabel: String?,
)

data class AbsenceReason(val id: Int, val name: String)

/** What the school allows for reporting absences, from `absences/meta`. */
data class AbsenceReportOptions(
    val canReport: Boolean,
    val reasons: List<AbsenceReason>,
    val defaultReasonId: Int?,
    val reasonAndTextHidden: Boolean,
)

data class AbsenceReport(
    val schoolYear: SchoolYear,
    val absences: List<Absence>,
    val fetchedAt: Long,
)
