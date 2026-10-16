package works.firstfruits.hiragana.keyboard

import kotlin.math.abs
import kotlin.math.atan2

/** Decides the direction of a flick from how far the finger moved. */
object FlickResolver {
    /**
     * Returns the direction for a move of ([dx], [dy]) pixels (y grows downward).
     * A move shorter than [threshold] in both axes is a tap (CENTER).
     */
    fun resolve(dx: Float, dy: Float, threshold: Float): FlickDirection {
        if (isTap(dx, dy, threshold)) return FlickDirection.CENTER
        return if (abs(dx) >= abs(dy)) {
            if (dx < 0) FlickDirection.LEFT else FlickDirection.RIGHT
        } else {
            if (dy < 0) FlickDirection.UP else FlickDirection.DOWN
        }
    }

    /** True when a move of ([dx], [dy]) is too short to count as a flick. */
    fun isTap(dx: Float, dy: Float, threshold: Float): Boolean =
        abs(dx) < threshold && abs(dy) < threshold

    /**
     * The direction of [available] that ([dx], [dy]) points at most closely, or
     * null when [available] holds no direction to choose. Used by the
     * three-stage flick, where a stage offers only some of the eight corners.
     */
    fun closest(dx: Float, dy: Float, available: Collection<FlickDirection>): FlickDirection? {
        val angle = angleOf(dx, dy)
        return available
            .filter { it != FlickDirection.CENTER }
            .minByOrNull { distance(angle, angle(it)) }
    }

    /** The angle of a move, in degrees, growing clockwise from the right (y grows downward). */
    private fun angleOf(dx: Float, dy: Float): Double =
        Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))

    /** The middle angle of a direction, used to pick the closest one. */
    private fun angle(direction: FlickDirection): Double = when (direction) {
        FlickDirection.RIGHT -> 0.0
        FlickDirection.DOWN_RIGHT -> 35.0
        FlickDirection.DOWN -> 90.0
        FlickDirection.DOWN_LEFT -> 125.0
        FlickDirection.LEFT -> 180.0
        FlickDirection.UP_LEFT -> -125.0
        FlickDirection.UP -> -90.0
        FlickDirection.UP_RIGHT -> -35.0
        FlickDirection.CENTER -> 0.0
    }

    /** The smaller angle between two angles, 0..180. */
    private fun distance(a: Double, b: Double): Double {
        val diff = abs(a - b) % 360.0
        return if (diff > 180.0) 360.0 - diff else diff
    }
}
