package works.firstfruits.hiragana.ime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Runs the engine through JNI with a document in memory instead of a text field. */
@RunWith(AndroidJUnit4::class)
class NativeEngineTest {

    private class Document : NativeEngine.Client {
        val text = StringBuilder()
        var preedit = ""
        var candidates: List<String> = emptyList()
        var mode = 0

        override fun textBeforeCursor(): String = text.toString()
        override fun deleteBeforeCursor(utf16Length: Int) {
            text.setLength(text.length - utf16Length)
        }
        override fun commitText(text: String) {
            this.text.append(text)
        }
        override fun updatePreedit(text: String, spans: IntArray) {
            preedit = text
        }
        override fun updateCandidates(candidates: Array<String>, cursor: Int, visible: Boolean) {
            this.candidates = if (visible) candidates.toList() else emptyList()
        }
        override fun modeChanged(mode: Int) {
            this.mode = mode
        }
    }

    private lateinit var userDir: File
    private lateinit var engine: NativeEngine
    private val doc = Document()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AssetInstaller.install(context)
        userDir = File(context.cacheDir, "engine-test-${System.nanoTime()}").apply { mkdirs() }
        val settings = File(userDir, "settings.json").apply { writeText("""{"mode": "あ", "user-dictionary": ""}""") }
        engine = NativeEngine.create(AssetInstaller.systemDir(context), userDir, settings)!!
        engine.setClient(doc)
    }

    @After
    fun tearDown() {
        engine.close()
        userDir.deleteRecursively()
    }

    private fun type(s: String) {
        for (c in s) engine.tapKey(c.code, Keysym.keycodeOf(c))
    }

    private fun convert() = engine.tapKey(Keysym.SPACE, Keysym.KEY_SPACE)
    private fun enter() = engine.tapKey(Keysym.RETURN, Keysym.KEY_ENTER)

    @Test
    fun romajiIsConverted() {
        type("kyouha")
        assertEquals("きょうは", doc.text.toString())
        convert()
        assertTrue(doc.candidates.isNotEmpty())
        val first = doc.candidates.first()
        assertEquals(first, doc.preedit)
        enter()
        assertEquals(first, doc.text.toString())
        assertEquals("", doc.preedit)
    }

    @Test
    fun kanjiCandidate() {
        type("kanji")
        convert()
        assertEquals("漢字", doc.preedit)
        enter()
        assertEquals("漢字", doc.text.toString())
        assertEquals("", doc.preedit)
    }

    @Test
    fun okurigana() {
        type("ka")
        engine.tapKey(Keysym.HENKAN, Keysym.KEY_HENKAN, Keysym.SHIFT_MASK)
        type("ku")
        assertEquals("書く", doc.text.toString() + doc.preedit)
    }

    @Test
    fun flickKanaAndCycle() {
        engine.insertKana("か")
        engine.cycleKana()
        assertEquals("が", doc.text.toString())
        engine.insertKana("つ")
        engine.cycleKana()
        assertEquals("がっ", doc.text.toString())
    }

    @Test
    fun candidateClickedOnAnotherPage() {
        type("kou")
        convert()
        val all = doc.candidates
        assertTrue("needs more than one page: $all", all.size > 10)
        engine.candidateClicked(11)
        assertEquals(all[11], doc.text.toString())
        engine.focusOut()
    }

    @Test
    fun surrogatePairsAreDeletedWhole() {
        doc.text.append("𠮷")
        type("kanji")
        convert()
        enter()
        assertEquals("𠮷漢字", doc.text.toString())
    }

    @Test
    fun learningIsSavedAndReloaded() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        type("kou")
        convert()
        val second = doc.candidates[1]
        engine.candidateClicked(1)
        assertEquals(second, doc.text.toString())
        engine.focusOut()
        engine.close()

        doc.text.setLength(0)
        engine = NativeEngine.create(AssetInstaller.systemDir(context), userDir, File(userDir, "settings.json"))!!
        engine.setClient(doc)
        type("kou")
        convert()
        assertEquals(second, doc.preedit)
    }
}
