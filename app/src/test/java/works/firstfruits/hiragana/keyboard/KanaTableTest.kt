package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KanaTableTest {

    @Test
    fun everyKanaKeyHasStages() {
        for (label in listOf("あ", "か", "さ", "た", "な", "は", "ま", "や", "ら", "わ", "、")) {
            assertNotNull(label, KanaTable.tree(label))
        }
    }

    @Test
    fun aKeyStartsAtItsOwnKana() {
        for (label in listOf("あ", "か", "さ", "た", "な", "は", "ま", "や", "ら", "わ", "、")) {
            val key = KanaTable.tree(label)!!
            assertEquals(label, key.map(KanaMode.NORMAL)[FlickDirection.CENTER]?.text)
        }
    }

    @Test
    fun theFiveSidesMatchTheOldFlicks() {
        // Left, up, right and down of the first stage stay what they were before
        // the third stage was added, so turning the setting off changes little.
        assertEquals("き", KanaTable.tree("か")!!.side(FlickDirection.LEFT))
        assertEquals("く", KanaTable.tree("か")!!.side(FlickDirection.UP))
        assertEquals("け", KanaTable.tree("か")!!.side(FlickDirection.RIGHT))
        assertEquals("こ", KanaTable.tree("か")!!.side(FlickDirection.DOWN))
    }

    @Test
    fun onlyTheDakutenRowsHaveTheExtraMaps() {
        assertNotNull(KanaTable.tree("か")!!.dakuten)
        assertNotNull(KanaTable.tree("は")!!.dakuten)
        assertNotNull(KanaTable.tree("は")!!.handakuten)
        assertNull(KanaTable.tree("さ")!!.handakuten)
        assertNull(KanaTable.tree("あ")!!.dakuten)
    }

    @Test
    fun theHaRowHasBothKindsOfDots() {
        val ha = KanaTable.tree("は")!!
        // The a column of each row carries its own dot on a corner of the key.
        assertEquals("ば", ha.map(KanaMode.DAKUTEN)[FlickDirection.UP_RIGHT]?.text)
        assertEquals("ぱ", ha.map(KanaMode.HANDAKUTEN)[FlickDirection.UP_LEFT]?.text)
        // The middle of those rows is the plain kana, a way back to it.
        assertEquals("は", ha.map(KanaMode.DAKUTEN)[FlickDirection.CENTER]?.text)
        assertEquals("は", ha.map(KanaMode.HANDAKUTEN)[FlickDirection.CENTER]?.text)
    }

    @Test
    fun theYoonIsADeeperStageOfTheIColumn() {
        val ka = KanaTable.tree("か")!!
        val i = ka.map(KanaMode.NORMAL)[FlickDirection.LEFT]
        assertTrue(i is FlickNode.Branch)
        i as FlickNode.Branch
        assertEquals("きゅ", (i.children[FlickDirection.UP] as FlickNode.Branch).label)
        assertEquals("きゃ", i.children[FlickDirection.RIGHT]?.text)
    }

    @Test
    fun aRowWithoutHandakutenHasNoCornerForIt() {
        val ka = KanaTable.tree("か")!!
        assertNull(ka.map(KanaMode.NORMAL)[FlickDirection.UP_LEFT])
        assertNotNull(KanaTable.tree("は")!!.map(KanaMode.NORMAL)[FlickDirection.UP_LEFT])
    }

    private fun FlickKey.side(direction: FlickDirection): String? =
        map(KanaMode.NORMAL)[direction]?.text
}
