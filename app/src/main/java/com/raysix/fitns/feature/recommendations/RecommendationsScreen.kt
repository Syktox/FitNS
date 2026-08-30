package com.raysix.fitns.feature.recommendations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.BrandGradient
import com.raysix.fitns.core.design.EmptyStateCard
import com.raysix.fitns.core.design.GradientHeroCard
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
                title = "Coaching current",
                subtitle = "Clear, cautious guidance shaped by the signals you log.",
                actions = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        main = {
            CoachingCurrentCard(uiState.recommendations)
            if (uiState.recommendations.isEmpty()) {
                EmptyStateCard(
                    title = "Calm waters for now",
                    message = "Keep logging meals, training, and body weight. Useful coaching signals will surface here."
                )
            } else {
                uiState.recommendations.forEach { recommendation -> RecommendationCard(recommendation) }
            }
        },
        side = {
            SectionCard(
                title = "Your compass",
                subtitle = "Use patterns as direction, not as a verdict.",
                accent = true
            ) {
                GuidanceLine(
                    icon = Icons.Outlined.Waves,
                    title = "Watch the trend",
                    body = "One unusual day matters less than a steady current."
                )
                GuidanceLine(
                    icon = Icons.Outlined.Lightbulb,
                    title = "Choose one action",
                    body = "Small changes are easier to repeat and easier to evaluate."
                )
            }
            SectionCard(title = "Health note", subtitle = "Keep expert care in the loop when needed.") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Outlined.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "FitNS does not diagnose conditions or recommend medication. Review unusual or potentially risky health data with a medical professional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun CoachingCurrentCard(recommendations: List<RecommendationItem>) {
    val attentionCount = recommendations.count { it.severity == RecommendationSeverity.Attention }
    val positiveCount = recommendations.count { it.severity == RecommendationSeverity.Positive }
    GradientHeroCard(brush = BrandGradient) {
        val onPrimary = MaterialTheme.colorScheme.onPrimary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = onPrimary.copy(alpha = 0.16f),
                contentColor = onPrimary,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (recommendations.isEmpty()) "Building your baseline" else "${recommendations.size} signals in view",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary
                )
                Text(
                    when {
                        recommendations.isEmpty() -> "More consistent logs make the next insight sharper."
                        attentionCount > 0 -> "$attentionCount need attention · $positiveCount moving well"
                        positiveCount > 0 -> "$positiveCount positive currents · stay consistent"
                        else -> "Use these observations to choose your next small step."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onPrimary.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: RecommendationItem) {
    val visual = recommendation.severity.visual()
    ModernCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = visual.color.copy(alpha = 0.14f),
                    contentColor = visual.color,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(visual.icon, contentDescription = null, modifier = Modifier.size(23.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        recommendation.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(recommendation.message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                SeverityChip(visual.label, visual.color)
            }
            Text(
                recommendation.rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Signal confidence", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${(recommendation.confidence.coerceIn(0.0, 1.0) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = visual.color
                    )
                }
                LinearProgressIndicator(
                    progress = { recommendation.confidence.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = visual.color,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

@Composable
private fun GuidanceLine(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SeverityChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.14f), contentColor = color) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private data class RecommendationVisual(val label: String, val color: Color, val icon: ImageVector)

@Composable
private fun RecommendationSeverity.visual(): RecommendationVisual = when (this) {
    RecommendationSeverity.Positive -> RecommendationVisual(
        label = "On course",
        color = MaterialTheme.colorScheme.primary,
        icon = Icons.Outlined.CheckCircle
    )
    RecommendationSeverity.Attention -> RecommendationVisual(
        label = "Check in",
        color = MaterialTheme.colorScheme.error,
        icon = Icons.Outlined.WarningAmber
    )
    RecommendationSeverity.Info -> RecommendationVisual(
        label = "Insight",
        color = MaterialTheme.colorScheme.tertiary,
        icon = Icons.Outlined.Info
    )
}
