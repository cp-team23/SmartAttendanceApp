package com.smartattendance.student.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Dynamic ML face box
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    // Static oval guide
    private val cutBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    // Dim background
    private val dimPaint = Paint().apply {
        color = Color.parseColor("#88000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Hint text
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 42f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val textBgPaint = Paint().apply {
        color = Color.parseColor("#AA000000")
        isAntiAlias = true
    }

    private var faceRect: RectF? = null
    private var hintText: String = "Align your face inside the frame"
    private var isFaceValid: Boolean = false

    // Fixed oval
    private val cutOval = RectF()

    fun update(rect: RectF?, hint: String, valid: Boolean) {
        faceRect = rect
        hintText = hint
        isFaceValid = valid
        invalidate()
    }

    fun clear() {
        faceRect = null
        hintText = "Align your face inside the frame"
        isFaceValid = false
        invalidate()
    }

    fun getCutOval(): RectF = RectF(cutOval)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val ovalWidth = w * 0.78f
        val ovalHeight = h * 0.55f

        cutOval.set(
            (w - ovalWidth) / 2f,
            (h - ovalHeight) / 2f,
            (w + ovalWidth) / 2f,
            (h + ovalHeight) / 2f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Dim background with cut-out
        val path = Path()
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        path.addOval(cutOval, Path.Direction.CCW)
        canvas.drawPath(path, dimPaint)

        // Oval border

        cutBorderPaint.color = if (isFaceValid) {
            Color.parseColor("#2ECC71") // green
        } else {
            Color.WHITE
        }
        canvas.drawOval(cutOval, cutBorderPaint)


        // Hint
        drawHint(canvas)
    }

    private fun drawHint(canvas: Canvas) {
        val padding = 24f
        val textWidth = textPaint.measureText(hintText)
        val left = (width - textWidth) / 2f - padding
        val right = (width + textWidth) / 2f + padding
        val top = height - 180f
        val bottom = height - 100f

        canvas.drawRoundRect(
            RectF(left, top, right, bottom),
            16f,
            16f,
            textBgPaint
        )

        canvas.drawText(
            hintText,
            (width - textWidth) / 2f,
            bottom - 24f,
            textPaint
        )
    }
}
