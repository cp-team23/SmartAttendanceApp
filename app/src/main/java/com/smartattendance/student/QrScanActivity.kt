package com.smartattendance.student

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.smartattendance.student.adapters.QrAnalyzer
import com.smartattendance.student.models.QrAttendancePayload
import com.smartattendance.student.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicBoolean

class QrScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var loadingOverlay: FrameLayout

    private val gson = Gson()
    private val hasHandledQr = AtomicBoolean(false)

    companion object {
        private const val CAMERA_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scan)

        previewView   = findViewById(R.id.previewView)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }

        setupBottomNavigation()
    }

    // FIX: Release camera when app goes to background (phone call, etc.)
    override fun onPause() {
        super.onPause()
        if (::cameraProvider.isInitialized) cameraProvider.unbindAll()
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission() && !hasHandledQr.get()) startCamera()
    }

    // FIX: Handle camera permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR code", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)

        future.addListener({
            cameraProvider = future.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(this), QrAnalyzer { onQrDetected(it) })

            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )

            setupZoom()

        }, ContextCompat.getMainExecutor(this))
    }

    private fun onQrDetected(qrText: String) {
        if (hasHandledQr.get()) return
        if (!hasHandledQr.compareAndSet(false, true)) return

        if (!qrText.trim().startsWith("{")) {
            resetScanner("Invalid QR Code")
            return
        }
        processQr(qrText)
    }

    private fun processQr(qrText: String) {
        try {
            val payload = gson.fromJson(qrText, QrAttendancePayload::class.java)

            if (payload.attendanceId.isBlank() || payload.encryptedCode.isBlank() || payload.expireTime <= 0) {
                resetScanner("Invalid QR Code")
                return
            }

            if (System.currentTimeMillis() > payload.expireTime) {
                resetScanner("QR Expired")
                return
            }

            verifyQrWithServer(payload)

        } catch (e: JsonSyntaxException) {
            resetScanner("Invalid QR Code")
        }
    }

    private fun verifyQrWithServer(payload: QrAttendancePayload) {
        showLoading()

        RetrofitClient.create(this).scanQrCode(payload)
            .enqueue(object : Callback<Map<String, String>> {

                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    hideLoading()

                    if (response.isSuccessful) {
                        cameraProvider.unbindAll()
                        goToFaceVerification(payload)
                        return
                    }

                    val error = response.errorBody()?.string()
                    when {
                        error?.contains("QR_EXPIRED")                 == true -> showAndReset("QR expired")
                        error?.contains("ATTENDANCE_ALREADY_MARKED")  == true -> showAndReset("Attendance already marked")
                        error?.contains("NOT_ALLOWED")                == true -> showAndReset("Attendance not allowed")
                        error?.contains("IMAGE_NOT_FOUND")            == true -> showAndReset("Upload profile photo first")
                        error?.contains("INVALID_QR_DATA")            == true -> showAndReset("Invalid QR")
                        else -> showAndReset("Verification failed")
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    hideLoading()
                    showAndReset("No internet connection")
                }
            })
    }

    private fun showAndReset(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        resetScanner()
    }

    private fun resetScanner(message: String? = null) {
        message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        Handler(Looper.getMainLooper()).postDelayed({ hasHandledQr.set(false) }, 1200)
    }

    private fun showLoading() { loadingOverlay.visibility = View.VISIBLE }
    private fun hideLoading() { loadingOverlay.visibility = View.GONE }

    private fun goToFaceVerification(payload: QrAttendancePayload) {
        val intent = Intent(this, FaceVerificationActivity::class.java)
        intent.putExtra("attendanceId",  payload.attendanceId)
        intent.putExtra("encryptedCode", payload.encryptedCode)
        intent.putExtra("expireTime",    payload.expireTime)
        startActivity(intent)
        finish()
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() =
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

    private fun setupZoom() {
        val detector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val zoom = camera.cameraInfo.zoomState.value ?: return false
                    camera.cameraControl.setZoomRatio(zoom.zoomRatio * detector.scaleFactor)
                    return true
                }
            })
        previewView.setOnTouchListener { _, event -> detector.onTouchEvent(event); true }
    }

    private fun setupBottomNavigation() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
