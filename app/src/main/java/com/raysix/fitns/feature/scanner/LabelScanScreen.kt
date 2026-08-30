package com.raysix.fitns.feature.scanner

import android.app.Activity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.input.toUserDecimalOrNull
import com.raysix.fitns.feature.nutrition.FoodLoggingTopBar
import com.raysix.fitns.feature.nutrition.InlineStatus
import com.raysix.fitns.feature.nutrition.InlineStatusKind
import com.raysix.fitns.feature.nutrition.MacroSummary
import com.raysix.fitns.feature.nutrition.ManualFoodInput
import com.raysix.fitns.feature.nutrition.PersistentFoodActionBar
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt

@Composable
fun LabelScanScreen(
    viewModel: LabelScanViewModel = hiltViewModel(),
    onApply: (ManualFoodInput) -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reading = state.phase == LabelScanPhase.Capture && state.loading

    Box(modifier = Modifier.fillMaxSize().testTag("label_scan_screen")) {
        when {
            reading -> {
                DarkScannerSystemBars()
                LabelReadingContent(state = state, onCancel = onCancel)
            }
            state.phase == LabelScanPhase.Capture -> LabelCaptureContent(
                state = state,
                onImageCaptured = viewModel::onImageCaptured,
                onCancel = onCancel
            )
            else -> LabelReviewScreen(state = state, viewModel = viewModel, onApply = onApply, onCancel = onCancel)
        }
    }
}

@Composable
private fun LabelCaptureContent(
    state: LabelScanUiState,
    onImageCaptured: (ByteArray) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    DarkScannerSystemBars()
    CameraCaptureView(
        onImageBytes = onImageCaptured,
        captureButtonLabel = "Read label",
        guide = CameraCaptureGuide.NutritionLabel,
        onCancel = onCancel,
        fullScreen = true,
        externalErrorMessage = state.errorMessage,
        modifier = modifier.fillMaxSize().testTag("label_camera")
    )
}

@Composable
private fun LabelReadingContent(
    state: LabelScanUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        state.previewBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Captured nutrition label being read",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.68f),
                            0.3f to Color.Black.copy(alpha = 0.16f),
                            1f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        Surface(
            onClick = onCancel,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.62f),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(16.dp)
                .size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = "Close label scanner", modifier = Modifier.size(25.dp))
            }
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(42.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Reading every row", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Finding the nutrition basis, units, and nutrients.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.76f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelReviewScreen(
    state: LabelScanUiState,
    viewModel: LabelScanViewModel,
    onApply: (ManualFoodInput) -> Unit,
    onCancel: () -> Unit
) {
    val requiredNutritionIsValid = listOf(state.calories, state.protein, state.carbs, state.fat).all { value ->
        value.toUserDecimalOrNull()?.let { it >= 0.0 } == true
    }
    val optionalNutritionIsValid = listOf(
        state.sugar,
        state.saturatedFat,
        state.fiber,
        state.salt,
        state.sodium
    ).all { value ->
        value.isBlank() || value.toUserDecimalOrNull()?.let { it >= 0.0 } == true
    }
    val canApply = state.basisGrams.toUserDecimalOrNull()?.let { it > 0.0 } == true &&
        requiredNutritionIsValid && optionalNutritionIsValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        FoodLoggingTopBar(
            title = "Review nutrition",
            subtitle = "Correct anything the scanner missed",
            onBack = onCancel
        )
        LabelReviewContent(state = state, viewModel = viewModel, modifier = Modifier.weight(1f))
        PersistentFoodActionBar(
            label = "Use values • ${(state.calories.toUserDecimalOrNull() ?: 0.0).roundToInt()} kcal",
            supportingText = "${state.basisGrams.ifBlank { "—" }} g nutrition basis",
            enabled = canApply,
            loading = false,
            onClick = { viewModel.apply(onApply) },
            modifier = Modifier.imePadding()
        )
    }
}

@Composable
private fun DarkScannerSystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as Activity).window
    val controller = WindowCompat.getInsetsController(window, view)
    val previousLightStatus = controller.isAppearanceLightStatusBars
    val previousLightNavigation = controller.isAppearanceLightNavigationBars
    DisposableEffect(view) {
        onDispose {
            controller.isAppearanceLightStatusBars = previousLightStatus
            controller.isAppearanceLightNavigationBars = previousLightNavigation
        }
    }
    SideEffect {
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
}

@Composable
private fun LabelReviewContent(
    state: LabelScanUiState,
    viewModel: LabelScanViewModel,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        val visual: @Composable ColumnScope.() -> Unit = {
            LabelPhoto(state)
            MacroSummary(
                calories = state.calories.toUserDecimalOrNull() ?: 0.0,
                protein = state.protein.toUserDecimalOrNull() ?: 0.0,
                carbohydrates = state.carbs.toUserDecimalOrNull() ?: 0.0,
                fat = state.fat.toUserDecimalOrNull() ?: 0.0
            )
            state.warnings.forEach { warning -> InlineStatus(warning, InlineStatusKind.Error) }
            OutlinedButton(onClick = viewModel::retake, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Text("Retake photo", modifier = Modifier.padding(start = 8.dp))
            }
        }
        val editor: @Composable ColumnScope.() -> Unit = {
            LabelEditor(state, viewModel)
        }
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LabelScrollPane(Modifier.weight(0.4f).fillMaxHeight(), visual)
                LabelScrollPane(Modifier.weight(0.6f).fillMaxHeight(), editor)
            }
        } else {
            LabelScrollPane(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                visual()
                editor()
            }
        }
    }
}

@Composable
private fun LabelScrollPane(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
private fun LabelPhoto(state: LabelScanUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        state.previewBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Captured nutrition label",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).padding(8.dp).clip(RoundedCornerShape(20.dp))
            )
        } ?: Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
        }
    }
}

@Composable
private fun LabelEditor(state: LabelScanUiState, viewModel: LabelScanViewModel) {
    SectionCard(title = "Product & basis", subtitle = "Tell us what the printed values refer to.") {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Product name") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().testTag("label_name_input")
        )
        LabelNumericField(
            value = state.basisGrams,
            onValueChange = viewModel::onBasisGramsChange,
            label = if (state.perPortion) "Portion weight" else "Nutrition basis",
            suffix = "g",
            positive = true,
            supporting = if (state.perPortion) {
                "Enter the weight of the printed portion."
            } else {
                "Usually 100 g on packaged foods."
            }
        )
    }

    SectionCard(title = "Detected nutrition", subtitle = "Edit the totals shown on the label.") {
        LabelNumericField(state.calories, viewModel::onCaloriesChange, "Energy", "kcal")
        LabelFieldPair(
            first = { LabelNumericField(state.protein, viewModel::onProteinChange, "Protein", "g") },
            second = { LabelNumericField(state.carbs, viewModel::onCarbsChange, "Carbs", "g") }
        )
        LabelFieldPair(
            first = { LabelNumericField(state.fat, viewModel::onFatChange, "Fat", "g") },
            second = { LabelNumericField(state.sugar, viewModel::onSugarChange, "Sugar", "g") }
        )
        LabelFieldPair(
            first = { LabelNumericField(state.saturatedFat, viewModel::onSaturatedFatChange, "Saturated fat", "g") },
            second = { LabelNumericField(state.fiber, viewModel::onFiberChange, "Fiber", "g") }
        )
        LabelFieldPair(
            first = { LabelNumericField(state.salt, viewModel::onSaltChange, "Salt", "g") },
            second = { LabelNumericField(state.sodium, viewModel::onSodiumChange, "Sodium", "mg") }
        )
    }

    if (state.micronutrients.values.isNotEmpty()) {
        SectionCard(title = "Micronutrients", subtitle = "Recognized estimates from the label.") {
            state.micronutrients.values.entries.sortedBy { it.key.name }.forEach { (key, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(key.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${value.amount.formatPlain()} ${key.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelFieldPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 390.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                first()
                second()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { first() }
                Box(Modifier.weight(1f)) { second() }
            }
        }
    }
}

@Composable
private fun LabelNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    positive: Boolean = false,
    supporting: String? = null
) {
    val parsed = value.toUserDecimalOrNull()
    val invalid = value.isNotBlank() && (parsed == null || if (positive) parsed <= 0.0 else parsed < 0.0)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        isError = invalid,
        supportingText = when {
            invalid -> ({ Text(if (positive) "Enter an amount above zero" else "Enter zero or a positive number") })
            supporting != null -> ({ Text(supporting) })
            else -> null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun Double.formatPlain(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
