package com.smartattendance.student

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ScaleGestureDetector
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
import java.util.concurrent.atomic.AtomicBoolean

class QrScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera   // 🔍 FOR ZOOM

    private val gson = Gson()

    // 🔒 CRITICAL FLAGS
    private val hasHandledQr = AtomicBoolean(false)
    private var lastQrText: String? = null

    companion object {
        private const val CAMERA_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scan)

        previewView = findViewById(R.id.previewView)

        if (hasCameraPermission()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.menu.setGroupCheckable(0, false, true)

        bottomNav.setOnItemSelectedListener { item ->
            val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java), options.toBundle())
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java), options.toBundle())
                    finish()
                    true
                }
                else -> false
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(this),
                QrAnalyzer { qrText ->
                    onQrDetected(qrText)
                }
            )

            cameraProvider.unbindAll()

            // 🔥 STORE CAMERA INSTANCE
            camera = cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

            setupPinchToZoom() // 🔍 ENABLE ZOOM

        }, ContextCompat.getMainExecutor(this))
    }

    // 🔍 PINCH TO ZOOM
    private fun setupPinchToZoom() {
        val scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val zoomState = camera.cameraInfo.zoomState.value ?: return false
                    val newZoom = zoomState.zoomRatio * detector.scaleFactor
                    camera.cameraControl.setZoomRatio(newZoom)
                    return true
                }
            }
        )

        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    // 🔒 Analyzer ONLY reports, does NOT navigate
    private fun onQrDetected(qrText: String) {
        if (hasHandledQr.get()) return

        // HARD FILTER
        if (!qrText.trim().startsWith("{")) return

        lastQrText = qrText

        // 🔥 HANDLE ONLY ONCE
        if (hasHandledQr.compareAndSet(false, true)) {
            processQr(qrText)
        }
    }

    private fun processQr(qrText: String) {
        try {
            val payload = gson.fromJson(qrText, QrAttendancePayload::class.java)

            if (payload.attendanceId.isBlank() ||
                payload.encryptedCode.isBlank() ||
                payload.expireTime <= 0
            ) {
                resetScanner("Invalid QR Code")
                return
            }

            if (System.currentTimeMillis() > payload.expireTime) {
                resetScanner("QR Code Expired")
                return
            }

            // ✅ STOP CAMERA ONLY NOW
            cameraProvider.unbindAll()

            // Simulate backend
            Handler(Looper.getMainLooper()).postDelayed({
                goToFaceVerification(payload)
            }, 800)

        } catch (e: JsonSyntaxException) {
            resetScanner("Invalid QR")
        }
    }

    private fun resetScanner(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        hasHandledQr.set(false)
        lastQrText = null
    }

    private fun goToFaceVerification(payload: QrAttendancePayload) {
        val intent = Intent(this, FaceVerificationActivity::class.java)
        intent.putExtra("attendanceId", payload.attendanceId)
        intent.putExtra("encryptedCode", payload.encryptedCode)
        intent.putExtra("expireTime", payload.expireTime)
        startActivity(intent)
        finish()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
