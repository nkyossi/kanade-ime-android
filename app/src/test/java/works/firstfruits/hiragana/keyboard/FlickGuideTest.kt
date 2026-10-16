package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class FlickGuideTest {

    @Test
    fun fiveCellsAroundTheKey() {
        val labels = listOf("あ", "い", "う", "え", "お")
        val cells = FlickGuide.cells(labels, 100f, 200f, 40f, 50f)
        assertEquals(5, cells.size)
        assertEquals(FlickGuide.Cell(FlickDirection.CENTER, "あ", 100f, 200f), cells[0])
        assertEquals(FlickGuide.Cell(FlickDirection.LEFT, "い", 60f, 200f), cells[1])
        assertEquals(FlickGuide.Cell(FlickDirection.UP, "う", 100f, 150f), cells[2])
        assertEquals(FlickGuide.Cell(FlickDirection.RIGHT, "え", 140f, 200f), cells[3])
        assertEquals(FlickGuide.Cell(FlickDirection.DOWN, "お", 100f, 250f), cells[4])
    }

    @Test
    fun nullDirectionsAreSkipped() {
        val labels = listOf("わ", "を", null, "ん", null)
        val cells = FlickGuide.cells(labels, 0f, 0f, 10f, 10f)
        assertEquals(
            listOf(FlickDirection.CENTER, FlickDirection.LEFT, FlickDirection.RIGHT),
            cells.map { it.direction },
        )
    }
}
