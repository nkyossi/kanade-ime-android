package works.firstfruits.hiragana.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

/** Draws a [KeyboardLayout] and reports taps and flicks to [listener]. */
class KeyboardView(context: Context) : View(context) {

    fun interface Listener {
        /** [direction] is CENTER except for a flick on a Kana key. */
        fun onKey(key: Key, direction: FlickDirection)
    }

    var listener: Listener? = null

    var layout: KeyboardLayout = QwertyLayout.layout
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    /** Shows the letter keys in upper case while the one-shot shift is on. */
    var shifted = false
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val rowHeight = 52 * density
    private val gap = 3 * density
    private val flickThreshold = 24 * density

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0xfa, 0xfa, 0xfa) }
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0xc8, 0xd8, 0xf0) }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0x20, 0x20, 0x20)
        textAlign = Paint.Align.CENTER
        textSize = 20 * density
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0x80, 0x80, 0x80)
        textAlign = Paint.Align.CENTER
        textSize = 9 * density
    }

    private var bounds: List<Pair<Key, RectF>> = emptyList()
    private var pressed: Pair<Key, RectF>? = null
    private var downX = 0f
    private var downY = 0f

    private val repeatHandler = Handler(Looper.getMainLooper())
    private var repeated = false
    private val repeater = object : Runnable {
        override fun run() {
            val key = pressed?.first ?: return
            repeated = true
            listener?.onKey(key, FlickDirection.CENTER)
            repeatHandler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    init {
        setBackgroundColor(Color.rgb(0xd8, 0xdb, 0xe0))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (layout.rows.size * rowHeight + gap).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val result = mutableListOf<Pair<Key, RectF>>()
        val w = (right - left).toFloat()
        layout.rows.forEachIndexed { r, row ->
            val total = row.keys.sumOf { it.width.toDouble() }.toFloat()
            var x = 0f
            val y = r * rowHeight
            for (key in row.keys) {
                val kw = w * key.width / total
                result += key to RectF(x + gap, y + gap, x + kw, y + rowHeight)
                x += kw
            }
        }
        bounds = result
    }

    override fun onDraw(canvas: Canvas) {
        val radius = 6 * density
        for ((key, rect) in bounds) {
            canvas.drawRoundRect(rect, radius, radius, if (pressed?.first === key) pressedPaint else keyPaint)
            val label = if (shifted && key.action is KeyAction.Letter) key.label.uppercase() else key.label
            val textY = rect.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2
            canvas.drawText(label, rect.centerX(), textY, labelPaint)
            if (key.hint.isNotEmpty()) canvas.drawText(key.hint, rect.centerX(), rect.bottom - 4 * density, hintPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressed = bounds.firstOrNull { it.second.contains(event.x, event.y) }
                downX = event.x
                downY = event.y
                repeated = false
                if (pressed?.first?.repeatable == true) repeatHandler.postDelayed(repeater, REPEAT_DELAY_MS)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                repeatHandler.removeCallbacks(repeater)
                val key = pressed?.first
                pressed = null
                invalidate()
                if (key != null && !repeated) {
                    val direction = if (key.action is KeyAction.Kana) {
                        FlickResolver.resolve(event.x - downX, event.y - downY, flickThreshold)
                    } else {
                        FlickDirection.CENTER
                    }
                    listener?.onKey(key, direction)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                repeatHandler.removeCallbacks(repeater)
                pressed = null
                invalidate()
            }
        }
        return true
    }

    companion object {
        private const val REPEAT_DELAY_MS = 400L
        private const val REPEAT_INTERVAL_MS = 50L
    }
}
