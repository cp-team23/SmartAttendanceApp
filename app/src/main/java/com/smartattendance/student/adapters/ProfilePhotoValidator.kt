package com.smartattendance.student.adapters

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs

object ProfilePhotoValidator {

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    fun validate(
        context: Context,
        uri: Uri,
        onResult: (ValidationResult) -> Unit
    ) {
        val bitmap: Bitmap? = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            onResult(ValidationResult.Error("Could not read image"))
            return
        }

        if (bitmap == null) {
            onResult(ValidationResult.Error("Could not read image"))
            return
        }

        val image = InputImage.fromBitmap(bitmap, 0)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->

                val result = when {
                    // No face at all
                    faces.isEmpty() ->
                        ValidationResult.Invalid("No face found in photo.\nPlease use a clear front-facing photo.")

                    // More than one face
                    faces.size > 1 ->
                        ValidationResult.Invalid("Multiple faces detected.\nPlease upload a photo with only your face.")

                    // Exactly one face — run detailed checks
                    else -> checkSingleFace(faces[0], bitmap)
                }

                onResult(result)
                detector.close()
            }
            .addOnFailureListener {
                // If detection itself fails, allow the upload (don't block on library error)
                onResult(ValidationResult.Error("Face check failed. Proceeding with upload."))
                detector.close()
            }
    }

    private fun checkSingleFace(face: Face, bitmap: Bitmap): ValidationResult {

        val bitmapW = bitmap.width.toFloat()
        val bitmapH = bitmap.height.toFloat()
        val box = face.boundingBox

        // 1. Both eye landmarks must be visible
        if (face.getLandmark(FaceLandmark.LEFT_EYE) == null ||
            face.getLandmark(FaceLandmark.RIGHT_EYE) == null
        ) {
            return ValidationResult.Invalid(
                "Eyes not clearly visible.\nPlease look directly at the camera."
            )
        }

        // 2. Both eyes must be open
        val leftEyeOpen  = face.leftEyeOpenProbability  ?: 0f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 0f
        if (leftEyeOpen < 0.5f || rightEyeOpen < 0.5f) {
            return ValidationResult.Invalid(
                "Eyes appear closed.\nPlease keep your eyes open."
            )
        }

        // 3. Face must not be too small (too far from camera)
        val widthRatio = box.width() / bitmapW
        if (widthRatio < 0.20f) {
            return ValidationResult.Invalid(
                "Face is too far away.\nMove closer to the camera and retake."
            )
        }

        // 4. Face must not be too large (too close / cropped)
        if (widthRatio > 0.85f) {
            return ValidationResult.Invalid(
                "Face is too close.\nStep back slightly and retake."
            )
        }

        // 5. Head must not be turned too much left/right
        if (abs(face.headEulerAngleY) > 20f) {
            return ValidationResult.Invalid(
                "Head is turned sideways.\nFace the camera directly."
            )
        }

        // 6. Head must not be tilted up/down too much
        if (abs(face.headEulerAngleX) > 15f) {
            return ValidationResult.Invalid(
                "Head is tilted up or down.\nKeep your head level."
            )
        }

        // 7. Head must not be tilted ear-to-shoulder
        if (abs(face.headEulerAngleZ) > 20f) {
            return ValidationResult.Invalid(
                "Head is tilted sideways.\nKeep your head straight."
            )
        }

        // 8. Face should be roughly centered in the photo
        val faceCenterX = box.centerX() / bitmapW
        val faceCenterY = box.centerY() / bitmapH
        if (faceCenterX !in 0.25f..0.75f || faceCenterY !in 0.20f..0.80f) {
            return ValidationResult.Invalid(
                "Face is not centered.\nPosition your face in the middle of the photo."
            )
        }

        // 9. Nose must be visible (catches masked/covered faces)
        if (face.getLandmark(FaceLandmark.NOSE_BASE) == null) {
            return ValidationResult.Invalid(
                "Face is partially covered.\nRemove any mask or obstruction."
            )
        }

        return ValidationResult.Valid
    }
}
