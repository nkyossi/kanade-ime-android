package works.firstfruits.hiragana.keyboard

/**
 * Where the enlarged preview of a pressed letter key goes. Coordinates are plain
 * floats so the layout can be checked in JVM unit tests (RectF is not available
 * there).
 */
object LetterPreview {
    /** A preview box: [left], [top], [right] and [bottom] in view pixels. */
    data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /**
     * The box drawn above a key ([left], [top], [right], [bottom]): 1.3x the key
     * wide and 1.6x tall, sitting on the top edge of the key. Returns null for a
     * top row key ([topRowTop]) that has no room above and is drawn inside instead.
     */
    fun above(left: Float, top: Float, right: Float, bottom: Float, topRowTop: Float): Box? {
        if (top <= topRowTop) return null
        val centerX = (left + right) / 2
        val halfWidth = (right - left) * 1.3f / 2
        val height = (bottom - top) * 1.6f
        return Box(centerX - halfWidth, top - height, centerX + halfWidth, top)
    }
}
