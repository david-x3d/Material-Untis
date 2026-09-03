package dev.x3d.dayline.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test

class UntisDateTimeTest {
    @Test
    fun parsesTimes() {
        assertEquals(0, UntisTime.parse(7).hour)
        assertEquals(7, UntisTime.parse(7).minute)
        assertEquals("00:07", UntisTime.parse(7).format())

        assertEquals(7, UntisTime.parse(720).hour)
        assertEquals(20, UntisTime.parse(720).minute)
        assertEquals("07:20", UntisTime.parse(720).format())

        assertEquals(13, UntisTime.parse(1345).hour)
        assertEquals(45, UntisTime.parse(1345).minute)
        assertEquals("13:45", UntisTime.parse(1345).format())

        assertEquals(9, UntisTime.parse(930).hour)
        assertEquals(30, UntisTime.parse(930).minute)
        assertEquals("09:30", UntisTime.parse(930).format())
    }

    @Test
    fun parsesDates() {
        val date = UntisDate(20260903)
        assertEquals(2026, date.year)
        assertEquals(9, date.month)
        assertEquals(3, date.day)
        assertEquals(20260904, date.plusDays(1).yyyymmdd)
    }

    @Test
    fun minutesOfDay() {
        assertEquals(7 * 60 + 20, UntisTime.parse(720).toMinutesOfDay())
        assertEquals(13 * 60 + 45, UntisTime.parse(1345).toMinutesOfDay())
    }
}
