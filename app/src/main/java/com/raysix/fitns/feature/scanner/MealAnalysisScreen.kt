package com.raysix.fitns.feature.scanner

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raysix.fitns.core.design.AdaptiveGutterLayout
import com.raysix.fitns.core.design.ErrorBanner
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionTitle
import com.raysix.fitns.domain.model.MealType
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealAnalysisScreen(
    viewModel: MealAnalysisViewModel = hiltViewModel(),
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AdaptiveGutterLayout(
        header = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScreenHeader(
                    title = when (state.phase) {
                        MealAnalysisPhase.Idle -> "Scan Meal"
                        MealAnalysisPhase.Analyzing -> "Analyzing Meal"
                        MealAnalysisPhase.Review, MealAnalysisPhase.Saving -> "Review Meal"
                    },
                    subtitle = when (state.phase) {
                        MealAnalysisPhase.Idle -> "Capture a clear photo. Analysis starts automatically."
                        MealAnalysisPhase.Analyzing -> "Finding foods and estimating their nutrition."
                        MealAnalysisPhase.Review, MealAnalysisPhase.Saving -> "Check the macros and choose where to log the meal."
                    }
                )
            }
        },
        gutter = {
            when (state.phase) {
                MealAnalysisPhase.Idle -> CameraCaptureView(
                    onImageBytes = viewModel::onImageCaptured,
                    captureButtonLabel = "Capture meal",
                    onCancel = onClose
                )
                MealAnalysisPhase.Analyzing,
                MealAnalysisPhase.Review,
                MealAnalysisPhase.Saving -> {
                    state.previewBitmap?.let { bitmap ->
                        ModernCard {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Captured meal",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 380.dp)
                            )
                        }
                    }
                }
            }
        },
        main = {
            if (state.phase == MealAnalysisPhase.Analyzing) {
                ModernCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Analyzing your meal…", fontWeight = FontWeight.SemiBold)
                            Text(
                                "This usually takes a few seconds.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            state.errorMessage?.let { message ->
                ErrorBanner(message = message)
            }

            if (state.phase == MealAnalysisPhase.Review || state.phase == MealAnalysisPhase.Saving) {
                MealMacroSummary(state.items)
                ModernCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("Meal type")
                        Text(
                            "Choose where these foods should be logged.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack).forEach { type ->
                                FilterChip(
                                    selected = state.mealType == type,
                                    onClick = { viewModel.onMealTypeChange(type) },
                                    enabled = !state.loading,
                                    label = { Text(type.name) }
                                )
                            }
                        }
                    }
                }
                state.disclaimer?.let { disclaimer ->
                    ModernCard {
                        Text(
                            text = disclaimer,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                SectionTitle("Detected items")
                state.items.forEach { item ->
                    AnalysisItemCard(
                        item = item,
                        onUpdate = { grams, calories, protein, carbs, fat ->
                            viewModel.updateItem(item.id, grams, calories, protein, carbs, fat)
                        },
                        onRemove = { viewModel.removeItem(item.id) }
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.save(onClose) },
                        enabled = !state.loading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.loading) "Saving..." else "Save ${state.items.size} items")
                    }
                    OutlinedButton(onClick = onClose) {
                        Text("Cancel")
                    }
                }
            }
        }
    )
}

@Composable
private fun MealMacroSummary(items: List<EditableMealItem>) {
    val calories = items.sumOf { it.calories.toDoubleOrNull() ?: 0.0 }
    val protein = items.sumOf { it.protein.toDoubleOrNull() ?: 0.0 }
    val carbs = items.sumOf { it.carbs.toDoubleOrNull() ?: 0.0 }
    val fat = items.sumOf { it.fat.toDoubleOrNull() ?: 0.0 }

    ModernCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Estimated meal totals")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MealMacroMetric("Calories", "${calories.roundToInt()} kcal", Modifier.weight(1f))
                MealMacroMetric("Protein", "${protein.roundToInt()} g", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MealMacroMetric("Carbohydrates", "${carbs.roundToInt()} g", Modifier.weight(1f))
                MealMacroMetric("Fat", "${fat.roundToInt()} g", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MealMacroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AnalysisItemCard(
    item: EditableMealItem,
    onUpdate: (String, String, String, String, String) -> Unit,
    onRemove: () -> Unit
) {
    ModernCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Confidence ${(item.confidence * 100).roundToInt()}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditField(item.grams, { onUpdate(it, item.calories, item.protein, item.carbs, item.fat) }, "Grams", Modifier.weight(1f))
                EditField(item.calories, { onUpdate(item.grams, it, item.protein, item.carbs, item.fat) }, "kcal", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditField(item.protein, { onUpdate(item.grams, item.calories, it, item.carbs, item.fat) }, "Protein g", Modifier.weight(1f))
                EditField(item.carbs, { onUpdate(item.grams, item.calories, item.protein, it, item.fat) }, "Carbs g", Modifier.weight(1f))
                EditField(item.fat, { onUpdate(item.grams, item.calories, item.protein, item.carbs, it) }, "Fat g", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EditField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true
    )
}
