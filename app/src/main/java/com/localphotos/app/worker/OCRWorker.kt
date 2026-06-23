package com.localphotos.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.localphotos.app.data.repository.PhotoRepository
import org.koin.java.KoinJavaComponent.get

class OCRWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository: PhotoRepository by lazy {
        get(PhotoRepository::class.java)
    }

    override suspend fun doWork(): Result {
        repository.refreshPhotos()

        var processed = 0
        while (processed < 50) {
            val hasMore = repository.processOne()
            if (!hasMore) break
            processed++
        }

        return if (processed > 0) Result.success() else Result.success()
    }
}
