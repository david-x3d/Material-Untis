package dev.x3d.dayline.domain.model

import kotlin.math.abs

/** How subjects are coloured: generated from the name (re-rollable via [seed]) or picked per subject. */
data class SubjectColors(
    val custom: Boolean = false,
    val seed: Int = 0,
    /** Subject short name → ARGB. Only used when [custom] is on; missing subjects fall back to auto. */
    val picked: Map<String, Int> = emptyMap(),
) {
    fun argb(subject: String, dark: Boolean): Int =
        picked[subject]?.takeIf { custom } ?: SubjectPalette.auto(subject, seed, dark)
}

object SubjectPalette {
    /** Swatches for the picker: bright enough for dark mode, deep enough for light mode. */
    val PRESETS: List<Int> = listOf(
        0xFFEF5350, 0xFFFF7043, 0xFFFFA726, 0xFFFFCA28,
        0xFFD4E157, 0xFF9CCC65, 0xFF66BB6A, 0xFF26A69A,
        0xFF26C6DA, 0xFF29B6F6, 0xFF5C6BC0, 0xFF7E57C2,
        0xFFAB47BC, 0xFFEC407A, 0xFF8D6E63, 0xFF78909C,
    ).map { it.toInt() }

    fun auto(subject: String, seed: Int, dark: Boolean): Int {
        if (subject.isEmpty()) return if (dark) 0xFF8A9BA8.toInt() else 0xFF5F6B73.toInt()
        val hue = (abs((subject.hashCode() * 31 + seed * 7919)) % 360).toFloat()
        return hsl(hue, if (dark) 0.6f else 0.55f, if (dark) 0.72f else 0.45f)
    }

    private fun hsl(h: Float, s: Float, l: Float): Int {
        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs((h / 60f) % 2 - 1))
        val m = l - c / 2
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(v: Float) = ((v + m) * 255).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
