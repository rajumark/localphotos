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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.localphotos.app.data.local.entities.PhotoListItem
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
    val pendingCount by viewModel.pendingCount.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    val currentItems = viewModel.photos.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    LaunchedEffect(searchQuery) {
        if (currentItems.itemCount > 0) {
            listState.scrollToItem(0)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhotoSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search photos by text\u2026",
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.toggleViewMode() }) {
                Icon(
                    if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (isGridView) "List view" else "Grid view",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

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

        when {
            currentItems.loadState.refresh is LoadState.Loading && currentItems.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
            currentItems.loadState.refresh is LoadState.Error && currentItems.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No photos found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            currentItems.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No photos found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(1.dp)
                    ) {
                        items(currentItems.itemCount, key = { index ->
                            when (val item = currentItems[index]) {
                                is PhotoListItem.Photo -> "${item.entity.uri}_$index"
                                else -> index
                            }
                        }) { index ->
                            val item = currentItems[index]
                            if (item is PhotoListItem.Photo) {
                                Card(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(1.dp)
                                        .clickable { onPhotoClick(item.entity.uri) },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(item.entity.uri)
                                            .size(150)
                                            .crossfade(false)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(currentItems.itemCount, key = { index ->
                            when (val item = currentItems[index]) {
                                is PhotoListItem.Header -> "header_${item.label}_$index"
                                is PhotoListItem.Photo -> "${item.entity.uri}_$index"
                                else -> index
                            }
                        }) { index ->
                            when (val item = currentItems[index]) {
                                is PhotoListItem.Header -> {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                is PhotoListItem.Photo -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                            .clickable { onPhotoClick(item.entity.uri) },
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(item.entity.uri)
                                                    .size(300)
                                                    .crossfade(false)
                                                    .build(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(100.dp)
                                                    .aspectRatio(1f),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column(
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .weight(1f)
                                            ) {
                                                if (item.entity.text.isNotBlank()) {
                                                    Text(
                                                        text = item.entity.text,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 2,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = if (item.entity.isFavorite) "\u2605 Favorite" else "",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}
