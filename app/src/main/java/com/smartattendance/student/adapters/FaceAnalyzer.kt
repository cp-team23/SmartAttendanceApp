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
    private val REQUIRED_STABLE_FRAMES = 8

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(options)
    }

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->

                if (faces.isEmpty()) {
                    resetStability()
                    onFaceUpdate(null, imageProxy.width, imageProxy.height, "No face detected", false)
                    return@addOnSuccessListener
                }

                val face = faces.maxBy { it.boundingBox.width() * it.boundingBox.height() }

                val (isCurrentlyValid, hint) = validateFace(face, imageProxy)

                if (isCurrentlyValid) stableFrameCount++ else resetStability()

                val isStableValid = stableFrameCount >= REQUIRED_STABLE_FRAMES

                onFaceUpdate(
                    face,
                    imageProxy.width,
                    imageProxy.height,
                    if (isStableValid) "Hold still" else hint,
                    isStableValid
                )
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun resetStability() { stableFrameCount = 0 }

    private fun validateFace(face: Face, image: ImageProxy): Pair<Boolean, String> {

        val box = face.boundingBox

        // 1. Both eye landmarks must be detected
        if (face.getLandmark(FaceLandmark.LEFT_EYE) == null ||
            face.getLandmark(FaceLandmark.RIGHT_EYE) == null
        ) {
            return false to "Show full face"
        }

        // 2. Both eyes must be open
        val leftEyeOpen  = face.leftEyeOpenProbability  ?: 0f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 0f
        if (leftEyeOpen < 0.6f || rightEyeOpen < 0.6f) {
            return false to "Keep your eyes open"
        }

        // 3. Face size — comfortable distance range
        //    FIX: min lowered 0.28 → 0.20  (student can stand further away)
        //         max lowered 0.65 → 0.50  (won't need to be extremely close)
        val widthRatio = box.width().toFloat() / image.width
        if (widthRatio < 0.20f) return false to "Move closer"
        if (widthRatio > 0.50f) return false to "Move back"

        // 4. Head must not be turned left/right
        if (abs(face.headEulerAngleY) > 15f) return false to "Look straight ahead"

        // 5. Head must not be tilted up/down
        if (abs(face.headEulerAngleX) > 10f) return false to "Keep head straight"

        // 6. Head must not be rolled ear-to-shoulder
        if (abs(face.headEulerAngleZ) > 15f) return false to "Don't tilt your head"

        // 7. Face must be centered in the camera frame
        val faceCenterX = box.centerX().toFloat() / image.width
        val faceCenterY = box.centerY().toFloat() / image.height
        if (faceCenterX !in 0.30f..0.70f) return false to "Center your face"
        if (faceCenterY !in 0.25f..0.70f) return false to "Align face in frame"

        // 8. Face visibility ratio — reject partial faces
        val visibleRatio = (box.width() * box.height()).toFloat() / (image.width * image.height)
        if (visibleRatio < 0.06f) return false to "Show full face"

        // 9. Nose must be visible (catches masked faces)
        if (face.getLandmark(FaceLandmark.NOSE_BASE) == null) {
            return false to "Show full face"
        }

        return true to "Hold still"
    }
}