package works.firstfruits.hiragana.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HistoryResetTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun bumpsGenerationAndClearsTheFiles() {
        val userDir = folder.newFolder("user")
        val settingsFile = File(userDir, "settings.json")
        ImeSettings(mode = ImeSettings.MODE_ALPHANUMERIC, historyGeneration = 4).save(settingsFile)

        val dicDir = File(userDir, "dic").apply { mkdirs() }
        val dic = File(dicDir, "restrained.9.dic").apply { writeText("old history\n") }
        val bak = File(dicDir, "restrained.9.dic.bak").apply { writeText("backup") }
        val tmp = File(dicDir, "restrained.9.dic.tmp").apply { writeText("temp") }

        clearHistory(userDir, settingsFile)

        val loaded = ImeSettings.load(settingsFile)
        assertEquals(5L, loaded.historyGeneration ?: -1L)
        assertEquals(ImeSettings.MODE_ALPHANUMERIC, loaded.mode)
        assertTrue(dic.exists())
        assertEquals("; v1.0.0\n", dic.readText())
        assertFalse(bak.exists())
        assertFalse(tmp.exists())
    }
}
