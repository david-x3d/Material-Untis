package dev.x3d.dayline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.x3d.dayline.ui.DaylineRoot
import dev.x3d.dayline.ui.theme.MaterialUntisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialUntisTheme {
                DaylineRoot()
            }
        }
    }
}
