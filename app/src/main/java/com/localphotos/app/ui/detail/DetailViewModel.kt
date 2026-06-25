package com.localphotos.app.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _photo = MutableStateFlow<PhotoEntity?>(null)
    val photo: StateFlow<PhotoEntity?> = _photo.asStateFlow()

    private val _allUris = MutableStateFlow<List<String>>(emptyList())
    val allUris: StateFlow<List<String>> = _allUris.asStateFlow()

    private val _photoDetails = MutableStateFlow<PhotoRepository.PhotoDetails?>(null)
    val photoDetails: StateFlow<PhotoRepository.PhotoDetails?> = _photoDetails.asStateFlow()

    init {
        viewModelScope.launch {
            _allUris.value = repository.getAllPhotoUris()
        }
    }

    fun loadPhoto(uri: String) {
        viewModelScope.launch {
            _photo.value = repository.getPhotoByUri(uri)
        }
    }

    fun loadDetails(context: Context) {
        val current = _photo.value ?: return
        viewModelScope.launch {
            _photoDetails.value = repository.getPhotoDetails(context, current.uri)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val p = _photo.value ?: return@launch
            repository.setFavorite(p.uri, !p.isFavorite)
            _photo.value = p.copy(isFavorite = !p.isFavorite)
        }
    }

    fun deletePhoto(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val p = _photo.value ?: return@launch
            repository.deletePhoto(p.uri)
            onDeleted()
        }
    }

    fun sharePhoto(context: Context) {
        val p = _photo.value ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(p.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share photo"))
    }

    fun openInLens(context: Context) {
        val p = _photo.value ?: return
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(p.uri)
                `package` = "com.google.ar.lens"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.ar.lens"))
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {}
        }
    }
}
