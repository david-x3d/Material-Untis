package dev.x3d.dayline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.x3d.dayline.data.prefs.UserPrefs
import dev.x3d.dayline.domain.model.SubjectColors
import dev.x3d.dayline.ui.DaylineRoot
import dev.x3d.dayline.ui.theme.MaterialUntisTheme
import org.koin.android.ext.android.inject
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {
    private val prefs: UserPrefs by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoinContext {
                val dynamicColor by prefs.dynamicColor.collectAsStateWithLifecycle(initialValue = false)
                val subjectColors by prefs.subjectColors.collectAsStateWithLifecycle(initialValue = SubjectColors())
                MaterialUntisTheme(dynamicColor = dynamicColor, subjectColors = subjectColors) {
                    DaylineRoot()
                }
            }
        }
    }
}
