package com.syktox.fitns.feature.recommendations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syktox.fitns.core.design.EmptyStateCard
import com.syktox.fitns.core.design.ScreenHeader
import com.syktox.fitns.domain.model.RecommendationItem
import com.syktox.fitns.domain.model.RecommendationSeverity

@Composable
fun RecommendationsScreen(uiState: RecommendationsUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Recommendations",
                subtitle = "Cautious guidance from your local data."
            )
        }
        if (uiState.recommendations.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No recommendations yet.",
                    message = "Log nutrition, workouts, and body weight to unlock local guidance."
                )
            }
        }
        items(uiState.recommendations) { recommendation ->
            RecommendationCard(recommendation)
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Note", fontWeight = FontWeight.SemiBold)
                    Text("This app does not provide diagnoses or recommend medication. Unusual or potentially risky health data should be reviewed with a medical professional.")
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: RecommendationItem) {
    val color = when (recommendation.severity) {
        RecommendationSeverity.Positive -> MaterialTheme.colorScheme.primary
        RecommendationSeverity.Attention -> MaterialTheme.colorScheme.error
        RecommendationSeverity.Info -> MaterialTheme.colorScheme.secondary
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(recommendation.category, fontWeight = FontWeight.SemiBold)
                    SeverityChip(recommendation.severity, color)
                }
                Text(recommendation.message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(recommendation.rationale, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Confidence: ${(recommendation.confidence * 100).toInt()} %")
            }
        }
    }
}

@Composable
private fun SeverityChip(severity: RecommendationSeverity, color: Color) {
    val label = when (severity) {
        RecommendationSeverity.Positive -> "Positive"
        RecommendationSeverity.Attention -> "Attention"
        RecommendationSeverity.Info -> "Info"
    }
    SuggestionChip(
        onClick = {},
        label = {
            Text(label, color = color)
        }
    )
}
