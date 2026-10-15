package dev.x3d.dayline.data.mapper

import dev.x3d.dayline.data.rpc.AbsenceDto
import dev.x3d.dayline.domain.model.Absence
import dev.x3d.dayline.domain.model.AbsenceStatus
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object AbsenceMapper {
    fun toDomain(dto: AbsenceDto): Absence {
        val statusLabel = dto.excuseStatus.text() ?: dto.excuse?.excuseStatus.text()
        val excused = dto.isExcused || dto.excuse?.isExcused == true
        return Absence(
            id = dto.id,
            startDate = UntisDate(dto.startDate),
            endDate = UntisDate(maxOf(dto.endDate, dto.startDate)),
            start = UntisTime.parse(dto.startTime),
            end = UntisTime.parse(dto.endTime),
            reason = dto.reason.text(),
            text = dto.text.text() ?: dto.excuse?.text.text(),
            status = when {
                excused -> AbsenceStatus.EXCUSED
                statusLabel == null -> AbsenceStatus.OPEN
                else -> AbsenceStatus.UNEXCUSED
            },
            statusLabel = statusLabel,
        )
    }

    /** Newest first, like the WebUntis list. */
    fun sort(absences: List<Absence>): List<Absence> =
        absences.sortedWith(compareByDescending<Absence> { it.startDate }.thenByDescending { it.start })

    /** Strings, or objects like `{"name": "..."}` / `{"longName": "..."}`. */
    private fun JsonElement?.text(): String? {
        val value = when (this) {
            is JsonPrimitive -> if (isString) content else null
            is JsonObject -> listOf("longName", "longname", "name", "label", "text")
                .firstNotNullOfOrNull { key -> (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content }
            else -> null
        }
        return value?.trim()?.takeIf { it.isNotEmpty() }
    }
}
