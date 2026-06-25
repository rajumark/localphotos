package com.localphotos.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.localphotos.app.data.repository.PhotoRepository
import kotlin.math.abs
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    uri: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = koinViewModel()
) {
    val photo by viewModel.photo.collectAsState()
    val allUris by viewModel.allUris.collectAsState()
    val photoDetails by viewModel.photoDetails.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTextSheet by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val initialPage = allUris.indexOf(uri).coerceAtLeast(0)
    val pagerState = rememberPagerState(pageCount = { allUris.size }, initialPage = initialPage)

    LaunchedEffect(uri) {
        viewModel.loadPhoto(uri)
    }

    LaunchedEffect(allUris) {
        if (allUris.isNotEmpty()) {
            val idx = allUris.indexOf(uri).coerceAtLeast(0)
            pagerState.scrollToPage(idx)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageUri = allUris.getOrNull(page)
            if (pageUri != null) {
                AsyncImage(
                    model = pageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            val pageUri = allUris.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
            viewModel.loadPhoto(pageUri)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.8f))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { onBack() },
                        onVerticalDrag = { _, _ -> }
                    )
                }
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { showTextSheet = true },
                        onVerticalDrag = { _, _ -> }
                    )
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionItem(
                icon = { Icon(Icons.AutoMirrored.Filled.TextSnippet, contentDescription = "Text", tint = Color.White, modifier = Modifier.size(24.dp)) },
                label = "Text",
                onClick = { showTextSheet = true }
            )

            ActionItem(
                icon = { Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(24.dp)) },
                label = "Share",
                onClick = { viewModel.sharePhoto(context) }
            )

            ActionItem(
                icon = { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(24.dp)) },
                label = "Delete",
                onClick = { showDeleteDialog = true }
            )

            if (photo != null) {
                ActionItem(
                    icon = {
                        Icon(
                            if (photo!!.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (photo!!.isFavorite) Color(0xFFFF5252) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = if (photo!!.isFavorite) "Favorited" else "Favorite",
                    onClick = { viewModel.toggleFavorite() }
                )
            }

            Box {
                ActionItem(
                    icon = { Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White, modifier = Modifier.size(24.dp)) },
                    label = "More",
                    onClick = { showOverflowMenu = true }
                )
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open in Lens") },
                        onClick = {
                            showOverflowMenu = false
                            viewModel.openInLens(context)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Details") },
                        onClick = {
                            showOverflowMenu = false
                            viewModel.loadDetails(context)
                            showDetailsSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this photo?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deletePhoto(onBack)
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

    if (showTextSheet && photo != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTextSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extracted text",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(photo!!.text))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("Copy all")
                    }
                }

                Spacer(Modifier.height(16.dp))

                SelectionContainer {
                    Text(
                        text = if (photo!!.text.isNotBlank()) photo!!.text else "No text detected",
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }

    if (showDetailsSheet && photoDetails != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            sheetState = sheetState
        ) {
            DetailsContent(photoDetails = photoDetails!!)
        }
    }
}

@Composable
private fun DetailsContent(photoDetails: PhotoRepository.PhotoDetails) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Photo details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(16.dp))

        DetailRow("Name", photoDetails.displayName)
        DetailRow("Date added", photoDetails.dateAdded)
        DetailRow("Date modified", photoDetails.dateModified)
        DetailRow("Type", photoDetails.mimeType)
        DetailRow("Size", photoDetails.fileSize)
        DetailRow("Resolution", photoDetails.resolution)
        DetailRow("Album", photoDetails.bucketDisplayName)
        DetailRow("URI", photoDetails.uri)
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.35f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.65f),
                textAlign = TextAlign.End
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun ActionItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        icon()
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
    }
}
