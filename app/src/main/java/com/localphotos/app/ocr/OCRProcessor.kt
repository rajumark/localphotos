package com.localphotos.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi

class OCRProcessor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromUri(uri: Uri): String {
        val bitmap = loadBitmapFromUri(uri) ?: return ""
        return extractTextFromBitmap(bitmap)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun extractTextFromBitmap(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText.text) {}
                }
                .addOnFailureListener {
                    cont.resume("") {}
                }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 2
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
        recognizer.close()
    }
}
