package com.localphotos.app.di

import androidx.room.Room
import com.localphotos.app.data.local.AppDatabase
import com.localphotos.app.data.local.PhotoDao
import com.localphotos.app.data.repository.PhotoRepository
import com.localphotos.app.data.repository.PhotoRepositoryImpl
import com.localphotos.app.faceprocessing.FaceProcessor
import com.localphotos.app.faceprocessing.FaceStats
import com.localphotos.app.labeling.LabelProcessor
import com.localphotos.app.ocr.OCRProcessor
import com.localphotos.app.ui.detail.DetailViewModel
import com.localphotos.app.ui.favorites.FavoritesViewModel
import com.localphotos.app.ui.albums.AlbumsViewModel
import com.localphotos.app.ui.faces.FaceGridViewModel
import com.localphotos.app.ui.labels.LabelsViewModel
import android.content.Context
import com.localphotos.app.ui.main.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "localphotos.db"
        ).fallbackToDestructiveMigration().build()
    }

    single<PhotoDao> { get<AppDatabase>().photoDao() }

    single { OCRProcessor(androidContext()) }

    single { LabelProcessor(androidContext()) }

    single { FaceProcessor(androidContext()) }

    single { FaceStats() }

    single<PhotoRepository> {
        PhotoRepositoryImpl(
            context = androidContext(),
            photoDao = get(),
            ocrProcessor = get(),
            labelProcessor = get(),
            faceProcessor = get(),
            faceStats = get()
        ) as PhotoRepository
    }

    single { androidContext().getSharedPreferences("local_photos_prefs", Context.MODE_PRIVATE) }

    viewModel { params -> MainViewModel(get(), get(), params.getOrNull()) }
    viewModel { DetailViewModel(get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { LabelsViewModel(get()) }
    viewModel { AlbumsViewModel(get()) }
    viewModel { FaceGridViewModel(get(), get()) }
}
