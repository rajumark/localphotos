package com.localphotos.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.androidx.compose.koinViewModel
import com.localphotos.app.ui.components.PhotoFilterChips
import com.localphotos.app.ui.components.PhotoGrid
import com.localphotos.app.ui.components.PhotoSearchBar

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
    val photosFlow by viewModel.photos.collectAsState()
    val photos = photosFlow.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.onResume()
    }

    Column(modifier = modifier.fillMaxSize()) {
        PhotoSearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            placeholder = "Search photos by text…"
        )

        PhotoFilterChips(
            selectedFilter = selectedFilter,
            onFilterSelected = viewModel::onFilterChange
        )

        if (pendingCount > 0) {
            Text(
                text = "$pendingCount pending",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        if (photos.itemCount == 0 && !photos.loadState.refresh.endOfPaginationReached) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else if (photos.itemCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No photos found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            PhotoGrid(
                photos = photos.itemSnapshotList.filterNotNull(),
                onPhotoClick = onPhotoClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
