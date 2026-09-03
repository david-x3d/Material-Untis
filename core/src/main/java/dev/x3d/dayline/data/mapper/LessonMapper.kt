package dev.x3d.dayline.data.mapper

import dev.x3d.dayline.data.db.LessonEntity
import dev.x3d.dayline.data.rpc.ElementDto
import dev.x3d.dayline.data.rpc.PeriodDto
import dev.x3d.dayline.data.rpc.TimegridDayDto
import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.model.LessonStatus
import dev.x3d.dayline.domain.model.LessonType
import dev.x3d.dayline.domain.model.NamedRef
import dev.x3d.dayline.domain.model.TimegridDay
import dev.x3d.dayline.domain.model.TimegridUnit
import dev.x3d.dayline.domain.model.WatchPayload
import dev.x3d.dayline.domain.model.WatchPeriod
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime

object LessonMapper {
    fun statusOf(code: String?): LessonStatus = when (code?.lowercase()) {
        "cancelled" -> LessonStatus.CANCELLED
        "irregular" -> LessonStatus.IRREGULAR
        else -> LessonStatus.NORMAL
    }

    fun typeOf(lstype: String?): LessonType = when (lstype?.lowercase()) {
        "ls", null, "" -> LessonType.LESSON
        "oh" -> LessonType.OFFICE_HOUR
        "sb" -> LessonType.STANDBY
        "bs" -> LessonType.BREAK_SUPERVISION
        "ex" -> LessonType.EXAM
        else -> LessonType.UNKNOWN
    }

    fun toDomain(dto: PeriodDto): Lesson {
        val lstype = dto.lstype ?: dto.lsTypeAlt
        return Lesson(
            id = dto.id,
            date = UntisDate(dto.date),
            start = UntisTime.parse(dto.startTime),
            end = UntisTime.parse(dto.endTime),
            subject = dto.su.firstOrNull()?.toNamedRef(),
            teacher = dto.te.firstOrNull()?.toNamedRef(),
            room = dto.ro.firstOrNull()?.toNamedRef(),
            klass = dto.kl.firstOrNull()?.toNamedRef(),
            status = statusOf(dto.code),
            type = typeOf(lstype),
            info = dto.info?.trim()?.takeIf { it.isNotEmpty() },
            substText = dto.substText?.trim()?.takeIf { it.isNotEmpty() },
            lessonText = dto.lstext?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    fun toEntity(lesson: Lesson): LessonEntity = LessonEntity(
        id = lesson.id,
        date = lesson.date.yyyymmdd,
        startTime = lesson.start.raw,
        endTime = lesson.end.raw,
        subjectId = lesson.subject?.id ?: 0,
        subjectShort = lesson.subject?.shortName.orEmpty(),
        subjectLong = lesson.subject?.longName.orEmpty(),
        teacherId = lesson.teacher?.id ?: 0,
        teacherShort = lesson.teacher?.shortName.orEmpty(),
        teacherLong = lesson.teacher?.longName.orEmpty(),
        teacherOriginal = lesson.teacher?.originalShortName,
        roomId = lesson.room?.id ?: 0,
        roomShort = lesson.room?.shortName.orEmpty(),
        roomLong = lesson.room?.longName.orEmpty(),
        classId = lesson.klass?.id ?: 0,
        classShort = lesson.klass?.shortName.orEmpty(),
        status = lesson.status.name,
        type = lesson.type.name,
        info = lesson.info,
        substText = lesson.substText,
        lessonText = lesson.lessonText,
    )

    fun toDomain(entity: LessonEntity): Lesson = Lesson(
        id = entity.id,
        date = UntisDate(entity.date),
        start = UntisTime.parse(entity.startTime),
        end = UntisTime.parse(entity.endTime),
        subject = entity.subjectShort.takeIf { it.isNotEmpty() }?.let {
            NamedRef(entity.subjectId, it, entity.subjectLong.ifEmpty { it })
        },
        teacher = entity.teacherShort.takeIf { it.isNotEmpty() }?.let {
            NamedRef(entity.teacherId, it, entity.teacherLong.ifEmpty { it }, entity.teacherOriginal)
        },
        room = entity.roomShort.takeIf { it.isNotEmpty() }?.let {
            NamedRef(entity.roomId, it, entity.roomLong.ifEmpty { it })
        },
        klass = entity.classShort.takeIf { it.isNotEmpty() }?.let {
            NamedRef(entity.classId, it, it)
        },
        status = runCatching { LessonStatus.valueOf(entity.status) }.getOrDefault(LessonStatus.NORMAL),
        type = runCatching { LessonType.valueOf(entity.type) }.getOrDefault(LessonType.LESSON),
        info = entity.info,
        substText = entity.substText,
        lessonText = entity.lessonText,
    )

    fun toTimegrid(dto: TimegridDayDto): TimegridDay = TimegridDay(
        weekday = dto.day,
        units = dto.timeUnits.map {
            TimegridUnit(
                name = it.name.orEmpty(),
                start = UntisTime.parse(it.startTime),
                end = UntisTime.parse(it.endTime),
            )
        },
    )

    fun toWatchPayload(date: UntisDate, syncedAt: Long, lessons: List<Lesson>): WatchPayload =
        WatchPayload(
            date = date.yyyymmdd,
            syncedAt = syncedAt,
            periods = lessons.sortedBy { it.start.raw }.map { lesson ->
                WatchPeriod(
                    subject = lesson.subjectShort,
                    room = lesson.roomLabel,
                    teacher = lesson.teacherLabel,
                    start = lesson.start.raw,
                    end = lesson.end.raw,
                    status = when (lesson.status) {
                        LessonStatus.CANCELLED -> "cancelled"
                        LessonStatus.IRREGULAR -> "irregular"
                        LessonStatus.NORMAL -> "normal"
                    },
                    info = listOfNotNull(lesson.substText, lesson.info).joinToString(" · "),
                )
            },
        )

    private fun ElementDto.toNamedRef(): NamedRef? {
        val shortName = name.orEmpty()
        val longName = longname.orEmpty().ifEmpty { shortName }
        if (shortName.isEmpty() && longName.isEmpty()) return null
        return NamedRef(
            id = id ?: 0,
            shortName = shortName.ifEmpty { longName },
            longName = longName,
            originalShortName = orgname,
        )
    }
}
