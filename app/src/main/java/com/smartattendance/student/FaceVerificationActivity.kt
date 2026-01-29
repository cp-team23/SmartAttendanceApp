package com.smartattendance.student

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.Face
import com.smartattendance.student.adapters.FaceAnalyzer
import com.smartattendance.student.views.FaceOverlayView
import java.io.File

class FaceVerificationActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture
    private lateinit var faceOverlay: FaceOverlayView

    private var isCaptured = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_verification)

        previewView = findViewById(R.id.previewView)
        faceOverlay = findViewById(R.id.faceOverlay)

        startCamera()
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(
                ContextCompat.getMainExecutor(this),
                FaceAnalyzer { face, w, h, hint, valid ->

                    if (face == null) {
                        runOnUiThread { faceOverlay.clear() }
                        return@FaceAnalyzer
                    }

                    // Map ML face → preview space
                    val faceRect = mapFaceNormalized(face, w, h)

                    // 🔑 OVAL-BASED VALIDATION
                    val insideOval = isFaceInsideOval(faceRect)
                    val finalValid = valid && insideOval

                    runOnUiThread {
                        faceOverlay.update(
                            rect = null,          // 👈 no dynamic box drawn
                            hint = if (insideOval) hint else "Align face inside frame",
                            valid = finalValid
                        )
                    }

                    if (face != null) {
                        val rect = mapFaceNormalized(face, w, h)
                        val insideOval = isFaceInsideOval(rect)

                        faceOverlay.update(rect, hint, insideOval)

                        if (insideOval && valid && !isCaptured) {
                            isCaptured = true
                            faceOverlay.clear()
                            captureAndCrop()
                        }
                    } else {
                        faceOverlay.clear()
                    }

                }
            )

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageCapture,
                analysis
            )

        }, ContextCompat.getMainExecutor(this))
    }

    // ✅ Normalized face rect (logic only, not drawn)
    private fun mapFaceNormalized(face: Face, imageW: Int, imageH: Int): RectF {

        val scaleX = previewView.width.toFloat() / imageW
        val scaleY = previewView.height.toFloat() / imageH

        val box = face.boundingBox

        val centerX = box.centerX() * scaleX
        val centerY = box.centerY() * scaleY

        val faceWidth = box.width() * scaleX
        val faceHeight = faceWidth * 1.25f

        val left = previewView.width - (centerX + faceWidth / 2)
        val right = previewView.width - (centerX - faceWidth / 2)
        val top = centerY - faceHeight / 2
        val bottom = centerY + faceHeight / 2

        return RectF(left, top, right, bottom)
    }

    // 🔒 Face must be INSIDE oval (with tolerance)
    private fun isFaceInsideOval(faceRect: RectF): Boolean {
        val oval = faceOverlay.getCutOval()

        val faceCenterX = faceRect.centerX()
        val faceCenterY = faceRect.centerY()

        val ovalCenterX = oval.centerX()
        val ovalCenterY = oval.centerY()

        val rx = oval.width() / 2f
        val ry = oval.height() / 2f

        // Ellipse equation: ((x-h)^2 / rx^2) + ((y-k)^2 / ry^2) <= 1
        val normalizedX = (faceCenterX - ovalCenterX) / rx
        val normalizedY = (faceCenterY - ovalCenterY) / ry

        return (normalizedX * normalizedX + normalizedY * normalizedY) <= 1f
    }


    // 📸 Capture full image → crop by oval
    private fun captureAndCrop() {
        val fullFile = File(cacheDir, "full_${System.currentTimeMillis()}.jpg")

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(fullFile).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val cropped = cropFaceFromOval(fullFile)
                    sendImageToBackend(cropped)
                }

                override fun onError(exception: ImageCaptureException) {
                    isCaptured = false
                    Toast.makeText(
                        this@FaceVerificationActivity,
                        "Capture failed. Try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // ✂️ Crop strictly using oval
    private fun cropFaceFromOval(original: File): File {

        val bitmap = BitmapFactory.decodeFile(original.absolutePath)
        val oval = faceOverlay.getCutOval()

        val scaleX = bitmap.width.toFloat() / previewView.width
        val scaleY = bitmap.height.toFloat() / previewView.height

        val cropRect = RectF(
            oval.left * scaleX,
            oval.top * scaleY,
            oval.right * scaleX,
            oval.bottom * scaleY
        )

        val safe = RectF(
            cropRect.left.coerceAtLeast(0f),
            cropRect.top.coerceAtLeast(0f),
            cropRect.right.coerceAtMost(bitmap.width.toFloat()),
            cropRect.bottom.coerceAtMost(bitmap.height.toFloat())
        )

        val cropped = Bitmap.createBitmap(
            bitmap,
            safe.left.toInt(),
            safe.top.toInt(),
            safe.width().toInt(),
            safe.height().toInt()
        )

        val out = File(cacheDir, "face_${System.currentTimeMillis()}.jpg")
        out.outputStream().use {
            cropped.compress(Bitmap.CompressFormat.JPEG, 92, it)
        }

        return out
    }

    private fun sendImageToBackend(file: File) {
        Toast.makeText(this, "Verifying face...", Toast.LENGTH_SHORT).show()

        previewView.postDelayed({
            onAttendanceSuccess()
        }, 1000)
    }

    private fun onAttendanceSuccess() {
        startActivity(Intent(this, AttendanceSuccessActivity::class.java))
        finish()
    }


}
