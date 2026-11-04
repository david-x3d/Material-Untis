package dev.x3d.dayline

import android.app.Application
import android.os.Looper
import org.robolectric.Shadows.shadowOf
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import dev.x3d.dayline.data.prefs.UserPrefs
import dev.x3d.dayline.data.repo.DemoPeriodRepository
import dev.x3d.dayline.data.repo.FakePeriodRepository
import dev.x3d.dayline.sync.SyncScheduler
import dev.x3d.dayline.ui.settings.SettingsScreen
import dev.x3d.dayline.ui.settings.SettingsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-w400dp-h860dp", application = Application::class)
class DemoModeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun settingsSwitchEntersDemoAndRestoresAccount() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = UserPrefs(context)
        val real = FakePeriodRepository()
        runBlocking {
            prefs.setDemoMode(false)
            real.loginPassword("real.account", "secret", null)
        }
        val repository = DemoPeriodRepository(real, prefs)
        val viewModel = SettingsViewModel(repository, SyncScheduler(context), prefs)
        rule.setContent { MaterialTheme { SettingsScreen(onLoggedOut = {}, viewModel = viewModel) } }
        val toggle = rule.onNodeWithContentDescription("Demo mode")
        toggle.assertIsOff().performClick()
        rule.waitUntil(5000) { shadowOf(Looper.getMainLooper()).idle(); repository.demoMode.value && viewModel.state.value.demoMode }
        toggle.assertIsOn()
        rule.waitUntil(5000) { shadowOf(Looper.getMainLooper()).idle(); repository.session.value?.user == "max.muster" }
        toggle.performClick()
        rule.waitUntil(5000) { shadowOf(Looper.getMainLooper()).idle(); !repository.demoMode.value && !viewModel.state.value.demoMode && repository.session.value?.user == "real.account" }
        toggle.assertIsOff()
        assertEquals("real.account", real.session.value?.user)
    }
}
