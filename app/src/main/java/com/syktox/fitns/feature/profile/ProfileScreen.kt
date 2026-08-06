package com.syktox.fitns.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.syktox.fitns.core.design.ScreenHeader
import com.syktox.fitns.core.design.SectionTitle
import com.syktox.fitns.domain.model.NutritionGoal
import com.syktox.fitns.domain.model.UserProfile

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onSave: (UserProfile, NutritionGoal) -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Profile",
            subtitle = "Set goals that drive nutrition targets and progress tracking."
        )
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Profile")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumericField(age, { age = it }, "Age", Modifier.weight(1f))
                    NumericField(trainingDays, { trainingDays = it }, "Training days", Modifier.weight(1f))
                }
                OutlinedTextField(physiology, { physiology = it }, label = { Text("Physiology") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumericField(height, { height = it }, "Height cm", Modifier.weight(1f))
                    NumericField(weight, { weight = it }, "Weight kg", Modifier.weight(1f))
                }
                NumericField(targetWeight, { targetWeight = it }, "Target weight kg")
                OutlinedTextField(activity, { activity = it }, label = { Text("Activity level") }, modifier = Modifier.fillMaxWidth())
                ChoiceChips(
                    options = listOf("Low", "Moderate", "High"),
                    selected = activity,
                    onSelected = { activity = it }
                )
                OutlinedTextField(goalName, { goalName = it }, label = { Text("Goal") }, modifier = Modifier.fillMaxWidth())
                ChoiceChips(
                    options = listOf("Maintain", "Lose Fat", "Build Muscle"),
                    selected = goalName,
                    onSelected = { goalName = it }
                )
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionTitle("Nutrition Goals")
                    Button(
                        onClick = {
                            val estimate = estimateNutritionGoal(
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
                        }
                    ) {
                        Text("Estimate")
                    }
                }
                Text(
                    text = "Estimates use body weight, activity, and goal. Adjust them after observing progress.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Button(
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Profile")
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
        modifier = modifier
    )
}

private fun Double.formatPlain(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else toString()
}

private fun estimateNutritionGoal(weightKg: Double?, goal: String, activityLevel: String): NutritionGoal {
    val bodyWeight = weightKg?.takeIf { it > 0.0 } ?: 75.0
    val activityMultiplier = when (activityLevel.lowercase()) {
        "low" -> 28.0
        "high" -> 36.0
        else -> 32.0
    }
    val goalAdjustment = when (goal.lowercase()) {
        "lose fat" -> -350.0
        "build muscle" -> 250.0
        else -> 0.0
    }
    val calories = (bodyWeight * activityMultiplier + goalAdjustment).coerceAtLeast(1400.0)
    val protein = bodyWeight * if (goal.equals("Build Muscle", ignoreCase = true)) 2.0 else 1.8
    val fat = bodyWeight * 0.8
    val carbs = ((calories - protein * 4.0 - fat * 9.0) / 4.0).coerceAtLeast(80.0)

    return NutritionGoal(
        caloriesKcal = calories.roundToNearest(25.0),
        proteinGrams = protein.roundToNearest(5.0),
        carbohydrateGrams = carbs.roundToNearest(5.0),
        fatGrams = fat.roundToNearest(5.0),
        fiberGrams = 30.0,
        waterMilliliters = (bodyWeight * 35.0).roundToNearest(250.0)
    )
}

private fun Double.roundToNearest(step: Double): Double {
    return kotlin.math.round(this / step) * step
}
