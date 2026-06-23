package com.localphotos.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.localphotos.app.di.appModule
import com.localphotos.app.worker.OCRWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LocalPhotosApp : Application() {
    override fun onCreate() {
        super.onCreate()
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
