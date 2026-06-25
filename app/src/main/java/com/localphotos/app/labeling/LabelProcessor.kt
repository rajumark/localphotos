package com.localphotos.app.labeling

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LabelProcessor(private val context: Context) {

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    suspend fun extractLabelsFromUri(uri: Uri): List<LabelResult> {
        val bitmap = loadBitmapFromUri(uri) ?: return emptyList()
        return extractLabelsFromBitmap(bitmap)
    }

    private suspend fun extractLabelsFromBitmap(bitmap: Bitmap): List<LabelResult> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    cont.resume(labels.map { LabelResult(it.text, it.confidence) }) {}
                }
                .addOnFailureListener {
                    cont.resume(emptyList()) {}
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
        labeler.close()
    }
}

data class LabelResult(
    val text: String,
    val confidence: Float
)
