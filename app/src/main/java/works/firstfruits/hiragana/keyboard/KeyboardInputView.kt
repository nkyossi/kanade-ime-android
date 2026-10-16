package works.firstfruits.hiragana.keyboard

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout

/**
 * The input view: the candidate bar above the keyboard, with the flick guide of
 * the pressed key drawn on top of both. The guide of a top row key therefore
 * overlaps the candidate bar instead of being cut off by it, and the keyboard
 * keeps its usual height.
 */
class KeyboardInputView(context: Context) : FrameLayout(context) {

    /** The keyboard whose pressed key is guided. */
    var keyboard: KeyboardView? = null

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val keyboard = keyboard ?: return
        val guide = keyboard.guide ?: return
        val column = keyboard.parent as? View
        val offsetX = (keyboard.left + (column?.left ?: 0)).toFloat()
        val offsetY = (keyboard.top + (column?.top ?: 0)).toFloat()
        val save = canvas.save()
        canvas.translate(offsetX, offsetY)
        keyboard.drawGuide(canvas, guide, 0f, -offsetY, width - offsetX, height - offsetY)
        canvas.restoreToCount(save)
    }
}
