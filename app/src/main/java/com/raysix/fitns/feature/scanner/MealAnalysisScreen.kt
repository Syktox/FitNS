package com.raysix.fitns.feature.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.input.toUserDecimalOrNull
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.feature.nutrition.FoodLoggingTopBar
import com.raysix.fitns.feature.nutrition.InlineStatus
import com.raysix.fitns.feature.nutrition.InlineStatusKind
import com.raysix.fitns.feature.nutrition.MacroSummary
import com.raysix.fitns.feature.nutrition.MealDestinationDialog
import com.raysix.fitns.feature.nutrition.PersistentFoodActionBar
import kotlin.math.roundToInt

@Composable
fun MealAnalysisScreen(
    viewModel: MealAnalysisViewModel = hiltViewModel(),
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onAddManually: () -> Unit,
    onOpenPrivacySettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MealAnalysisContent(
        state = state,
        onImageCaptured = viewModel::onImageCaptured,
        onMealTypeChange = viewModel::onMealTypeChange,
        onUpdateItem = { updated ->
            viewModel.updateItem(
                id = updated.id,
                name = updated.name,
                grams = updated.grams,
                calories = updated.calories,
                protein = updated.protein,
                carbs = updated.carbs,
                fat = updated.fat
            )
        },
        onRemoveItem = viewModel::removeItem,
        onAddItem = viewModel::addItem,
        onRetry = viewModel::retryAnalysis,
        onRetake = viewModel::retake,
        onSave = { viewModel.save(onSaved) },
        onClose = onClose,
        onAddManually = onAddManually,
        onOpenPrivacySettings = onOpenPrivacySettings
    )
}

@Composable
internal fun MealAnalysisContent(
    state: MealAnalysisUiState,
    onImageCaptured: (ByteArray) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onUpdateItem: (EditableMealItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    onAddItem: () -> Unit,
    onRetry: () -> Unit,
    onRetake: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onAddManually: () -> Unit,
    onOpenPrivacySettings: () -> Unit
) {
    var showMealDestinationDialog by rememberSaveable { mutableStateOf(false) }
    var pendingMealTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingMealType = pendingMealTypeName?.let { name ->
        MealType.entries.firstOrNull { it.name == name }
    }

    if (showMealDestinationDialog) {
        MealDestinationDialog(
            selected = pendingMealType,
            onSelected = { pendingMealTypeName = it.name },
            onDismiss = {
                showMealDestinationDialog = false
                pendingMealTypeName = null
            },
            onConfirm = { mealType ->
                showMealDestinationDialog = false
                pendingMealTypeName = null
                onMealTypeChange(mealType)
                onSave()
            },
            title = "Log meal",
            description = "Choose where the meal should appear in today's diary.",
            confirmLabel = "Log meal"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("meal_analysis_screen")
    ) {
        FoodLoggingTopBar(
            title = when (state.phase) {
                MealAnalysisPhase.Capture -> "Scan a meal"
                MealAnalysisPhase.Analyzing -> "Reading your plate"
                MealAnalysisPhase.Review, MealAnalysisPhase.Saving -> "Review meal"
                MealAnalysisPhase.Error -> "Photo needs attention"
            },
            subtitle = when (state.phase) {
                MealAnalysisPhase.Capture -> "Capture every food in one frame"
                MealAnalysisPhase.Analyzing -> "Detecting foods and estimating portions"
                MealAnalysisPhase.Review, MealAnalysisPhase.Saving -> "Tune each estimate before logging"
                MealAnalysisPhase.Error -> "Your photo is still here"
            },
            onBack = onClose
        )

        when (state.phase) {
            MealAnalysisPhase.Capture -> MealCaptureContent(
                onImageCaptured = onImageCaptured,
                modifier = Modifier.weight(1f)
            )
            MealAnalysisPhase.Analyzing -> AnalysisProgressContent(
                state = state,
                modifier = Modifier.weight(1f)
            )
            MealAnalysisPhase.Error -> AnalysisErrorContent(
                state = state,
                onRetry = onRetry,
                onRetake = onRetake,
                onAddManually = onAddManually,
                onOpenPrivacySettings = onOpenPrivacySettings,
                modifier = Modifier.weight(1f)
            )
            MealAnalysisPhase.Review, MealAnalysisPhase.Saving -> {
                MealReviewContent(
                    state = state,
                    onUpdateItem = onUpdateItem,
                    onRemoveItem = onRemoveItem,
                    onAddItem = onAddItem,
                    onRetake = onRetake,
                    modifier = Modifier.weight(1f)
                )
                val foodLabel = if (state.items.size == 1) "food" else "foods"
                PersistentFoodActionBar(
                    label = "Log ${state.items.size} $foodLabel • ${state.totals.calories.roundToInt()} kcal",
                    supportingText = "${state.items.size} selected · Choose meal next",
                    enabled = state.canSave,
                    loading = state.phase == MealAnalysisPhase.Saving || state.loading,
                    onClick = {
                        pendingMealTypeName = null
                        showMealDestinationDialog = true
                    },
                    modifier = Modifier.imePadding()
                )
            }
        }
    }
}

@Composable
private fun MealCaptureContent(
    onImageCaptured: (ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.68f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CameraCaptureView(
                        onImageBytes = onImageCaptured,
                        captureButtonLabel = "Analyze meal",
                        modifier = Modifier.fillMaxWidth().testTag("meal_camera")
                    )
                }
                Column(
                    modifier = Modifier.weight(0.32f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CaptureGuidanceCard()
                    PrivacyNote()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CaptureGuidanceCard(compact = true)
                CameraCaptureView(
                    onImageBytes = onImageCaptured,
                    captureButtonLabel = "Analyze meal",
                    modifier = Modifier.fillMaxWidth().testTag("meal_camera")
                )
                PrivacyNote()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CaptureGuidanceCard(compact: Boolean = false) {
    ModernCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.padding(9.dp).size(22.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Frame the whole plate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (compact) "Use even light and keep items visible."
                    else "Use even light, shoot from above, and keep sauces or sides visible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            }
        }
        if (!compact) {
            CaptureTip(Icons.Filled.CameraAlt, "One clear photo", "Avoid blur and strong shadows")
            CaptureTip(Icons.Filled.Restaurant, "Separate items", "Leave visible edges between foods")
            CaptureTip(Icons.Filled.Tune, "Review estimates", "You stay in control of every value")
        }
    }
}

@Composable
private fun CaptureTip(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrivacyNote() {
    Text(
        "Meal photos are sent only to your configured n8n analysis endpoint after privacy consent is enabled.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun AnalysisProgressContent(
    state: MealAnalysisUiState,
    modifier: Modifier = Modifier
) {
    AdaptiveAnalysisLayout(
        modifier = modifier,
        visual = {
            CapturedPhotoCard(state = state, badge = "ANALYZING")
        },
        details = {
            ModernCard(containerColor = MaterialTheme.colorScheme.surface) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(44.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Building your meal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("This usually takes a few seconds.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                AnalysisStep("Photo prepared", complete = true)
                AnalysisStep("Finding individual foods", active = true)
                AnalysisStep("Estimating portions and macros")
            }
            Text(
                "Keep this screen open while the analysis finishes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun AnalysisStep(label: String, complete: Boolean = false, active: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = when {
                complete -> MaterialTheme.colorScheme.primary
                active -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = when {
                complete -> MaterialTheme.colorScheme.onPrimary
                active -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        ) {
            if (complete) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(5.dp).size(15.dp))
            } else {
                Box(Modifier.padding(9.dp).size(7.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.primary))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (active || complete) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active || complete) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnalysisErrorContent(
    state: MealAnalysisUiState,
    onRetry: () -> Unit,
    onRetake: () -> Unit,
    onAddManually: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    AdaptiveAnalysisLayout(
        modifier = modifier,
        visual = { CapturedPhotoCard(state = state, badge = "PHOTO KEPT") },
        details = {
            InlineStatus(
                message = state.errorMessage ?: "This photo could not be analyzed.",
                kind = InlineStatusKind.Error
            )
            ModernCard {
                Text("What would you like to do?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                when (state.failure) {
                    MealAnalysisFailure.PrivacyDisabled -> {
                        Text(
                            "Turn on meal photo analysis, then return here and retry the same image.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onOpenPrivacySettings, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text("Open privacy settings")
                        }
                        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text("Try this photo again")
                        }
                    }
                    MealAnalysisFailure.Remote -> {
                        Text("Your image is ready, so you can retry without taking another photo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text("Retry analysis")
                        }
                    }
                    MealAnalysisFailure.NoFoodDetected,
                    MealAnalysisFailure.InvalidImage,
                    null -> Unit
                }
                OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                    Text("Take another photo", modifier = Modifier.padding(start = 8.dp))
                }
                TextButton(onClick = onAddManually, modifier = Modifier.fillMaxWidth()) {
                    Text("Add food manually")
                }
            }
        }
    )
}

@Composable
private fun AdaptiveAnalysisLayout(
    modifier: Modifier = Modifier,
    visual: @Composable ColumnScope.() -> Unit,
    details: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.52f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = visual
                )
                Column(
                    modifier = Modifier.weight(0.48f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = details
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                visual()
                details()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MealReviewContent(
    state: MealAnalysisUiState,
    onUpdateItem: (EditableMealItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    onAddItem: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ReviewPane(Modifier.weight(0.4f).fillMaxHeight()) {
                    ReviewSummary(state, onRetake)
                }
                ReviewPane(Modifier.weight(0.6f).fillMaxHeight()) {
                    DetectedItemsEditor(state, onUpdateItem, onRemoveItem, onAddItem)
                }
            }
        } else {
            ReviewPane(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                ReviewSummary(state, onRetake)
                DetectedItemsEditor(state, onUpdateItem, onRemoveItem, onAddItem)
            }
        }
    }
}

@Composable
private fun ReviewPane(
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
private fun ReviewSummary(
    state: MealAnalysisUiState,
    onRetake: () -> Unit
) {
    CapturedPhotoCard(state = state, badge = "${state.items.size} FOODS")
    ModernCard {
        Text("Meal totals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        MacroSummary(
            calories = state.totals.calories,
            protein = state.totals.protein,
            carbohydrates = state.totals.carbohydrates,
            fat = state.totals.fat
        )
    }
    state.disclaimer?.takeIf { it.isNotBlank() }?.let { disclaimer ->
        InlineStatus(disclaimer, InlineStatusKind.Info)
    }
    OutlinedButton(
        onClick = onRetake,
        enabled = state.phase != MealAnalysisPhase.Saving,
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
    ) {
        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
        Text("Retake photo", modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CapturedPhotoCard(state: MealAnalysisUiState, badge: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF071B31)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
            state.previewBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "Captured meal",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFF063B73), Color(0xFF087FC2)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(54.dp))
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.62f),
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.TopStart).padding(14.dp)
            ) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DetectedItemsEditor(
    state: MealAnalysisUiState,
    onUpdateItem: (EditableMealItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    onAddItem: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Detected foods", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Open an item to correct the estimate.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onAddItem, enabled = state.phase != MealAnalysisPhase.Saving) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(19.dp))
            Text("Add missing", modifier = Modifier.padding(start = 5.dp))
        }
    }
    state.errorMessage?.let { InlineStatus(it, InlineStatusKind.Error) }
    if (state.items.isEmpty()) {
        ModernCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("No foods selected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Add a missing food or retake the photo to continue.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add food", modifier = Modifier.padding(start = 8.dp))
            }
        }
    } else {
        state.items.forEachIndexed { index, item ->
            AnalysisItemCard(
                item = item,
                index = index,
                enabled = state.phase != MealAnalysisPhase.Saving,
                onUpdate = onUpdateItem,
                onRemove = { onRemoveItem(item.id) }
            )
        }
    }
}

@Composable
private fun AnalysisItemCard(
    item: EditableMealItem,
    index: Int,
    enabled: Boolean,
    onUpdate: (EditableMealItem) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(item.confidence <= 0.0) }
    val confidence = confidencePresentation(item.confidence)
    ModernCard(
        modifier = Modifier.fillMaxWidth().testTag("meal_item_${item.id}"),
        containerColor = if (item.isValid()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = !expanded }
                .semantics {
                    role = Role.Button
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.name.ifBlank { "Unnamed food" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${item.grams.ifBlank { "—" }} g · ${item.calories.ifBlank { "—" }} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ConfidenceChip(confidence, item.confidence)
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    label = { Text("Food name") },
                    isError = item.name.isBlank(),
                    supportingText = if (item.name.isBlank()) ({ Text("Required") }) else null,
                    enabled = enabled,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                ReviewFieldPair(
                    first = {
                        MealEditField(item.grams, { onUpdate(item.copy(grams = it)) }, "Portion", "g", true, enabled)
                    },
                    second = {
                        MealEditField(item.calories, { onUpdate(item.copy(calories = it)) }, "Energy", "kcal", false, enabled)
                    }
                )
                ReviewFieldPair(
                    first = {
                        MealEditField(item.protein, { onUpdate(item.copy(protein = it)) }, "Protein", "g", false, enabled)
                    },
                    second = {
                        MealEditField(item.carbs, { onUpdate(item.copy(carbs = it)) }, "Carbs", "g", false, enabled)
                    }
                )
                MealEditField(item.fat, { onUpdate(item.copy(fat = it)) }, "Fat", "g", false, enabled)
                OutlinedButton(onClick = onRemove, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                    Text("Remove from meal", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

private data class ConfidencePresentation(val label: String, val colorKind: ConfidenceKind)
private enum class ConfidenceKind { Good, Review, Low, Manual }

private fun confidencePresentation(confidence: Double): ConfidencePresentation = when {
    confidence <= 0.0 -> ConfidencePresentation("Manual", ConfidenceKind.Manual)
    confidence >= 0.8 -> ConfidencePresentation("High", ConfidenceKind.Good)
    confidence >= 0.55 -> ConfidencePresentation("Review", ConfidenceKind.Review)
    else -> ConfidencePresentation("Low", ConfidenceKind.Low)
}

@Composable
private fun ConfidenceChip(presentation: ConfidencePresentation, confidence: Double) {
    val container = when (presentation.colorKind) {
        ConfidenceKind.Good -> MaterialTheme.colorScheme.primaryContainer
        ConfidenceKind.Review -> MaterialTheme.colorScheme.tertiaryContainer
        ConfidenceKind.Low -> MaterialTheme.colorScheme.errorContainer
        ConfidenceKind.Manual -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when (presentation.colorKind) {
        ConfidenceKind.Good -> MaterialTheme.colorScheme.onPrimaryContainer
        ConfidenceKind.Review -> MaterialTheme.colorScheme.onTertiaryContainer
        ConfidenceKind.Low -> MaterialTheme.colorScheme.onErrorContainer
        ConfidenceKind.Manual -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = container,
        contentColor = content,
        modifier = Modifier.semantics {
            contentDescription = if (confidence > 0.0) {
                "${presentation.label} confidence, ${(confidence * 100).roundToInt()} percent"
            } else {
                "Manually added food"
            }
        }
    ) {
        Text(
            presentation.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ReviewFieldPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
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
private fun MealEditField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    suffix: String,
    positive: Boolean,
    enabled: Boolean
) {
    val parsed = value.toUserDecimalOrNull()
    val invalid = parsed == null || !parsed.isFinite() || if (positive) parsed <= 0.0 else parsed < 0.0
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        isError = invalid,
        supportingText = if (invalid) ({ Text(if (positive) "Above zero" else "Zero or more") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
