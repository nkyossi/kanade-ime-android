package works.firstfruits.hiragana.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UserDictionaryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun file() = File(folder.root, "my.dic")

    @Test
    fun loadsEntriesAndComments() {
        val f = file()
        f.writeText("; ユーザー辞書\nやまだ /山田/\nさくらしょうがっこう /さくら小学校/桜小学校/\n")
        val dic = UserDictionary.load(f)
        assertEquals(2, dic.entries().size)
        assertEquals("やまだ", dic.entries()[0].reading)
        assertEquals(listOf("山田"), dic.entries()[0].candidates)
        assertEquals(listOf("さくら小学校", "桜小学校"), dic.entries()[1].candidates)
        assertTrue(dic.lines.first() is UserDictionary.Comment)
    }

    @Test
    fun commentsAreKeptWhenSaved() {
        val f = file()
        f.writeText("; コメント\nやまだ /山田/\n")
        UserDictionary.load(f).add("さくら", listOf("桜")).save(f)
        val text = f.readText()
        assertTrue(text.startsWith("; コメント\n"))
        assertTrue(text.contains("さくら /桜/\n"))
    }

    @Test
    fun duplicateReadingIsMerged() {
        val f = file()
        f.writeText("やまだ /山田/\nやまだ /山田太郎/\n")
        val dic = UserDictionary.load(f)
        assertEquals(1, dic.entries().size)
        assertEquals(listOf("山田", "山田太郎"), dic.entries()[0].candidates)
    }

    @Test
    fun addAndRemove() {
        val dic = UserDictionary().add("やまだ", listOf("山田"))
        assertEquals(1, dic.entries().size)
        assertEquals(0, dic.remove("やまだ").entries().size)
    }

    @Test
    fun savesAndLoadsBackTheSame() {
        val f = file()
        val dic = UserDictionary()
            .add("やまだ", listOf("山田"))
            .add("さくら", listOf("桜", "さくら"))
        dic.save(f)
        assertEquals(dic, UserDictionary.load(f))
    }

    @Test
    fun readingsMustBeHiragana() {
        assertTrue(UserDictionary.isValidReading("やまだ"))
        assertFalse(UserDictionary.isValidReading("yamada"))
        assertFalse(UserDictionary.isValidReading("山田"))
        assertFalse(UserDictionary.isValidReading(""))
    }

    @Test
    fun candidatesMustNotHaveSeparators() {
        assertTrue(UserDictionary.isValidCandidate("山田"))
        assertFalse(UserDictionary.isValidCandidate("a/b"))
        assertFalse(UserDictionary.isValidCandidate("a;b"))
        assertFalse(UserDictionary.isValidCandidate(""))
    }
}
