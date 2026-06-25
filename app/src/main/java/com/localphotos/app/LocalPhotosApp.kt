package com.localphotos.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.localphotos.app.di.appModule
import com.localphotos.app.worker.ProcessingWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class LocalPhotosApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val imageLoader = ImageLoader.Builder(this)
            .crossfade(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .build()
        Coil.setImageLoader(imageLoader)

        startKoin {
            androidContext(this@LocalPhotosApp)
            modules(appModule)
        }

        val processingWork = OneTimeWorkRequestBuilder<ProcessingWorker>()
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "photo_processing",
                ExistingWorkPolicy.REPLACE,
                processingWork
            )

        val periodicWork = PeriodicWorkRequestBuilder<ProcessingWorker>(6, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "photo_processing_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
    }
}
