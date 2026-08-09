package com.raysix.fitns.feature.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScreenHeader(
                    title = "Meal Analysis",
                    subtitle = "Photo analysis estimates are approximations. Review everything before saving."
                )
            }
        }

        if (state.phase == MealAnalysisPhase.Idle || state.phase == MealAnalysisPhase.Analyzing) {
            item {
                CameraCaptureView(
                    onImageBytes = viewModel::onImageCaptured,
                    captureButtonLabel = if (state.previewBitmap != null) "Retake photo" else "Capture photo"
                )
            }
            state.previewBitmap?.let { bitmap ->
                item {
                    ModernCard {
                        Text(
                            "Photo captured. Confirm consent below before analysis.",
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                ModernCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.consentGranted,
                                onCheckedChange = viewModel::onConsentChange
                            )
                            Text(
                                "I agree that this photo is uploaded to my configured n8n instance for analysis.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SectionTitle("Meal type")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack).forEach { type ->
                                FilterChip(
                                    selected = state.mealType == type,
                                    onClick = { viewModel.onMealTypeChange(type) },
                                    label = { Text(type.name) }
                                )
                            }
                        }
                        Button(
                            onClick = viewModel::analyze,
                            enabled = state.consentGranted && !state.loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (state.loading) "Analyzing..." else "Analyze meal"
                            )
                        }
                    }
                }
            }
        }

        state.errorMessage?.let { message ->
            item {
                ErrorBanner(message = message)
            }
        }

        if (state.phase == MealAnalysisPhase.Review) {
            state.disclaimer?.let { disclaimer ->
                item {
                    ModernCard {
                        Text(
                            text = disclaimer,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            item {
                SectionTitle("Detected items")
            }
            items(state.items, key = { it.id }) { item ->
                AnalysisItemCard(
                    item = item,
                    onUpdate = { grams, calories, protein, carbs, fat ->
                        viewModel.updateItem(item.id, grams, calories, protein, carbs, fat)
                    },
                    onRemove = { viewModel.removeItem(item.id) }
                )
            }
            item {
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
