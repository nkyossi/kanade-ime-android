package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LetterPreviewTest {
    @Test
    fun topRowKeyHasNoBubble() {
        assertNull(LetterPreview.above(0f, 3f, 40f, 52f, 3f))
    }

    @Test
    fun bubbleIsWiderAndTallerAboveTheKey() {
        val box = LetterPreview.above(0f, 55f, 40f, 104f, 3f)!!
        assertEquals(-6.0, box.left.toDouble(), 0.001)
        assertEquals(46.0, box.right.toDouble(), 0.001)
        assertEquals(55.0, box.bottom.toDouble(), 0.001)
        assertEquals(-23.4, box.top.toDouble(), 0.001)
    }
}
