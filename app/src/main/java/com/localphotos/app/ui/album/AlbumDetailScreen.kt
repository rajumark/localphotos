package com.localphotos.app.ui.album

import androidx.compose.runtime.Composable
import com.localphotos.app.ui.main.MainScreen

@Composable
fun AlbumDetailScreen(
    bucketId: String,
    albumName: String,
    onPhotoClick: (String) -> Unit,
    onBack: () -> Unit
) {
    MainScreen(
        onPhotoClick = onPhotoClick,
        title = albumName,
        bucketId = bucketId,
        onBack = onBack
    )
}
