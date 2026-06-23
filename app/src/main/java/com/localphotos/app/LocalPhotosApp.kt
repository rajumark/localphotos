package com.localphotos.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.localphotos.app.di.appModule
import com.localphotos.app.worker.OCRWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

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

        val ocrWork = OneTimeWorkRequestBuilder<OCRWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresStorageNotLow(true)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "ocr_processing",
                ExistingWorkPolicy.KEEP,
                ocrWork
            )
    }
}
