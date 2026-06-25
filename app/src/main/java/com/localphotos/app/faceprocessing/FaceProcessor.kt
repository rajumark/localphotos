package com.localphotos.app.faceprocessing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FaceProcessor(private val context: Context) {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
            .build()
    )

    suspend fun detectFacesFromUri(uri: Uri): FaceDetectionResult {
        val bitmap = loadBitmapFromUri(uri) ?: return FaceDetectionResult(emptyList())
        return detectFacesFromBitmap(bitmap)
    }

    private suspend fun detectFacesFromBitmap(bitmap: Bitmap): FaceDetectionResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            detector.process(image)
                .addOnSuccessListener { faces ->
                    val rects = faces.map { face ->
                        val bounds = face.boundingBox
                        FaceRect(bounds.left, bounds.top, bounds.right, bounds.bottom)
                    }
                    cont.resume(FaceDetectionResult(rects)) {}
                }
                .addOnFailureListener {
                    cont.resume(FaceDetectionResult(emptyList())) {}
                }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 4
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, opts)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        detector.close()
    }
}

data class FaceRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class FaceDetectionResult(
    val faces: List<FaceRect>
)
