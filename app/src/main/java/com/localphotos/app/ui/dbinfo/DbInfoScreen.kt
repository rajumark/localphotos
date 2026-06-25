package com.localphotos.app.ui.dbinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DbInfoScreen(
    onBack: () -> Unit,
    viewModel: DbInfoViewModel = koinViewModel()
) {
    val totalPhotos by viewModel.totalPhotos.collectAsState(initial = 0)
    val textProcessed by viewModel.textProcessed.collectAsState(initial = 0)
    val labelProcessed by viewModel.labelProcessed.collectAsState(initial = 0)
    val faceProcessed by viewModel.faceProcessed.collectAsState(initial = 0)
    val fullyProcessed by viewModel.fullyProcessed.collectAsState(initial = 0)
    val distinctLabels by viewModel.distinctLabels.collectAsState(initial = 0)
    val totalLabelAssignments by viewModel.totalLabelAssignments.collectAsState(initial = 0)
    val totalFacesFound by viewModel.totalFacesFound.collectAsState(initial = 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DB Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Photo Processing")
            StatCard("Total Photos", totalPhotos, MaterialTheme.colorScheme.primary)
            StatCard("Fully Processed", fullyProcessed, MaterialTheme.colorScheme.tertiary)
            StatCard("OCR Processed", textProcessed, MaterialTheme.colorScheme.secondary)
            StatCard("Label Processed", labelProcessed, MaterialTheme.colorScheme.secondary)
            StatCard("Face Processed", faceProcessed, MaterialTheme.colorScheme.secondary)

            Spacer(Modifier.height(8.dp))
            SectionHeader("Label Stats")
            StatCard("Distinct Labels", distinctLabels, MaterialTheme.colorScheme.primary)
            StatCard("Total Label Assignments", totalLabelAssignments, MaterialTheme.colorScheme.secondary)

            Spacer(Modifier.height(8.dp))
            SectionHeader("Face Stats")
            StatCard("Total Faces Found", totalFacesFound, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun StatCard(label: String, value: Int, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
