package com.localphotos.app.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.androidx.compose.koinViewModel
import com.localphotos.app.ui.components.PhotoGrid
import com.localphotos.app.ui.components.PhotoSearchBar

@Composable
fun FavoritesScreen(
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val photos = viewModel.photos.collectAsLazyPagingItems()

    Column(modifier = modifier.fillMaxSize()) {
        PhotoSearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            placeholder = "Search favorites…"
        )

        when {
            photos.loadState.refresh is LoadState.Loading && photos.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
            photos.loadState.refresh is LoadState.Error && photos.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No favorites yet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            photos.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No favorites yet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                PhotoGrid(
                    photos = photos.itemSnapshotList.filterNotNull(),
                    onPhotoClick = onPhotoClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
