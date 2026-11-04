package dev.x3d.dayline.data

import android.app.Application
import dev.x3d.dayline.data.prefs.UserPrefs
import dev.x3d.dayline.data.repo.DemoPeriodRepository
import dev.x3d.dayline.data.repo.FakePeriodRepository
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.time.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DemoRepositoryTest {
    @Test fun switchingPreservesAccountAndKeepsReportsOutOfRealRepository() = runBlocking {
        val prefs = UserPrefs(RuntimeEnvironment.getApplication())
        prefs.setDemoMode(false)
        val account = FakePeriodRepository()
        account.loginPassword("echter.account", "secret", null)
        var realCalls = 0
        val real = object : PeriodRepository by account {
            override suspend fun refresh(force: Boolean) { realCalls++ }
            override suspend fun reportAbsence(startDate: UntisDate, startTime: UntisTime, endDate: UntisDate, endTime: UntisTime, reasonId: Int, text: String) { realCalls++ }
        }
        val repository = DemoPeriodRepository(real, prefs)
        repository.setDemoMode(true)
        assertTrue(prefs.demoMode.first())
        withTimeout(5000) { repository.session.first { it?.user == "max.muster" } }
        repository.refresh(true)
        val date = UntisDate.today()
        repository.reportAbsence(date, UntisTime(800), date, UntisTime(1300), 1, "Nur Demo")
        assertTrue(repository.absences().first()!!.absences.any { it.text == "Nur Demo" })
        assertEquals(0, realCalls)
        assertEquals("echter.account", account.session.value!!.user)
        repository.setDemoMode(false)
        withTimeout(5000) { repository.session.first { it?.user == "echter.account" } }
        assertFalse(prefs.demoMode.first())
        assertNull(repository.absences().first())
    }
}
