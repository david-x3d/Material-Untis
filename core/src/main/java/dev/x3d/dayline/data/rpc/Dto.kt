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
