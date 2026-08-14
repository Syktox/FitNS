package com.raysix.fitns.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.ActivityLevelSelector
import com.raysix.fitns.core.design.BiologicalSexDropdown
import com.raysix.fitns.core.design.FitnessGoalSelector
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.domain.model.NutritionGoal
import com.raysix.fitns.domain.model.UserProfile
import com.raysix.fitns.domain.usecase.NutritionGoalEstimator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onSave: (UserProfile, NutritionGoal) -> Unit,
    onOpenWeight: () -> Unit = {},
    onOpenTips: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var age by rememberSaveable { mutableStateOf("") }
    var physiology by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var targetWeight by rememberSaveable { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf("") }
    var trainingDays by rememberSaveable { mutableStateOf("") }
    var goalName by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var fiber by rememberSaveable { mutableStateOf("") }
    var water by rememberSaveable { mutableStateOf("") }
    var initialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.profile, uiState.nutritionGoal) {
        if (initialized || !uiState.isLoaded) return@LaunchedEffect
        age = uiState.profile.age?.toString().orEmpty()
        physiology = uiState.profile.sexOrPhysiology.orEmpty()
        height = uiState.profile.heightCm?.formatPlain().orEmpty()
        weight = uiState.profile.weightKg?.formatPlain().orEmpty()
        targetWeight = uiState.profile.targetWeightKg?.formatPlain().orEmpty()
        activity = uiState.profile.activityLevel
        trainingDays = uiState.profile.trainingDaysPerWeek.toString()
        goalName = uiState.profile.goal
        calories = uiState.nutritionGoal.caloriesKcal.formatPlain()
        protein = uiState.nutritionGoal.proteinGrams.formatPlain()
        carbs = uiState.nutritionGoal.carbohydrateGrams.formatPlain()
        fat = uiState.nutritionGoal.fatGrams.formatPlain()
        fiber = uiState.nutritionGoal.fiberGrams.formatPlain()
        water = uiState.nutritionGoal.waterMilliliters.formatPlain()
        initialized = true
    }

    AdaptiveTwoColumn(
        header = {
            ScreenHeader(
                title = "Profile",
                subtitle = "Set goals that drive nutrition targets and progress tracking.",
                actions = {
                    TextButton(onClick = onBack) { Text("Back") }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        main = {
            SectionCard(title = "Profile") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(age, { age = it }, "Age", Modifier.weight(1f))
                        NumericField(trainingDays, { trainingDays = it }, "Training days", Modifier.weight(1f))
                    }
                    BiologicalSexDropdown(
                        value = physiology,
                        onValueChange = { physiology = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(height, { height = it }, "Height cm", Modifier.weight(1f))
                        NumericField(weight, { weight = it }, "Weight kg", Modifier.weight(1f))
                    }
                    NumericField(targetWeight, { targetWeight = it }, "Target weight kg")
                    ActivityLevelSelector(
                        selected = activity,
                        onSelected = { activity = it }
                    )
                    FitnessGoalSelector(
                        selected = goalName,
                        onSelected = { goalName = it }
                    )
                }
            }
            SectionCard(
                title = "Nutrition Goals",
                subtitle = "Estimates use body weight, activity, and goal. Adjust them after observing progress.",
                trailing = {
                    Surface(
                        onClick = {
                            val estimate = NutritionGoalEstimator.estimate(
                                weightKg = weight.toDoubleOrNull(),
                                goal = goalName.ifBlank { "Maintain" },
                                activityLevel = activity.ifBlank { "Moderate" }
                            )
                            calories = estimate.caloriesKcal.formatPlain()
                            protein = estimate.proteinGrams.formatPlain()
                            carbs = estimate.carbohydrateGrams.formatPlain()
                            fat = estimate.fatGrams.formatPlain()
                            fiber = estimate.fiberGrams.formatPlain()
                            water = estimate.waterMilliliters.formatPlain()
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            "Estimate",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumericField(calories, { calories = it }, "Calories kcal")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(protein, { protein = it }, "Protein g", Modifier.weight(1f))
                        NumericField(carbs, { carbs = it }, "Carbs g", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(fat, { fat = it }, "Fat g", Modifier.weight(1f))
                        NumericField(fiber, { fiber = it }, "Fiber g", Modifier.weight(1f))
                    }
                    NumericField(water, { water = it }, "Water ml")
                }
            }
            uiState.statusMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
            Surface(
                onClick = {
                    onSave(
                        UserProfile(
                            age = age.toIntOrNull(),
                            sexOrPhysiology = physiology.ifBlank { null },
                            heightCm = height.toDoubleOrNull(),
                            weightKg = weight.toDoubleOrNull(),
                            targetWeightKg = targetWeight.toDoubleOrNull(),
                            activityLevel = activity.ifBlank { "Moderate" },
                            trainingDaysPerWeek = trainingDays.toIntOrNull() ?: 0,
                            goal = goalName.ifBlank { "Maintain" }
                        ),
                        NutritionGoal(
                            caloriesKcal = calories.toDoubleOrNull() ?: 0.0,
                            proteinGrams = protein.toDoubleOrNull() ?: 0.0,
                            carbohydrateGrams = carbs.toDoubleOrNull() ?: 0.0,
                            fatGrams = fat.toDoubleOrNull() ?: 0.0,
                            fiberGrams = fiber.toDoubleOrNull() ?: 0.0,
                            waterMilliliters = water.toDoubleOrNull() ?: 0.0
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    "Save Profile",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            }
        },
        side = {
            SectionCard(title = "Quick Access") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickLink(text = "Weight Tracking", onClick = onOpenWeight)
                    QuickLink(text = "Coaching Tips", onClick = onOpenTips)
                    QuickLink(text = "Settings", onClick = onOpenSettings)
                }
            }
        }
    )
}

@Composable
fun NutritionGoalsSettingsScreen(
    uiState: ProfileUiState,
    onSave: (NutritionGoal) -> Unit,
    onBack: () -> Unit
) {
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var fiber by rememberSaveable { mutableStateOf("") }
    var water by rememberSaveable { mutableStateOf("") }
    var initialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.nutritionGoal, uiState.isLoaded) {
        if (initialized || !uiState.isLoaded) return@LaunchedEffect
        calories = uiState.nutritionGoal.caloriesKcal.formatPlain()
        protein = uiState.nutritionGoal.proteinGrams.formatPlain()
        carbs = uiState.nutritionGoal.carbohydrateGrams.formatPlain()
        fat = uiState.nutritionGoal.fatGrams.formatPlain()
        fiber = uiState.nutritionGoal.fiberGrams.formatPlain()
        water = uiState.nutritionGoal.waterMilliliters.formatPlain()
        initialized = true
    }

    val editedGoal = NutritionGoal(
        caloriesKcal = calories.toDoubleOrNull() ?: 0.0,
        proteinGrams = protein.toDoubleOrNull() ?: 0.0,
        carbohydrateGrams = carbs.toDoubleOrNull() ?: 0.0,
        fatGrams = fat.toDoubleOrNull() ?: 0.0,
        fiberGrams = fiber.toDoubleOrNull() ?: 0.0,
        waterMilliliters = water.toDoubleOrNull() ?: 0.0
    )
    val canSave = editedGoal.caloriesKcal in 800.0..8000.0 &&
        editedGoal.proteinGrams >= 0.0 &&
        editedGoal.carbohydrateGrams >= 0.0 &&
        editedGoal.fatGrams >= 0.0 &&
        editedGoal.fiberGrams >= 0.0 &&
        editedGoal.waterMilliliters in 0.0..10000.0

    AdaptiveTwoColumn(
        header = {
            ScreenHeader(
                title = "Nutrition Goals",
                subtitle = "Fine-tune the targets calculated during onboarding.",
                actions = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        main = {
            SectionCard(title = "Energy and macros") {
                NumericField(calories, { calories = it }, "Calories kcal")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumericField(protein, { protein = it }, "Protein g", Modifier.weight(1f))
                    NumericField(carbs, { carbs = it }, "Carbs g", Modifier.weight(1f))
                }
                NumericField(fat, { fat = it }, "Fat g")
            }
        },
        side = {
            SectionCard(title = "Fiber and hydration") {
                NumericField(fiber, { fiber = it }, "Fiber g")
                NumericField(water, { water = it }, "Water ml")
            }
            uiState.statusMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
            Surface(
                onClick = { onSave(editedGoal) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    "Save Nutrition Goals",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            }
            if (!canSave && initialized) {
                Text(
                    "Check the values before saving. Calories must be between 800 and 8000 kcal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
private fun QuickLink(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    )
}

private fun Double.formatPlain(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else toString()
}
