package com.raysix.fitns.feature.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raysix.fitns.core.design.AdaptiveColumn
import com.raysix.fitns.core.design.AdaptiveGutterLayout
import com.raysix.fitns.core.design.ErrorBanner
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionTitle
import com.raysix.fitns.domain.model.NutrientKey
import com.raysix.fitns.feature.nutrition.ManualFoodInput

@Composable
fun LabelScanScreen(
    viewModel: LabelScanViewModel = hiltViewModel(),
    onApply: (ManualFoodInput) -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (state.phase) {
        LabelScanPhase.Capture -> CaptureStep(
            loading = state.loading,
            errorMessage = state.errorMessage,
            onImageCaptured = viewModel::onImageCaptured,
            onCancel = onCancel
        )
        LabelScanPhase.Review -> ReviewStep(
            state = state,
            viewModel = viewModel,
            onApply = onApply
        )
    }
}

@Composable
private fun CaptureStep(
    loading: Boolean,
    errorMessage: String?,
    onImageCaptured: (ByteArray) -> Unit,
    onCancel: () -> Unit
) {
    AdaptiveColumn(
        modifier = Modifier.fillMaxSize(),
        content = {
            ScreenHeader(
                title = "Scan Nutrition Label",
                subtitle = "Photograph the nutrition table and ingredient section. OCR results are only a draft and must be confirmed."
            )
            if (loading) {
                ModernCard {
                    Text(
                        "Recognizing text...",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            } else {
                CameraCaptureView(
                    onImageBytes = onImageCaptured,
                    captureButtonLabel = "Capture label",
                    onCancel = onCancel
                )
            }
            errorMessage?.let { ErrorBanner(message = it) }
        }
    )
}

@Composable
private fun ReviewStep(
    state: LabelScanUiState,
    viewModel: LabelScanViewModel,
    onApply: (ManualFoodInput) -> Unit
) {
    AdaptiveGutterLayout(
        header = {
            ScreenHeader(
                title = "Review Label",
                subtitle = "Correct the recognized values before applying them to your food entry."
            )
        },
        gutter = {
            state.warnings.forEach { warning ->
                ErrorBanner(message = warning)
            }

            state.previewBitmap?.let { bitmap ->
                ModernCard {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionTitle("Recognized photo")
                        androidx.compose.foundation.Image(
                            bitmap = bitmap,
                            contentDescription = "Captured nutrition label",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                        OutlinedButton(onClick = viewModel::retake) {
                            Text("Retake photo")
                        }
                    }
                }
            }
        },
        main = {
            ModernCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Product")
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.basisGrams,
                        onValueChange = viewModel::onBasisGramsChange,
                        label = { Text("Values refer to (g)") },
                        supportingText = {
                            Text(
                                if (state.perPortion) {
                                    "The table is per portion. Enter the portion weight the listed values refer to."
                                } else {
                                    "The table is per 100 g. Enter the amount the listed values refer to."
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            ModernCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Detected nutrition values")
                    LabelNumericField(state.calories, viewModel::onCaloriesChange, "Calories")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelNumericField(state.protein, viewModel::onProteinChange, "Protein g", Modifier.weight(1f))
                        LabelNumericField(state.carbs, viewModel::onCarbsChange, "Carbs g", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelNumericField(state.fat, viewModel::onFatChange, "Fat g", Modifier.weight(1f))
                        LabelNumericField(state.sugar, viewModel::onSugarChange, "Sugar g", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelNumericField(state.saturatedFat, viewModel::onSaturatedFatChange, "Sat. fat g", Modifier.weight(1f))
                        LabelNumericField(state.fiber, viewModel::onFiberChange, "Fiber g", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelNumericField(state.salt, viewModel::onSaltChange, "Salt g", Modifier.weight(1f))
                        LabelNumericField(state.sodium, viewModel::onSodiumChange, "Sodium mg", Modifier.weight(1f))
                    }
                }
            }

            if (state.micronutrients.values.isNotEmpty()) {
                ModernCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionTitle("Recognized micronutrients")
                        state.micronutrients.values.entries
                            .sortedBy { it.key.name }
                            .forEach { (key, value) ->
                                Text(
                                    text = "${key.label}: ${value.amount.formatPlain()} ${key.unit} (estimated)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.apply(onApply) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply to food entry")
                }
                OutlinedButton(onClick = viewModel::retake) {
                    Text("Retake")
                }
            }
        }
    )
}

@Composable
private fun LabelNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

private fun Double.formatPlain(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
