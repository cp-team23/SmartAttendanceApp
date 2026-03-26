package com.smartattendance.student

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.face.Face
import com.smartattendance.student.adapters.FaceAnalyzer
import com.smartattendance.student.network.RetrofitClient
import com.smartattendance.student.views.FaceOverlayView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class FaceVerificationActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var faceOverlay: FaceOverlayView
    private lateinit var verifyingOverlay: View
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture
    private lateinit var attendanceId: String

    private var isCaptured      = false
    private var cameraReady     = false
    private var lastCaptureTime = 0L

    // FIX: Timeout handler — if face never detected, show error after 45 seconds
    private val timeoutHandler  = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (!isCaptured && !isFinishing) {
            showErrorDialog(
                "Face Not Detected",
                "Unable to detect your face. Please ensure good lighting and try again."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_verification)

        attendanceId = intent.getStringExtra("attendanceId") ?: ""
        previewView  = findViewById(R.id.previewView)
        faceOverlay  = findViewById(R.id.faceOverlay)

        setupVerifyingOverlay()
        startCamera()

        // FIX: Start 45-second timeout
        timeoutHandler.postDelayed(timeoutRunnable, 45_000)
    }

    // FIX: Release camera when app backgrounds (phone call etc.)
    override fun onPause() {
        super.onPause()
        if (::cameraProvider.isInitialized) cameraProvider.unbindAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacks(timeoutRunnable) // FIX: cancel timeout on exit
    }

    private fun setupVerifyingOverlay() {
        val root = findViewById<FrameLayout>(android.R.id.content)
        verifyingOverlay = LayoutInflater.from(this).inflate(R.layout.overlay_verifying, root, false)
        root.addView(verifyingOverlay)
        verifyingOverlay.visibility = View.GONE
    }

    private fun showVerifyingOverlay() { runOnUiThread { verifyingOverlay.visibility = View.VISIBLE } }
    private fun hideVerifyingOverlay() { runOnUiThread { verifyingOverlay.visibility = View.GONE } }

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
                    handleFaceFrame(face, w, h, hint, valid)
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

            previewView.postDelayed({ cameraReady = true }, 1000)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleFaceFrame(face: Face?, imageWidth: Int, imageHeight: Int, hint: String, valid: Boolean) {
        if (face == null) {
            runOnUiThread { faceOverlay.update(null, "No face detected", false) }
            return
        }

        val faceRect   = mapFaceToScreen(face, imageWidth, imageHeight)
        val insideOval = isFaceInsideOval(faceRect)
        val finalValid = valid && insideOval

        val displayHint = when {
            finalValid  -> "Perfect! Hold still"
            !insideOval -> "Align your face inside the frame"
            else        -> hint
        }

        runOnUiThread { faceOverlay.update(null, displayHint, finalValid) }

        val now = System.currentTimeMillis()
        if (cameraReady && finalValid && !isCaptured && now - lastCaptureTime > 1500) {
            lastCaptureTime = now
            isCaptured = true
            timeoutHandler.removeCallbacks(timeoutRunnable) // FIX: cancel timeout on success
            runOnUiThread { faceOverlay.clear() }
            captureAndCrop()
        }
    }

    private fun mapFaceToScreen(face: Face, imageWidth: Int, imageHeight: Int): RectF {
        val scaleX  = previewView.width.toFloat()  / imageWidth
        val scaleY  = previewView.height.toFloat() / imageHeight
        val box     = face.boundingBox
        val centerX = box.centerX() * scaleX
        val centerY = box.centerY() * scaleY
        val faceWidth  = box.width()  * scaleX
        val faceHeight = faceWidth * 1.25f
        val left   = previewView.width - (centerX + faceWidth / 2)
        val right  = previewView.width - (centerX - faceWidth / 2)
        val top    = centerY - faceHeight / 2
        val bottom = centerY + faceHeight / 2
        return RectF(left, top, right, bottom)
    }

    private fun isFaceInsideOval(faceRect: RectF): Boolean {
        val oval = faceOverlay.getCutOval()
        val nx   = (faceRect.centerX() - oval.centerX()) / (oval.width()  / 2f)
        val ny   = (faceRect.centerY() - oval.centerY()) / (oval.height() / 2f)
        return (nx * nx + ny * ny) <= 1f
    }

    private fun captureAndCrop() {
        val fullFile = File(cacheDir, "full_${System.currentTimeMillis()}.jpg")

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(fullFile).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    cameraProvider.unbindAll()
                    val croppedFile = cropToOval(fullFile)
                    fullFile.delete() // FIX: delete original full-size file immediately
                    sendImageToBackend(croppedFile)
                }
                override fun onError(exception: ImageCaptureException) {
                    isCaptured = false
                    runOnUiThread { faceOverlay.update(null, "No face detected", false) }
                    timeoutHandler.postDelayed(timeoutRunnable, 45_000) // restart timeout
                }
            }
        )
    }

    private fun cropToOval(original: File): File {
        val rawBitmap = BitmapFactory.decodeFile(original.absolutePath)
        val exif      = ExifInterface(original.absolutePath)
        val exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val rotateDegrees = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val flipHorizontal = exifOrientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL
                || exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE
                || exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE

        val matrix = Matrix()
        if (rotateDegrees != 0f) matrix.postRotate(rotateDegrees)
        if (flipHorizontal) matrix.postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f)

        val bitmap = if (rotateDegrees != 0f || flipHorizontal) {
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        } else rawBitmap

        val oval   = faceOverlay.getCutOval()
        val scaleX = bitmap.width.toFloat()  / previewView.width
        val scaleY = bitmap.height.toFloat() / previewView.height

        val safe = RectF(
            (oval.left   * scaleX).coerceAtLeast(0f),
            (oval.top    * scaleY).coerceAtLeast(0f),
            (oval.right  * scaleX).coerceAtMost(bitmap.width.toFloat()),
            (oval.bottom * scaleY).coerceAtMost(bitmap.height.toFloat())
        )

        val cropped = Bitmap.createBitmap(bitmap, safe.left.toInt(), safe.top.toInt(), safe.width().toInt(), safe.height().toInt())
        val resized  = Bitmap.createScaledBitmap(cropped, 256, 256, true)

        val outputFile = File(cacheDir, "face_${System.currentTimeMillis()}.jpg")
        outputFile.outputStream().use { resized.compress(Bitmap.CompressFormat.JPEG, 90, it) }

        return outputFile
    }

    private fun sendImageToBackend(file: File) {
        showVerifyingOverlay()

        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body        = MultipartBody.Part.createFormData("image", file.name, requestFile)

        RetrofitClient.create(this)
            .scanFace(attendanceId, body)
            .enqueue(object : Callback<Map<String, String>> {

                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    hideVerifyingOverlay()
                    file.delete() // FIX: delete cropped face file after upload

                    if (response.isSuccessful) {
                        onAttendanceSuccess()
                        return
                    }

                    val error = response.errorBody()?.string() ?: ""
                    when {
                        error.contains("FACE_NOT_MATCHED") -> showFaceNotMatchedDialog()
                        error.contains("QR_EXPIRED")       -> showErrorDialog("QR Expired", "The QR code has expired. Please scan a new one.")
                        error.contains("SCAN_QR_AGAIN")    -> showErrorDialog("Scan Again", "Please scan the QR code again.")
                        else                               -> showErrorDialog("Verification Failed", "Something went wrong. Please try again.")
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    hideVerifyingOverlay()
                    file.delete() // FIX: delete even on failure
                    showErrorDialog("Network Error", "Check your connection and try again.")
                }
            })
    }

    private fun showFaceNotMatchedDialog() {
        runOnUiThread {
            MaterialAlertDialogBuilder(this)
                .setTitle("Face not matched")
                .setMessage("Please try again with proper lighting and alignment.")
                .setCancelable(false)
                .setPositiveButton("Try Again") { dialog, _ -> dialog.dismiss(); resetAndRestart() }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss(); goToHome() }
                .show()
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        runOnUiThread {
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Try Again") { dialog, _ -> dialog.dismiss(); resetAndRestart() }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss(); goToHome() }
                .show()
        }
    }

    private fun resetAndRestart() {
        isCaptured      = false
        cameraReady     = false
        lastCaptureTime = 0L
        faceOverlay.update(null, "No face detected", false)
        startCamera()
        timeoutHandler.removeCallbacks(timeoutRunnable)
        timeoutHandler.postDelayed(timeoutRunnable, 45_000) // restart timeout
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }

    private fun onAttendanceSuccess() {
        startActivity(Intent(this, AttendanceSuccessActivity::class.java))
        finish()
    }
}