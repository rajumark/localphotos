package com.localphotos.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.localphotos.app.data.local.entities.PhotoListItem
import com.localphotos.app.ui.components.PhotoSearchBar
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val showFavorites by viewModel.showFavorites.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentItems = viewModel.photos.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    BackHandler(enabled = selectedUris.isNotEmpty()) {
        viewModel.clearSelection()
    }

    LaunchedEffect(searchQuery) {
        if (currentItems.itemCount > 0) {
            listState.scrollToItem(0)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (selectedUris.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.clearSelection() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close selection")
                    }
                    Text(
                        text = "${selectedUris.size} Selected",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.shareSelected(context) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
        } else {
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
        }

        if (selectedUris.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showFavorites,
                    onClick = { viewModel.setShowFavorites(false) },
                    label = { Text("Photos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                FilterChip(
                    selected = showFavorites,
                    onClick = { viewModel.setShowFavorites(true) },
                    label = { Text("Favorites") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
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

        if (searchQuery.isNotBlank() && currentItems.itemCount > 0) {
            Text(
                text = "${currentItems.itemCount} result${if (currentItems.itemCount != 1) "s" else ""} for \"$searchQuery\"",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        val isEmpty = currentItems.itemCount == 0
        val isSearching = searchQuery.isNotBlank()

        when {
            currentItems.loadState.refresh is LoadState.Loading && isEmpty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
            currentItems.loadState.refresh is LoadState.Error && isEmpty -> {
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
            isEmpty && isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No results for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            isEmpty -> {
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
                                val isSelected = selectedUris.contains(item.entity.uri)
                                Card(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(1.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (selectedUris.isNotEmpty()) {
                                                    viewModel.toggleSelection(item.entity.uri)
                                                } else {
                                                    onPhotoClick(item.entity.uri)
                                                }
                                            },
                                            onLongClick = {
                                                viewModel.toggleSelection(item.entity.uri)
                                            }
                                        ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
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
                                        if (selectedUris.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .then(
                                                        if (isSelected) Modifier.background(Color.White, CircleShape)
                                                        else Modifier.border(2.dp, Color.White, CircleShape)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.35f))
                                                )
                                            }
                                        }
                                    }
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
                                    val isSelected = selectedUris.contains(item.entity.uri)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    if (selectedUris.isNotEmpty()) {
                                                        viewModel.toggleSelection(item.entity.uri)
                                                    } else {
                                                        onPhotoClick(item.entity.uri)
                                                    }
                                                },
                                                onLongClick = {
                                                    viewModel.toggleSelection(item.entity.uri)
                                                }
                                            ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        colors = if (isSelected) CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ) else CardDefaults.cardColors()
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Box {
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
                                                if (selectedUris.isNotEmpty()) {
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(100.dp)
                                                                .aspectRatio(1f)
                                                                .background(Color.Black.copy(alpha = 0.35f))
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .then(
                                                                if (isSelected) Modifier.background(Color.White, CircleShape)
                                                                else Modifier.border(2.dp, Color.White, CircleShape)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Filled.CheckCircle,
                                                                contentDescription = "Selected",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${selectedUris.size} photo${if (selectedUris.size != 1) "s" else ""}?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteSelected()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
