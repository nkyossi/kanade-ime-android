package works.firstfruits.hiragana.settings

import java.io.File

/** The one line the engine's history files are reset to. */
private const val HISTORY_HEADER = "; v1.0.0\n"

/**
 * Clears the input history the way the Windows version does: first bumps
 * history-generation so a running IME does not write the old history back,
 * then empties the dictionary files under userDir/dic.
 */
fun clearHistory(userDir: File, settingsFile: File) {
    val settings = ImeSettings.load(settingsFile)
    settings.copy(historyGeneration = (settings.historyGeneration ?: 0L) + 1L).save(settingsFile)

    val dicDir = File(userDir, "dic")
    if (!dicDir.isDirectory) return
    for (file in dicDir.listFiles().orEmpty()) {
        when {
            file.name.endsWith(".dic") -> file.writeText(HISTORY_HEADER)
            file.name.endsWith(".bak") || file.name.endsWith(".tmp") -> file.delete()
        }
    }
}
