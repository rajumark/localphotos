package com.localphotos.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PhotoFilterChips(
    selectedFilter: PhotoFilter,
    onFilterSelected: (PhotoFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        FilterChip(
            selected = selectedFilter == PhotoFilter.WithText,
            onClick = {
                onFilterSelected(
                    if (selectedFilter == PhotoFilter.WithText) PhotoFilter.All else PhotoFilter.WithText
                )
            },
            label = { Text("With text") },
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

enum class PhotoFilter(val label: String) {
    All("All"),
    WithText("With text")
}
