package com.localphotos.app.ui.labels

import androidx.compose.runtime.Composable
import com.localphotos.app.ui.main.MainScreen

@Composable
fun LabelDetailScreen(
    label: String,
    onPhotoClick: (String) -> Unit,
    onBack: () -> Unit
) {
    MainScreen(
        onPhotoClick = onPhotoClick,
        title = label,
        label = label,
        onBack = onBack
    )
}
