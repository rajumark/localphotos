package com.localphotos.app

import android.app.Application
import com.localphotos.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LocalPhotosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LocalPhotosApp)
            modules(appModule)
        }
    }
}
