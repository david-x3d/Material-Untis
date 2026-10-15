package dev.x3d.dayline.data.rpc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

const val CLIENT_ID = "MATERIALUNTIS"

@Serializable
data class JsonRpcRequest(
    val id: String,
    val jsonrpc: String = "2.0",
    val method: String,
    val params: JsonElement,
)

@Serializable
data class JsonRpcResponse(
    val id: String? = null,
    val jsonrpc: String? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null,
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String? = null,
    val data: JsonElement? = null,
)

@Serializable
data class AuthenticateParams(
    val user: String,
    val password: String,
    val client: String = CLIENT_ID,
)

@Serializable
data class AuthenticateResult(
    val sessionId: String,
    val personType: Int,
    val personId: Int,
    val klasseId: Int? = null,
)

@Serializable
data class PeriodDto(
    val id: Long,
    val date: Int,
    val startTime: Int,
    val endTime: Int,
    val code: String? = null,
    val lstype: String? = null,
    @SerialName("lsType") val lsTypeAlt: String? = null,
    val info: String? = null,
    val substText: String? = null,
    val lstext: String? = null,
    val activityType: String? = null,
    val su: List<ElementDto> = emptyList(),
    val te: List<ElementDto> = emptyList(),
    val ro: List<ElementDto> = emptyList(),
    val kl: List<ElementDto> = emptyList(),
)

@Serializable
data class ElementDto(
    val id: Int? = null,
    val name: String? = null,
    val longname: String? = null,
    val orgid: Int? = null,
    val orgname: String? = null,
)

@Serializable
data class TimegridDayDto(
    val day: Int,
    val timeUnits: List<TimeUnitDto> = emptyList(),
)

@Serializable
data class TimeUnitDto(
    val name: String? = null,
    val startTime: Int,
    val endTime: Int,
)

@Serializable
data class SchoolSearchEnvelope(
    val result: SchoolSearchResult? = null,
    val error: JsonRpcError? = null,
)

@Serializable
data class SchoolSearchResult(
    val schools: List<SchoolDto> = emptyList(),
)

@Serializable
data class SchoolDto(
    val server: String? = null,
    val displayName: String? = null,
    val loginName: String? = null,
    val address: String? = null,
    val schoolId: Long? = null,
    val serverUrl: String? = null,
)

@Serializable
data class TimetableOptions(
    val options: TimetableOptionsInner,
)

@Serializable
data class TimetableOptionsInner(
    val element: TimetableElement,
    val startDate: Int,
    val endDate: Int,
    val showInfo: Boolean = true,
    val showSubstText: Boolean = true,
    val showLsText: Boolean = true,
    val showBooking: Boolean = true,
    val classFields: List<String> = listOf("id", "name", "longname"),
    val roomFields: List<String> = listOf("id", "name", "longname"),
    val subjectFields: List<String> = listOf("id", "name", "longname"),
    val teacherFields: List<String> = listOf("id", "name", "longname"),
)

@Serializable
data class TimetableElement(
    val id: Int,
    val type: Int,
)

/** `getHolidays`: one entry per holiday period of the school, e.g. "Herbst" / "Herbstferien". */
@Serializable
data class HolidayDto(
    val id: Int? = null,
    val name: String? = null,
    val longName: String? = null,
    val startDate: Int,
    val endDate: Int,
)

@Serializable
data class SchoolYearDto(
    val id: Int? = null,
    val name: String? = null,
    val startDate: Int,
    val endDate: Int,
)

/** `GET /WebUntis/api/classreg/absences/students` → `{"data": {"absences": [...]}}` */
@Serializable
data class AbsencesEnvelope(
    val data: AbsencesData? = null,
)

@Serializable
data class AbsencesData(
    val absences: List<AbsenceDto> = emptyList(),
)

@Serializable
data class AbsenceDto(
    val id: Long = 0,
    val startDate: Int,
    val endDate: Int,
    val startTime: Int = 0,
    val endTime: Int = 0,
    // These are loosely typed on purpose: servers differ (string vs. object vs. null).
    val reason: JsonElement? = null,
    val text: JsonElement? = null,
    val excuseStatus: JsonElement? = null,
    val isExcused: Boolean = false,
    val excuse: ExcuseDto? = null,
)

@Serializable
data class ExcuseDto(
    val id: Long? = null,
    val text: JsonElement? = null,
    val excuseStatus: JsonElement? = null,
    val isExcused: Boolean = false,
)

/** A person the web user may act for (`grupet.loginServiceConfig.user.persons` in embedded.do). */
@Serializable
data class WebPersonDto(
    val id: Int,
    val type: Int,
    val displayName: String? = null,
)

/** `GET /WebUntis/api/classreg/absences/meta` → `{"data": {...}}` */
@Serializable
data class AbsenceMetaEnvelope(
    val data: AbsenceMetaDto? = null,
)

@Serializable
data class AbsenceMetaDto(
    val absenceReasons: List<AbsenceReasonDto> = emptyList(),
    val defaultAbsenceReason: AbsenceReasonDto? = null,
    val canReportAbsence: Boolean = false,
    val isAbsenceReasonAndTextHidden: Boolean = false,
    val defaultStartTime: Int? = null,
    val defaultEndTime: Int? = null,
)

@Serializable
data class AbsenceReasonDto(
    val id: Int,
    val name: String = "",
)

/** Body of `POST /WebUntis/api/classreg/absences/students/self` (same as the web app sends). */
@Serializable
data class ReportAbsenceRequest(
    val startDate: Int,
    val startTime: Int,
    val endDate: Int,
    val endTime: Int,
    val text: String,
    val reasonId: Int,
    val studentId: Int,
)
