package works.firstfruits.hiragana.keyboard

import kotlin.math.abs

/** Decides the direction of a flick from how far the finger moved. */
object FlickResolver {
    /**
     * Returns the direction for a move of ([dx], [dy]) pixels (y grows downward).
     * A move shorter than [threshold] in both axes is a tap (CENTER).
     */
    fun resolve(dx: Float, dy: Float, threshold: Float): FlickDirection {
        if (abs(dx) < threshold && abs(dy) < threshold) return FlickDirection.CENTER
        return if (abs(dx) >= abs(dy)) {
            if (dx < 0) FlickDirection.LEFT else FlickDirection.RIGHT
        } else {
            if (dy < 0) FlickDirection.UP else FlickDirection.DOWN
        }
    }
}
