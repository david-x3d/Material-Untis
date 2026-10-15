package dev.x3d.dayline

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import dev.x3d.dayline.domain.model.WatchPayload
import dev.x3d.dayline.domain.model.WatchPeriod
import dev.x3d.dayline.domain.time.UntisDate
import dev.x3d.dayline.domain.time.UntisTime
import java.io.File
import java.time.LocalTime
import kotlinx.coroutines.flow.flowOf
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** `gradlew :wear:testDebugUnitTest -Pscreenshots` writes docs/screenshots/watch.png. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "de-w227dp-h227dp-round-watch-xhdpi", application = android.app.Application::class)
class WearScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val dir = System.getProperty("screenshotDir")

    @Before fun onlyWhenAsked() = assumeTrue(dir != null)

    @Test fun today() {
        // A lesson that is running right now, so the hero card shows a countdown.
        val now = LocalTime.now().withSecond(0).withNano(0)
        fun t(minutes: Long) = UntisTime.fromLocalTime(now.plusMinutes(minutes)).raw
        val periods = listOf(
            WatchPeriod("BIO", "B12", "WE", t(-120), t(-75), "normal", ""),
            WatchPeriod("BIO", "B12", "WE", t(-75), t(-30), "normal", ""),
            WatchPeriod("INF", "LAB", "NK", t(-10), t(35), "irregular", "Raumänderung"),
            WatchPeriod("L", "A02", "FR", t(40), t(85), "cancelled", ""),
            WatchPeriod("PH", "P11", "NK", t(90), t(135), "normal", ""),
        )
        val payload = WatchPayload(UntisDate.today().yyyymmdd, System.currentTimeMillis(), periods)
        rule.setContent {
            MaterialTheme {
                AppScaffold { TodayScreen(flowOf(payload)) }
            }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        val bitmap = render()
        File(dir!!).mkdirs()
        File(dir, "watch.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Draws the window directly; captureToImage() waits for a frame Robolectric never delivers. */
    private fun render(): Bitmap {
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(android.graphics.Canvas(bitmap)) }
        return bitmap
    }
}
