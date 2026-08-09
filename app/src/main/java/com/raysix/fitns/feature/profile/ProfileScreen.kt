package com.raysix.fitns.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.domain.model.NutritionGoal
import com.raysix.fitns.domain.model.UserProfile
import com.raysix.fitns.domain.usecase.NutritionGoalEstimator

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onSave: (UserProfile, NutritionGoal) -> Unit,
    onOpenWeight: () -> Unit = {},
    onOpenTips: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var age by remember { mutableStateOf("") }
    var physiology by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf("") }
    var trainingDays by remember { mutableStateOf("") }
    var goalName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var water by remember { mutableStateOf("") }

    LaunchedEffect(uiState.profile, uiState.nutritionGoal) {
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
    }

    AdaptiveTwoColumn(
        header = {
            ScreenHeader(
                title = "Profile",
                subtitle = "Set goals that drive nutrition targets and progress tracking."
            )
        },
        main = {
            SectionCard(title = "Profile") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(age, { age = it }, "Age", Modifier.weight(1f))
                        NumericField(trainingDays, { trainingDays = it }, "Training days", Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        physiology, { physiology = it },
                        label = { Text("Physiology") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(height, { height = it }, "Height cm", Modifier.weight(1f))
                        NumericField(weight, { weight = it }, "Weight kg", Modifier.weight(1f))
                    }
                    NumericField(targetWeight, { targetWeight = it }, "Target weight kg")
                    OutlinedTextField(
                        activity, { activity = it },
                        label = { Text("Activity level") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChoiceChips(
                        options = listOf("Low", "Moderate", "High"),
                        selected = activity,
                        onSelected = { activity = it }
                    )
                    OutlinedTextField(
                        goalName, { goalName = it },
                        label = { Text("Goal") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChoiceChips(
                        options = listOf("Maintain", "Lose Fat", "Build Muscle"),
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceChips(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected.equals(option, ignoreCase = true),
                onClick = { onSelected(option) },
                label = { Text(option) }
            )
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
