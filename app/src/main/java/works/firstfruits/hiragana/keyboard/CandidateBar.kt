package works.firstfruits.hiragana.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/** A row of conversion candidates above the keyboard; also shows the input mode. */
class CandidateBar(context: Context) : LinearLayout(context) {

    fun interface Listener {
        fun onCandidate(index: Int)
    }

    var listener: Listener? = null

    private val density = resources.displayMetrics.density
    private val modeView = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.rgb(0x33, 0x66, 0xcc))
    }
    private val list = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val scroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(list)
    }

    init {
        orientation = HORIZONTAL
        setBackgroundColor(Color.rgb(0xf0, 0xf1, 0xf4))
        val height = (44 * density).toInt()
        addView(modeView, LayoutParams((44 * density).toInt(), height))
        addView(scroll, LayoutParams(0, height, 1f))
    }

    fun setMode(label: String) {
        modeView.text = label
    }

    fun setCandidates(candidates: List<String>, cursor: Int) {
        list.removeAllViews()
        candidates.forEachIndexed { i, text ->
            val view = TextView(context).apply {
                this.text = text
                textSize = 18f
                gravity = Gravity.CENTER
                val pad = (12 * density).toInt()
                setPadding(pad, 0, pad, 0)
                setTextColor(Color.rgb(0x20, 0x20, 0x20))
                if (i == cursor) setBackgroundColor(Color.rgb(0xc8, 0xd8, 0xf0))
                setOnClickListener { listener?.onCandidate(i) }
            }
            list.addView(view, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        if (cursor in 0 until list.childCount) {
            list.post { list.getChildAt(cursor)?.let { scroll.smoothScrollTo(maxOf(0, it.left - it.width), 0) } }
        } else {
            scroll.scrollTo(0, 0)
        }
    }
}
