package works.firstfruits.hiragana.ime

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

/**
 * Copies the dictionaries and the layouts from the assets to
 * filesDir/system once per app version, and makes filesDir/user.
 */
object AssetInstaller {
    private const val STAMP = ".installed"
    private val DIRS = listOf("dic", "layouts")

    fun systemDir(context: Context) = File(context.filesDir, "system")
    fun userDir(context: Context) = File(context.filesDir, "user")
    fun settingsFile(context: Context) = File(userDir(context), "settings.json")

    /** Installs the assets if needed; safe to call on any thread, but it may take a second. */
    @Synchronized
    fun install(context: Context) {
        val system = systemDir(context)
        val stamp = File(system, STAMP)
        val version = versionOf(context)
        if (stamp.isFile && stamp.readText() == version) {
            ensureUser(context)
            return
        }
        for (dir in DIRS) {
            val target = File(system, dir)
            target.mkdirs()
            for (name in context.assets.list(dir).orEmpty()) {
                val out = File(target, name)
                val tmp = File(target, "$name.tmp")
                context.assets.open("$dir/$name").use { input ->
                    tmp.outputStream().use { input.copyTo(it) }
                }
                if (!tmp.renameTo(out)) {
                    out.delete()
                    tmp.renameTo(out)
                }
            }
        }
        stamp.writeText(version)
        ensureUser(context)
    }

    private fun ensureUser(context: Context) {
        val user = userDir(context)
        user.mkdirs()
        val settings = settingsFile(context)
        if (!settings.exists()) settings.writeText(DEFAULT_SETTINGS)
    }

    @Suppress("DEPRECATION")
    private fun versionOf(context: Context): String = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.longVersionCode}:${info.lastUpdateTime}"
    } catch (e: PackageManager.NameNotFoundException) {
        "0"
    }

    /** The defaults of core/Settings.h, except that a software keyboard starts in hiragana. */
    private const val DEFAULT_SETTINGS = """{
  "mode": "あ",
  "dictionary": "restrained.9.dic",
  "user-dictionary": "my.dic",
  "layout": "roomazi",
  "altgr": "altgr",
  "keyboard": "us"
}
"""
}
