package works.firstfruits.hiragana.ime

import org.json.JSONObject
import java.io.File

/** Reads filesDir/user/settings.json, the file core/Settings.cpp loads. */
object SettingsJson {
    /** The mode a normal text field starts in ("mode"), or null if the file has none. */
    fun startMode(file: File): Int? = try {
        val mode = JSONObject(file.readText()).optString("mode", "")
        if (mode.isEmpty()) null else mode.codePointAt(0)
    } catch (e: Exception) {
        null
    }
}
