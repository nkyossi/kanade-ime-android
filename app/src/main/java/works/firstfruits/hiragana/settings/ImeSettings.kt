package works.firstfruits.hiragana.settings

import org.json.JSONObject
import java.io.File

/**
 * The values of settings.json the settings screen can change. Every other key is
 * kept as it is when saving, so the C++ side keeps its own settings.
 */
data class ImeSettings(
    val mode: String = MODE_HIRAGANA,
    val dictionary: String = DEFAULT_DICTIONARY,
    val permissible: Boolean = false,
    val useHalfWidthDigits: Boolean = false,
    val nnAsJisX4063: Boolean = false,
    val historyGeneration: Long? = null,
) {
    /** Writes the values into [file], keeping every other key already in it. */
    fun save(file: File) {
        val json = read(file) ?: JSONObject()
        json.put(KEY_MODE, mode)
        json.put(KEY_DICTIONARY, dictionary)
        json.put(KEY_PERMISSIBLE, permissible)
        json.put(KEY_HALF_WIDTH_DIGITS, useHalfWidthDigits)
        json.put(KEY_NN_AS_JIS_X_4063, nnAsJisX4063)
        historyGeneration?.let { json.put(KEY_HISTORY_GENERATION, it) }
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(json.toString(2))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    companion object {
        const val KEY_MODE = "mode"
        const val KEY_DICTIONARY = "dictionary"
        const val KEY_PERMISSIBLE = "permissible"
        const val KEY_HALF_WIDTH_DIGITS = "use-half-width-digits"
        const val KEY_NN_AS_JIS_X_4063 = "nn-as-jis-x-4063"
        const val KEY_HISTORY_GENERATION = "history-generation"

        const val MODE_HIRAGANA = "あ"
        const val MODE_ALPHANUMERIC = "A"
        const val DEFAULT_DICTIONARY = "restrained.9.dic"

        /** The settings in [file], or the defaults when it is missing or broken. */
        fun load(file: File): ImeSettings {
            val json = read(file) ?: return ImeSettings()
            return ImeSettings(
                mode = json.optString(KEY_MODE, MODE_HIRAGANA).ifEmpty { MODE_HIRAGANA },
                dictionary = json.optString(KEY_DICTIONARY, DEFAULT_DICTIONARY)
                    .ifEmpty { DEFAULT_DICTIONARY },
                permissible = json.optBoolean(KEY_PERMISSIBLE, false),
                useHalfWidthDigits = json.optBoolean(KEY_HALF_WIDTH_DIGITS, false),
                nnAsJisX4063 = json.optBoolean(KEY_NN_AS_JIS_X_4063, false),
                historyGeneration = if (json.has(KEY_HISTORY_GENERATION)) {
                    json.optLong(KEY_HISTORY_GENERATION)
                } else {
                    null
                },
            )
        }

        private fun read(file: File): JSONObject? = try {
            if (file.isFile) JSONObject(file.readText()) else null
        } catch (e: Exception) {
            null
        }
    }
}
