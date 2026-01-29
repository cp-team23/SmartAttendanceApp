package com.smartattendance.student.adapters

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import kotlin.math.abs

class FaceAnalyzer(
    private val onFaceUpdate: (Face?, Int, Int, String, Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector: FaceDetector

    // 🔒 Stability control (prevents instant capture)
    private var stableFrameCount = 0
    private val REQUIRED_STABLE_FRAMES = 6

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(options)
    }

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(image)
            .addOnSuccessListener { faces ->

                // ❌ No face
                if (faces.isEmpty()) {
                    resetStability()
                    onFaceUpdate(
                        null,
                        imageProxy.width,
                        imageProxy.height,
                        "No face detected",
                        false
                    )
                    return@addOnSuccessListener
                }

                // ✅ Pick largest face only
                val face = faces.maxBy {
                    it.boundingBox.width() * it.boundingBox.height()
                }

                val (isCurrentlyValid, hint) = validateFace(face, imageProxy)

                if (isCurrentlyValid) {
                    stableFrameCount++
                } else {
                    resetStability()
                }

                val isStableValid = stableFrameCount >= REQUIRED_STABLE_FRAMES

                onFaceUpdate(
                    face,
                    imageProxy.width,
                    imageProxy.height,
                    if (isStableValid) "Hold still" else hint,
                    isStableValid
                )
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun resetStability() {
        stableFrameCount = 0
    }

    // 🔐 FULL VALIDATION (NO HALF FACE / ONE EYE / EDGE CASES)
    private fun validateFace(face: Face, image: ImageProxy): Pair<Boolean, String> {

        val box = face.boundingBox

        // 1️⃣ Eyes must be visible
        if (face.getLandmark(FaceLandmark.LEFT_EYE) == null ||
            face.getLandmark(FaceLandmark.RIGHT_EYE) == null
        ) {
            return false to "Show full face"
        }

        // 2️⃣ Face size check (distance)
        val widthRatio = box.width().toFloat() / image.width
        if (widthRatio < 0.28f) return false to "Move closer"
        if (widthRatio > 0.65f) return false to "Move back"

        // 3️⃣ Head orientation
        if (abs(face.headEulerAngleY) > 15f) return false to "Look straight"
        if (abs(face.headEulerAngleX) > 10f) return false to "Keep head straight"

        // 4️⃣ FACE MUST BE CENTERED (prevents half / side face)
        val faceCenterX = box.centerX().toFloat() / image.width
        val faceCenterY = box.centerY().toFloat() / image.height

        if (faceCenterX !in 0.35f..0.65f) {
            return false to "Center your face"
        }

        if (faceCenterY !in 0.30f..0.65f) {
            return false to "Align face in frame"
        }

        // 5️⃣ Face visibility ratio (reject partial faces)
        val visibleRatio =
            (box.width() * box.height()).toFloat() /
                    (image.width * image.height)

        if (visibleRatio < 0.10f) {
            return false to "Show full face"
        }

        return true to "Hold still"
    }
}
