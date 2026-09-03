package dev.x3d.dayline.data.mapper

import dev.x3d.dayline.data.rpc.ElementDto
import dev.x3d.dayline.data.rpc.PeriodDto
import dev.x3d.dayline.domain.model.LessonStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonMapperTest {
    @Test
    fun missingCodeIsNormal() {
        val lesson = LessonMapper.toDomain(basePeriod(code = null))
        assertEquals(LessonStatus.NORMAL, lesson.status)
        assertFalse(lesson.isCancelled)
        assertFalse(lesson.isIrregular)
    }

    @Test
    fun cancelledMaps() {
        val lesson = LessonMapper.toDomain(basePeriod(code = "cancelled"))
        assertEquals(LessonStatus.CANCELLED, lesson.status)
        assertTrue(lesson.isCancelled)
    }

    @Test
    fun irregularMaps() {
        val lesson = LessonMapper.toDomain(basePeriod(code = "irregular", subst = "Covered by C. Diaz"))
        assertEquals(LessonStatus.IRREGULAR, lesson.status)
        assertTrue(lesson.isIrregular)
        assertEquals("Covered by C. Diaz", lesson.substText)
    }

    private fun basePeriod(code: String?, subst: String? = null) = PeriodDto(
        id = 1,
        date = 20260903,
        startTime = 800,
        endTime = 845,
        code = code,
        substText = subst,
        su = listOf(ElementDto(1, "M", "Mathematics")),
        te = listOf(ElementDto(10, "AB", "A. Baker")),
        ro = listOf(ElementDto(3, "101", "Room 101")),
        kl = listOf(ElementDto(12, "9a", "Class 9a")),
    )
}
