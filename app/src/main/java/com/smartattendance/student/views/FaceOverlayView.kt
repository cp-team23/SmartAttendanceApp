package com.smartattendance.student.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Dynamic ML face box (kept for compatibility)
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    // ── CHANGE 1: Oval border paint — color now changes with state ────────────
    // Removed hardcoded Color.WHITE, color is now set dynamically in onDraw()
    private val cutBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        color = Color.parseColor("#F44336") // starts Red (no face)
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
    private var hintText: String = "No face detected"
    private var isFaceValid: Boolean = false

    // ── CHANGE 2: Added a third state — no face / detected / valid ────────────
    // Previously only had valid/invalid (white vs green)
    // Now: NO_FACE=Red, FACE_DETECTED=Yellow, FACE_VALID=Green
    private enum class FaceState { NO_FACE, FACE_DETECTED, FACE_VALID }
    private var currentState: FaceState = FaceState.NO_FACE

    // Fixed oval
    private val cutOval = RectF()

    // ── CHANGE 3: update() now also sets the correct state ───────────────────
    fun update(rect: RectF?, hint: String, valid: Boolean) {
        faceRect    = rect
        hintText    = hint
        isFaceValid = valid

        // Decide which state we are in based on hint + valid flag
        currentState = when {
            valid                      -> FaceState.FACE_VALID     // Green
            hint == "No face detected" -> FaceState.NO_FACE        // Red
            else                       -> FaceState.FACE_DETECTED  // Yellow
        }

        invalidate()
    }

    // ── CHANGE 4: clear() now shows empty hint (capture in progress) ─────────
    fun clear() {
        faceRect     = null
        hintText     = ""   // was "Align your face inside the frame" — now empty
        isFaceValid  = false
        currentState = FaceState.NO_FACE
        invalidate()
    }

    fun getCutOval(): RectF = RectF(cutOval)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val ovalWidth  = w * 0.78f
        val ovalHeight = h * 0.55f

        cutOval.set(
            (w - ovalWidth)  / 2f,
            (h - ovalHeight) / 2f,
            (w + ovalWidth)  / 2f,
            (h + ovalHeight) / 2f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Dim background with oval cut-out (unchanged from your original)
        val path = Path()
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        path.addOval(cutOval, Path.Direction.CCW)
        canvas.drawPath(path, dimPaint)

        // ── CHANGE 5: Oval border — 3 colors instead of 2 ────────────────────
        // Before: only White or Green
        // Now:    Red (no face) / Yellow (face but not aligned) / Green (valid)
        cutBorderPaint.color = when (currentState) {
            FaceState.NO_FACE       -> Color.parseColor("#F44336") // Red
            FaceState.FACE_DETECTED -> Color.parseColor("#FFC107") // Yellow
            FaceState.FACE_VALID    -> Color.parseColor("#4CAF50") // Green
        }
        canvas.drawOval(cutOval, cutBorderPaint)

        // Hint text (unchanged from your original — same drawHint method)
        if (hintText.isNotEmpty()) {
            drawHint(canvas)
        }
    }

    // drawHint() is EXACTLY your original code — not changed at all
    private fun drawHint(canvas: Canvas) {
        val padding   = 24f
        val textWidth = textPaint.measureText(hintText)
        val left      = (width - textWidth) / 2f - padding
        val right     = (width + textWidth) / 2f + padding
        val top       = height - 180f
        val bottom    = height - 100f

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