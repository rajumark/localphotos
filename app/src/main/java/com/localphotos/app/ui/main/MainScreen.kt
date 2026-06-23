package com.localphotos.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import com.localphotos.app.data.local.entities.PhotoEntity
import com.localphotos.app.data.local.entities.PhotoGroup
import com.localphotos.app.ui.components.PhotoFilterChips
import com.localphotos.app.ui.components.PhotoSearchBar
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = koinViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val photoGroups by viewModel.photos.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(photoGroups) {
        if (photoGroups.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onResume()
    }

    Column(modifier = modifier.fillMaxSize()) {
        PhotoSearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            placeholder = "Search photos by text\u2026"
        )

        PhotoFilterChips(
            selectedFilter = selectedFilter,
            onFilterSelected = viewModel::onFilterChange
        )

        if (pendingCount > 0 && isProcessing) {
            Text(
                text = "Processing: $pendingCount remaining",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        if (photoGroups.isEmpty() && !isProcessing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No photos found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else if (photoGroups.isEmpty() && isProcessing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                photoGroups.forEach { group ->
                    item(key = "header_${group.header}") {
                        SectionHeader(header = group.header)
                    }
                    item(key = "photos_${group.header}") {
                        PhotoSectionGrid(
                            photos = group.photos,
                            onPhotoClick = onPhotoClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(header: String) {
    Text(
        text = header,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun PhotoSectionGrid(
    photos: List<PhotoEntity>,
    onPhotoClick: (String) -> Unit
) {
    val chunked = photos.chunked(3)
    Column(modifier = Modifier.fillMaxWidth()) {
        chunked.forEach { rowPhotos ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                rowPhotos.forEach { photo ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onPhotoClick(photo.uri) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                if (rowPhotos.size < 3) {
                    repeat(3 - rowPhotos.size) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}
