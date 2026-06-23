package com.localphotos.app.ui.detail

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

    fun loadPhoto(uri: String) {
        viewModelScope.launch {
            _photo.value = repository.getPhotoByUri(uri)
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
}
