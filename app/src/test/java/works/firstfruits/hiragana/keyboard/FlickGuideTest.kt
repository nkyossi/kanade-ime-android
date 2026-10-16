package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlickGuideTest {

    @Test
    fun fiveCellsAroundTheKey() {
        val labels = listOf("あ", "い", "う", "え", "お")
        val cells = FlickGuide.plus(labels, 100f, 200f, 40f, 50f, FlickDirection.LEFT)
        assertEquals(5, cells.size)
        assertEquals(FlickGuide.Cell("あ", 100f, 200f, 40f, 50f, false), cells[0])
        assertEquals(FlickGuide.Cell("い", 60f, 200f, 40f, 50f, true), cells[1])
        assertEquals(FlickGuide.Cell("う", 100f, 150f, 40f, 50f, false), cells[2])
        assertEquals(FlickGuide.Cell("え", 140f, 200f, 40f, 50f, false), cells[3])
        assertEquals(FlickGuide.Cell("お", 100f, 250f, 40f, 50f, false), cells[4])
    }

    @Test
    fun nullDirectionsAreSkipped() {
        val labels = listOf("わ", "を", null, "ん", null)
        val cells = FlickGuide.plus(labels, 0f, 0f, 10f, 10f, FlickDirection.CENTER)
        assertEquals(listOf("わ", "を", "ん"), cells.map { it.label })
        assertTrue(cells[0].selected)
        assertFalse(cells[1].selected)
    }

    @Test
    fun stageCellsSurroundTheCentreCell() {
        val cells = FlickGuide.stage(
            "き",
            mapOf(
                FlickDirection.UP to "きゅ",
                FlickDirection.UP_LEFT to "ぴ",
            ),
            100f,
            200f,
            40f,
            50f,
            FlickDirection.UP,
        )
        assertEquals(3, cells.size)
        assertEquals(FlickGuide.Cell("き", 100f, 200f, cells[0].width, cells[0].height, false), cells[0])
        assertEquals("きゅ", cells[1].label)
        assertEquals(100f, cells[1].centerX)
        assertEquals(175f, cells[1].centerY)
        assertTrue(cells[1].selected)
        assertEquals("ぴ", cells[2].label)
        assertEquals(80f, cells[2].centerX)
        assertEquals(175f, cells[2].centerY)
        assertFalse(cells[2].selected)
        // A stage cell is smaller than the key it belongs to.
        assertTrue(cells[0].width < 40f)
        assertTrue(cells[0].height < 50f)
    }

    @Test
    fun theCentreIsSelectedWhenNoDirectionIs() {
        val cells = FlickGuide.stage("か", emptyMap(), 0f, 0f, 40f, 50f, null)
        assertEquals(1, cells.size)
        assertTrue(cells[0].selected)
    }

    @Test
    fun fitMovesTheWholeGuideInsideAtOnce() {
        // A key at the left edge: the whole square moves right, so the guide
        // does not move about when a stage with other cells opens.
        val cells = FlickGuide.stage("か", emptyMap(), 10f, 40f, 40f, 50f, null)
        val reachX = 40f * FlickGuide.STAGE_REACH
        val reachY = 50f * FlickGuide.STAGE_REACH
        val fitted = FlickGuide.fit(cells, 10f, 40f, reachX, reachY, 0f, 0f, 360f, 200f)
        assertEquals(reachX - 10f, fitted[0].centerX - cells[0].centerX, 0.01f)
        assertEquals(0f, fitted[0].centerY - cells[0].centerY, 0.01f)
    }

    @Test
    fun fitLeavesAGuideThatAlreadyFitsAlone() {
        val cells = FlickGuide.stage("か", emptyMap(), 100f, 100f, 40f, 50f, null)
        val reachX = 40f * FlickGuide.STAGE_REACH
        val reachY = 50f * FlickGuide.STAGE_REACH
        assertEquals(cells, FlickGuide.fit(cells, 100f, 100f, reachX, reachY, 0f, 0f, 360f, 200f))
    }
}
