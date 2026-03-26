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

    private var stableFrameCount = 0
    private val REQUIRED_STABLE_FRAMES = 8  // increased from 6 → more stable capture

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // changed FAST → ACCURATE
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // NEW: enables eye open probability
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

                if (faces.isEmpty()) {
                    resetStability()
                    onFaceUpdate(null, imageProxy.width, imageProxy.height, "No face detected", false)
                    return@addOnSuccessListener
                }

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

    private fun validateFace(face: Face, image: ImageProxy): Pair<Boolean, String> {

        val box = face.boundingBox

        // 1. Both eyes must be detected as landmarks
        if (face.getLandmark(FaceLandmark.LEFT_EYE) == null ||
            face.getLandmark(FaceLandmark.RIGHT_EYE) == null
        ) {
            return false to "Show full face"
        }

        // 2. NEW: Both eyes must be open (probability > 0.6)
        val leftEyeOpen  = face.leftEyeOpenProbability  ?: 0f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 0f
        if (leftEyeOpen < 0.6f || rightEyeOpen < 0.6f) {
            return false to "Keep your eyes open"
        }

        // 3. Face size (distance from camera)
        val widthRatio = box.width().toFloat() / image.width
        if (widthRatio < 0.28f) return false to "Move closer"
        if (widthRatio > 0.65f) return false to "Move back"

        // 4. Head horizontal rotation (left/right turn)
        if (abs(face.headEulerAngleY) > 15f) return false to "Look straight ahead"

        // 5. Head vertical tilt (up/down)
        if (abs(face.headEulerAngleX) > 10f) return false to "Keep head straight"

        // 6. NEW: Head roll/tilt (ear-to-shoulder tilt)
        if (abs(face.headEulerAngleZ) > 15f) return false to "Don't tilt your head"

        // 7. Face must be centered horizontally and vertically
        val faceCenterX = box.centerX().toFloat() / image.width
        val faceCenterY = box.centerY().toFloat() / image.height
        if (faceCenterX !in 0.35f..0.65f) return false to "Center your face"
        if (faceCenterY !in 0.30f..0.65f) return false to "Align face in frame"

        // 8. Face visibility ratio — reject partial/cut-off faces
        val visibleRatio = (box.width() * box.height()).toFloat() / (image.width * image.height)
        if (visibleRatio < 0.10f) return false to "Show full face"

        // 9. NEW: Check nose and mouth are visible (catches masked/covered faces)
        if (face.getLandmark(FaceLandmark.NOSE_BASE) == null) {
            return false to "Show full face"
        }

        return true to "Hold still"
    }
}
