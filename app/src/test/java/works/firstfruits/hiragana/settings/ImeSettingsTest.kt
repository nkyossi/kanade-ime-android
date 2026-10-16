package works.firstfruits.hiragana.settings

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImeSettingsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun file(name: String = "settings.json") = File(folder.root, name)

    @Test
    fun defaultsWhenFileIsMissing() {
        assertEquals(ImeSettings(), ImeSettings.load(file("missing.json")))
    }

    @Test
    fun savedValuesAreLoadedBack() {
        val f = file()
        val settings = ImeSettings(
            mode = ImeSettings.MODE_ALPHANUMERIC,
            dictionary = "restrained.3.dic",
            permissible = true,
            useHalfWidthDigits = true,
            nnAsJisX4063 = true,
        )
        settings.save(f)
        assertEquals(settings, ImeSettings.load(f))
    }

    @Test
    fun unknownKeysAreKept() {
        val f = file()
        f.writeText("""{"history-generation": 7, "foo": 1, "layout": "roomazi"}""")
        ImeSettings(mode = ImeSettings.MODE_ALPHANUMERIC).save(f)
        val json = JSONObject(f.readText())
        assertEquals(7, json.getInt("history-generation"))
        assertEquals(1, json.getInt("foo"))
        assertEquals("roomazi", json.getString("layout"))
        assertEquals(ImeSettings.MODE_ALPHANUMERIC, json.getString("mode"))
    }

    @Test
    fun brokenJsonFallsBackToDefaults() {
        val f = file()
        f.writeText("{ this is not json")
        assertEquals(ImeSettings(), ImeSettings.load(f))
    }
}
