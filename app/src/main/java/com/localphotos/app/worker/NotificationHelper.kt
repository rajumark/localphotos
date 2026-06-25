package com.localphotos.app.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "photo_processing"
        const val PROGRESS_NOTIFICATION_ID = 1
        const val READY_NOTIFICATION_ID = 2
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Photo Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows photo processing progress"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildProgressNotification(remaining: Int, total: Int, etaMinutes: Int): Notification {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val etaText = if (remaining > 0 && etaMinutes > 0) "~${etaMinutes}m"
                      else if (remaining > 0) "~?m"
                      else ""

        val contentText = if (etaText.isNotEmpty()) "$remaining : $etaText"
                          else "$remaining"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Processing photos")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setProgress(total, total - remaining, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun buildReadyNotification(): Notification {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Ready to search")
            .setContentText("All photos have been processed")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun showNotification(id: Int, notification: Notification) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}
