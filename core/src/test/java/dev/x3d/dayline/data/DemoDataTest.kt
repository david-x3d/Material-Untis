package dev.x3d.dayline.data

import dev.x3d.dayline.data.repo.DemoData
import dev.x3d.dayline.data.repo.FakePeriodRepository
import dev.x3d.dayline.domain.model.*
import dev.x3d.dayline.domain.time.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DemoDataTest {
    @Test fun showcaseHasEveryStatusAndChangeAndHolidays() {
        val today = UntisDate(20261104)
        val lessons = DemoData.lessons(today)
        assertEquals(lessons.size, lessons.map { it.id }.distinct().size)
        assertEquals(LessonStatus.entries.toSet(), lessons.map { it.status }.toSet())
        assertTrue(lessons.any { it.type == LessonType.EXAM })
        assertTrue(lessons.any { it.subject?.originalShortName != null })
        assertTrue(lessons.any { it.teacher?.originalShortName != null })
        assertTrue(lessons.any { it.room?.originalShortName != null })
        assertTrue(lessons.any { it.subject?.shortName == "VA" })
        assertTrue(lessons.any { it.date == today })
        val holiday = DemoData.holidays(today).single()
        assertEquals(UntisDate(20261109), holiday.start)
        assertFalse(lessons.any { it.date in holiday })
    }

    @Test fun demoReportsAppearAndSurviveRefresh() = runBlocking {
        val repository = FakePeriodRepository()
        repository.refreshAbsences()
        val before = repository.absences().first()!!
        assertEquals(AbsenceStatus.entries.toSet(), before.absences.map { it.status }.toSet())
        val date = UntisDate.today()
        repository.reportAbsence(date, UntisTime(800), date.plusDays(1), UntisTime(1300), 1, "Eigene Demo-Meldung")
        repository.refreshAbsences()
        val after = repository.absences().first()!!
        assertEquals(before.absences.size + 1, after.absences.size)
        assertEquals("Eigene Demo-Meldung", after.absences.first().text)
        assertEquals(AbsenceStatus.OPEN, after.absences.first().status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidReportIsRejected() = runBlocking {
        val date = UntisDate.today()
        FakePeriodRepository().reportAbsence(date, UntisTime(1300), date, UntisTime(800), 1, "")
    }
}
