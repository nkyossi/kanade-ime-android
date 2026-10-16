package works.firstfruits.hiragana.keyboard

import android.content.Context
import android.content.res.Configuration

/** The colors of the keyboard and the candidate bar, for a light or dark screen. */
data class KeyboardTheme(
    val keyboardBackground: Int,
    val key: Int,
    val pressedKey: Int,
    val specialKey: Int,
    val keyText: Int,
    val hintText: Int,
    val candidateBackground: Int,
    val candidateText: Int,
    val selectedCandidate: Int,
    val modeText: Int,
    val guideBackground: Int,
    val guideText: Int,
    val guideSelected: Int,
) {
    companion object {
        /** The colors used before dark mode was supported. */
        val light = KeyboardTheme(
            keyboardBackground = 0xFFD8DBE0.toInt(),
            key = 0xFFFAFAFA.toInt(),
            pressedKey = 0xFFC8D8F0.toInt(),
            specialKey = 0xFFE4E7EC.toInt(),
            keyText = 0xFF202020.toInt(),
            hintText = 0xFF808080.toInt(),
            candidateBackground = 0xFFF0F1F4.toInt(),
            candidateText = 0xFF202020.toInt(),
            selectedCandidate = 0xFFC8D8F0.toInt(),
            modeText = 0xFF3366CC.toInt(),
            guideBackground = 0xFF505860.toInt(),
            guideText = 0xFFFFFFFF.toInt(),
            guideSelected = 0xFF3366CC.toInt(),
        )

        val dark = KeyboardTheme(
            keyboardBackground = 0xFF1E2024.toInt(),
            key = 0xFF3A3D42.toInt(),
            pressedKey = 0xFF4A6A9A.toInt(),
            specialKey = 0xFF2B2E33.toInt(),
            keyText = 0xFFF0F0F0.toInt(),
            hintText = 0xFFA0A4AA.toInt(),
            candidateBackground = 0xFF2B2E33.toInt(),
            candidateText = 0xFFF0F0F0.toInt(),
            selectedCandidate = 0xFF4A6A9A.toInt(),
            modeText = 0xFF7AA2E0.toInt(),
            guideBackground = 0xFF505860.toInt(),
            guideText = 0xFFFFFFFF.toInt(),
            guideSelected = 0xFF4A6A9A.toInt(),
        )

        /** The theme that fits the night mode of [context]. */
        fun forContext(context: Context): KeyboardTheme {
            val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return if (mode == Configuration.UI_MODE_NIGHT_YES) dark else light
        }
    }
}

/** The smallest key row height, in dp. */
const val ROW_HEIGHT_DP_MIN = 40f

/** The largest key row height, in dp. */
const val ROW_HEIGHT_DP_MAX = 72f

/** Limits a key row height to [ROW_HEIGHT_DP_MIN]..[ROW_HEIGHT_DP_MAX]. */
fun clampRowHeightDp(value: Float): Float = value.coerceIn(ROW_HEIGHT_DP_MIN, ROW_HEIGHT_DP_MAX)
