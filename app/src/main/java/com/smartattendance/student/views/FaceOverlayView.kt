package com.smartattendance.student.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val cutBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        color = Color.parseColor("#F44336")
    }

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#88000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

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
    private var hintText: String = "No face detected"
    private var isFaceValid: Boolean = false

    private enum class FaceState { NO_FACE, FACE_DETECTED, FACE_VALID }
    private var currentState: FaceState = FaceState.NO_FACE

    private val cutOval = RectF()

    fun update(rect: RectF?, hint: String, valid: Boolean) {
        faceRect    = rect
        hintText    = hint
        isFaceValid = valid
        currentState = when {
            valid                      -> FaceState.FACE_VALID
            hint == "No face detected" -> FaceState.NO_FACE
            else                       -> FaceState.FACE_DETECTED
        }
        invalidate()
    }

    fun clear() {
        faceRect     = null
        hintText     = ""
        isFaceValid  = false
        currentState = FaceState.NO_FACE
        invalidate()
    }

    fun getCutOval(): RectF = RectF(cutOval)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // FIX: Reduced oval size so student doesn't need to be too close
        // Old: 0.78 width, 0.55 height  →  huge oval, face had to fill it
        // New: 0.65 width, 0.48 height  →  comfortable passport-photo size
        val ovalWidth  = w * 0.65f
        val ovalHeight = h * 0.48f

        cutOval.set(
            (w - ovalWidth)  / 2f,
            (h - ovalHeight) / 2f,
            (w + ovalWidth)  / 2f,
            (h + ovalHeight) / 2f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val path = Path()
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        path.addOval(cutOval, Path.Direction.CCW)
        canvas.drawPath(path, dimPaint)

        cutBorderPaint.color = when (currentState) {
            FaceState.NO_FACE       -> Color.parseColor("#F44336") // Red
            FaceState.FACE_DETECTED -> Color.parseColor("#FFC107") // Yellow
            FaceState.FACE_VALID    -> Color.parseColor("#4CAF50") // Green
        }
        canvas.drawOval(cutOval, cutBorderPaint)

        if (hintText.isNotEmpty()) {
            drawHint(canvas)
        }
    }

    private fun drawHint(canvas: Canvas) {
        val padding   = 24f
        val textWidth = textPaint.measureText(hintText)
        val left      = (width - textWidth) / 2f - padding
        val right     = (width + textWidth) / 2f + padding
        val top       = height - 180f
        val bottom    = height - 100f

        canvas.drawRoundRect(RectF(left, top, right, bottom), 16f, 16f, textBgPaint)
        canvas.drawText(hintText, (width - textWidth) / 2f, bottom - 24f, textPaint)
    }
}