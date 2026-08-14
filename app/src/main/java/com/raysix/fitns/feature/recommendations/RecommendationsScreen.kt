package com.raysix.fitns.feature.recommendations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.domain.model.RecommendationItem
import com.raysix.fitns.domain.model.RecommendationSeverity

@Composable
fun RecommendationsScreen(uiState: RecommendationsUiState, onBack: () -> Unit = {}) {
    AdaptiveTwoColumn(
        header = {
            ScreenHeader(
                title = "Recommendations",
                subtitle = "Cautious guidance from your local data.",
                actions = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        main = {
            if (uiState.recommendations.isEmpty()) {
                EmptyStateCard(
                    title = "No recommendations yet.",
                    message = "Log nutrition, workouts, and body weight to unlock local guidance."
                )
            } else {
                uiState.recommendations.forEach { recommendation ->
                    RecommendationCard(recommendation)
                }
            }
        },
        side = {
            SectionCard(title = "Note") {
                Text(
                    "This app does not provide diagnoses or recommend medication. Unusual or potentially risky health data should be reviewed with a medical professional.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun RecommendationCard(recommendation: RecommendationItem) {
    val color = when (recommendation.severity) {
        RecommendationSeverity.Positive -> MaterialTheme.colorScheme.primary
        RecommendationSeverity.Attention -> MaterialTheme.colorScheme.error
        RecommendationSeverity.Info -> MaterialTheme.colorScheme.secondary
    }
    ModernCard {
        Row(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(recommendation.category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SeverityChip(recommendation.severity, color)
                }
                Text(recommendation.message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(recommendation.rationale, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Confidence: ${(recommendation.confidence * 100).toInt()} %", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
