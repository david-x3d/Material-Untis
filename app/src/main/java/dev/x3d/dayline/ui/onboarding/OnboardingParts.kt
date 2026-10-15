package dev.x3d.dayline.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.x3d.dayline.R
import dev.x3d.dayline.domain.model.AccountType

/** Answers collected before sign-in (account type is asked first, used at login). */
class OnboardingDraft {
    var accountType: AccountType = AccountType.STUDENT
}

const val ONBOARDING_STEPS = 3

/** Header of each onboarding step: wavy progress, "Schritt x von 3", big title, subtitle. */
@Composable
fun StepHeader(step: Int, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LinearWavyProgressIndicator(
            progress = { step / ONBOARDING_STEPS.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.onboarding_step, step, ONBOARDING_STEPS),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The app logo in an expressive cookie shape. */
@Composable
fun LogoBadge(size: Dp = 112.dp) {
    val shape = MaterialShapes.Cookie9Sided.toShape()
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF14524B), Color(0xFF0B2522)))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(size * 1.15f),
        )
    }
}
