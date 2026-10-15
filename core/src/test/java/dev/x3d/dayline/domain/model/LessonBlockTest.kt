package dev.x3d.dayline.domain.model

import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import org.junit.Assert.assertEquals
import org.junit.Test

class LessonBlockTest {
    @Test
    fun doublePeriodIsJoined() {
        val blocks = LessonBlock.merge(listOf(lesson(2, 835, 920), lesson(1, 750, 835)))
        assertEquals(1, blocks.size)
        assertEquals(UntisTime(750), blocks[0].start)
        assertEquals(UntisTime(920), blocks[0].end)
        assertEquals(listOf(1L, 2L), blocks[0].ids)
    }

    @Test
    fun longBreakOrDifferentRoomSplits() {
        val afterBreak = LessonBlock.merge(listOf(lesson(1, 750, 920), lesson(2, 940, 1025)))
        assertEquals(2, afterBreak.size)
        val otherRoom = LessonBlock.merge(listOf(lesson(1, 750, 835), lesson(2, 835, 920, room = "PC")))
        assertEquals(2, otherRoom.size)
    }

    @Test
    fun parallelLessonsStaySeparate() {
        val blocks = LessonBlock.merge(listOf(lesson(1, 940, 1025), lesson(2, 940, 1025, subject = "EK")))
        assertEquals(2, blocks.size)
    }

    private fun lesson(id: Long, start: Int, end: Int, subject: String = "M", room: String = "A07") = Lesson(
        id = id,
        date = UntisDate(20261015),
        start = UntisTime(start),
        end = UntisTime(end),
        subject = NamedRef(1, subject, subject),
        teacher = NamedRef(2, "RO", "RO"),
        room = NamedRef(3, room, room),
        klass = null,
        status = LessonStatus.NORMAL,
        type = LessonType.LESSON,
        info = null,
        substText = null,
        lessonText = null,
    )
}
