package dev.x3d.dayline

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import dev.x3d.dayline.data.prefs.UserPrefs
import dev.x3d.dayline.data.repo.FakePeriodRepository
import dev.x3d.dayline.domain.PeriodRepository
import dev.x3d.dayline.domain.model.Lesson
import dev.x3d.dayline.domain.model.LessonStatus
import dev.x3d.dayline.domain.model.LessonType
import dev.x3d.dayline.domain.model.NamedRef
import dev.x3d.dayline.domain.model.TimegridDay
import dev.x3d.dayline.domain.model.TimegridUnit
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import dev.x3d.dayline.sync.SyncScheduler
import dev.x3d.dayline.ui.absences.AbsencesScreen
import dev.x3d.dayline.ui.absences.AbsencesViewModel
import dev.x3d.dayline.ui.lesson.LessonScreen
import dev.x3d.dayline.ui.login.LoginScreen
import dev.x3d.dayline.ui.login.LoginViewModel
import dev.x3d.dayline.ui.login.QrDraftStore
import dev.x3d.dayline.ui.onboarding.AccountTypeScreen
import dev.x3d.dayline.ui.onboarding.OnboardingDraft
import dev.x3d.dayline.ui.lesson.LessonViewModel
import dev.x3d.dayline.ui.settings.SettingsScreen
import dev.x3d.dayline.ui.settings.SettingsViewModel
import dev.x3d.dayline.ui.theme.MaterialUntisTheme
import dev.x3d.dayline.ui.timetable.TimetableScreen
import dev.x3d.dayline.ui.timetable.TimetableViewModel
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the phone screens with sample data for the README.
 * `gradlew :app:testDebugUnitTest -Pscreenshots` writes docs/screenshots/phone-*.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "de-w400dp-h860dp-night-xxhdpi", application = android.app.Application::class)
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val dir = System.getProperty("screenshotDir")

    @Before fun onlyWhenAsked() = assumeTrue(dir != null)

    private val monday = UntisDate.from(LocalDate.now().with(DayOfWeek.MONDAY))
    private var nextId = 1L

    private fun l(
        day: Long, s: Int, e: Int, su: String, ro: String, te: String,
        status: LessonStatus = LessonStatus.NORMAL, roOrg: String? = null, teOrg: String? = null, info: String? = null,
    ) = Lesson(
        nextId++, monday.plusDays(day), UntisTime(s), UntisTime(e),
        NamedRef(su.hashCode(), su, LONG[su] ?: su), NamedRef(1, te, "Lehrkraft $te", teOrg), NamedRef(2, ro, "Raum $ro", roOrg),
        NamedRef(3, "8B", "8B"), status, LessonType.LESSON, info, null, null,
    )

    private fun double(day: Long, s1: Int, e1: Int, s2: Int, e2: Int, su: String, ro: String, te: String, status: LessonStatus = LessonStatus.NORMAL, roOrg: String? = null) =
        listOf(l(day, s1, e1, su, ro, te, status, roOrg), l(day, s2, e2, su, ro, te, status, roOrg))

    // Invented sample week (not a real school or class).
    private val lessons = buildList {
        addAll(double(0, 800, 845, 845, 930, "BIO", "B12", "WE"))
        addAll(double(1, 800, 845, 845, 930, "M", "A07", "RO"))
        addAll(double(2, 800, 845, 845, 930, "INF", "LAB", "NK", LessonStatus.IRREGULAR, roOrg = "A07"))
        addAll(double(3, 800, 845, 845, 930, "GE", "A03", "LU"))
        addAll(double(4, 800, 845, 845, 930, "MU", "M01", "PE"))
        add(l(0, 950, 1035, "E", "A05", "TA", LessonStatus.IRREGULAR, teOrg = "SM"))
        add(l(0, 1040, 1125, "L", "A02", "FR", info = "Vokabeltest"))
        addAll(double(1, 950, 1035, 1040, 1125, "D", "A05", "SM"))
        add(l(2, 950, 1035, "M", "A09", "RO", LessonStatus.IRREGULAR, roOrg = "A07"))
        add(l(2, 1040, 1125, "PO", "A04", "GA"))
        addAll(double(3, 950, 1035, 1040, 1125, "CH", "C21", "OK"))
        addAll(double(4, 950, 1035, 1040, 1125, "E", "A05", "TA"))
        addAll(double(0, 1145, 1230, 1235, 1320, "SP", "GYM", "VA"))
        addAll(double(1, 1145, 1230, 1235, 1320, "PH", "P11", "NK"))
        addAll(double(2, 1145, 1230, 1235, 1320, "D", "A05", "SM"))
        add(l(3, 1145, 1230, "L", "A02", "FR", LessonStatus.CANCELLED))
        add(l(3, 1235, 1320, "ETH", "A08", "GA"))
        addAll(double(4, 1145, 1230, 1235, 1320, "KU", "K02", "LI"))
    }

    private val grid = listOf(
        TimegridDay(
            2,
            listOf(800 to 845, 845 to 930, 950 to 1035, 1040 to 1125, 1145 to 1230, 1235 to 1320, 1330 to 1415, 1415 to 1500)
                .mapIndexed { i, (s, e) -> TimegridUnit("${i + 1}", UntisTime(s), UntisTime(e)) },
        ),
    )

    private val repo: PeriodRepository = object : PeriodRepository by FakePeriodRepository(lessons) {
        override fun timegrid() = flowOf(grid)
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.setContent {
            MaterialUntisTheme(dark = true) {
                Surface(color = MaterialTheme.colorScheme.background) { Box(Modifier.fillMaxSize()) { content() } }
            }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        val bitmap = render()
        File(dir!!).mkdirs()
        File(dir, "phone-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun timetable() = shot("timetable") { TimetableScreen(onLesson = {}, viewModel = TimetableViewModel(repo)) }

    @Test fun lesson() = shot("lesson") { LessonScreen(id = 5, onBack = {}, viewModel = LessonViewModel(repo)) }

    @Test fun absences() = shot("absences") { AbsencesScreen(viewModel = AbsencesViewModel(repo)) }

    @Test fun settings() = shot("settings") {
        SettingsScreen(onLoggedOut = {}, viewModel = SettingsViewModel(repo, SyncScheduler(ApplicationProvider.getApplicationContext()), UserPrefs(ApplicationProvider.getApplicationContext())))
    }

    @Test fun welcome() = shot("onboarding-1") { AccountTypeScreen(onContinue = {}, draft = OnboardingDraft()) }

    @Test fun login() = shot("onboarding-3") {
        LoginScreen(
            onLoggedIn = {}, onScanQr = {}, onChangeSchool = {},
            viewModel = LoginViewModel(repo, SyncScheduler(ApplicationProvider.getApplicationContext()), QrDraftStore(), OnboardingDraft()),
        )
    }

    private companion object {
        val LONG = mapOf(
            "M" to "Mathematik", "D" to "Deutsch", "E" to "Englisch", "L" to "Latein", "BIO" to "Biologie",
            "INF" to "Informatik", "GE" to "Geschichte", "MU" to "Musik", "PO" to "Politik", "CH" to "Chemie",
            "SP" to "Sport", "PH" to "Physik", "ETH" to "Ethik", "KU" to "Kunst",
        )
    }

    /** Draws the window directly; captureToImage() waits for a frame Robolectric never delivers. */
    private fun render(): Bitmap {
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(android.graphics.Canvas(bitmap)) }
        return bitmap
    }
}
