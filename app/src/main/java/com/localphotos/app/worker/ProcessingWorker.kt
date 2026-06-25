package com.localphotos.app.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.localphotos.app.data.repository.PhotoRepository
import kotlinx.coroutines.delay
import org.koin.java.KoinJavaComponent.get

class ProcessingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository: PhotoRepository by lazy { get(PhotoRepository::class.java) }
    private val notificationHelper = NotificationHelper(applicationContext)

    override suspend fun doWork(): Result {
        notificationHelper.createChannel()

        repository.refreshPhotos()

        val totalPhases = repository.getRemainingPhaseCount()
        if (totalPhases == 0) return Result.success()

        val startTime = System.currentTimeMillis()
        var lastNotificationUpdate = 0L

        try {
            setForeground(createForegroundInfo(totalPhases, totalPhases, 0))
        } catch (_: Exception) {
        }

        while (true) {
            if (isStopped) break

            val hasMore = repository.processNextPhoto()
            if (!hasMore) break

            val remaining = repository.getRemainingPhaseCount()
            val completed = totalPhases - remaining
            val now = System.currentTimeMillis()

            if (now - lastNotificationUpdate > 2000 || remaining == 0) {
                val elapsed = now - startTime
                val avg = if (completed > 0) elapsed / completed else 0L
                val eta = if (avg > 0 && remaining > 0) {
                    ((remaining * avg) / 60000).toInt().coerceAtLeast(1)
                } else 0
                try {
                    setForeground(createForegroundInfo(remaining, totalPhases, eta))
                } catch (_: Exception) {
                }
                lastNotificationUpdate = now
            }

            delay(100)
        }

        notificationHelper.showNotification(
            NotificationHelper.READY_NOTIFICATION_ID,
            notificationHelper.buildReadyNotification()
        )

        return Result.success()
    }

    private fun createForegroundInfo(remaining: Int, total: Int, etaMinutes: Int): ForegroundInfo {
        val notification = notificationHelper.buildProgressNotification(remaining, total, etaMinutes)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationHelper.PROGRESS_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NotificationHelper.PROGRESS_NOTIFICATION_ID, notification)
        }
    }
}
