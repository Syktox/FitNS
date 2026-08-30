package com.raysix.fitns.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.ActivityLevelSelector
import com.raysix.fitns.core.design.BiologicalSexDropdown
import com.raysix.fitns.core.design.FitnessGoalSelector
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.input.toUserDecimalOrNull
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
                title = "Your course",
                subtitle = "Set the body, routine, and fuel targets that guide FitNS.",
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
            SectionCard(title = "Body & routine", subtitle = "The baseline behind your estimates.") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdaptiveFieldPair(
                        first = {
                            NumericField(age, { age = it }, "Age")
                        },
                        second = {
                            NumericField(trainingDays, { trainingDays = it }, "Training days")
                        }
                    )
                    BiologicalSexDropdown(
                        value = physiology,
                        onValueChange = { physiology = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AdaptiveFieldPair(
                        first = {
                            NumericField(height, { height = it }, "Height cm")
                        },
                        second = {
                            NumericField(weight, { weight = it }, "Weight kg")
                        }
                    )
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
                title = "Fuel targets",
                subtitle = "Estimate a starting point, then steer with your real-world trend.",
                trailing = {
                    Surface(
                        onClick = {
                            val estimate = NutritionGoalEstimator.estimate(
                                weightKg = weight.toUserDecimalOrNull(),
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
                            "Recalculate",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumericField(calories, { calories = it }, "Calories kcal")
                    AdaptiveFieldPair(
                        first = {
                            NumericField(protein, { protein = it }, "Protein g")
                        },
                        second = {
                            NumericField(carbs, { carbs = it }, "Carbs g")
                        }
                    )
                    AdaptiveFieldPair(
                        first = {
                            NumericField(fat, { fat = it }, "Fat g")
                        },
                        second = {
                            NumericField(fiber, { fiber = it }, "Fiber g")
                        }
                    )
                    NumericField(water, { water = it }, "Water ml")
                }
            }
            uiState.statusMessage?.let {
                ProfileStatus(it, error = false)
            }
            uiState.errorMessage?.let {
                ProfileStatus(it, error = true)
            }
            Surface(
                onClick = {
                    onSave(
                        UserProfile(
                            age = age.toIntOrNull(),
                            sexOrPhysiology = physiology.ifBlank { null },
                            heightCm = height.toUserDecimalOrNull(),
                            weightKg = weight.toUserDecimalOrNull(),
                            targetWeightKg = targetWeight.toUserDecimalOrNull(),
                            activityLevel = activity.ifBlank { "Moderate" },
                            trainingDaysPerWeek = trainingDays.toIntOrNull() ?: 0,
                            goal = goalName.ifBlank { "Maintain" }
                        ),
                        NutritionGoal(
                            caloriesKcal = calories.toUserDecimalOrNull() ?: 0.0,
                            proteinGrams = protein.toUserDecimalOrNull() ?: 0.0,
                            carbohydrateGrams = carbs.toUserDecimalOrNull() ?: 0.0,
                            fatGrams = fat.toUserDecimalOrNull() ?: 0.0,
                            fiberGrams = fiber.toUserDecimalOrNull() ?: 0.0,
                            waterMilliliters = water.toUserDecimalOrNull() ?: 0.0
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    "Save my course",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            }
        },
        side = {
            ProfileCourseCard(
                goal = goalName.ifBlank { uiState.profile.goal.ifBlank { "Build your baseline" } },
                trainingDays = trainingDays.toIntOrNull() ?: uiState.profile.trainingDaysPerWeek,
                targetWeight = targetWeight.toUserDecimalOrNull() ?: uiState.profile.targetWeightKg
            )
            SectionCard(title = "Quick dive", subtitle = "Jump straight to the signal you need.") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickLink(Icons.Outlined.MonitorWeight, "Weight tracking", onOpenWeight)
                    QuickLink(Icons.Outlined.Lightbulb, "Coaching current", onOpenTips)
                    QuickLink(Icons.Outlined.Settings, "Settings", onOpenSettings)
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
        caloriesKcal = calories.toUserDecimalOrNull() ?: 0.0,
        proteinGrams = protein.toUserDecimalOrNull() ?: 0.0,
        carbohydrateGrams = carbs.toUserDecimalOrNull() ?: 0.0,
        fatGrams = fat.toUserDecimalOrNull() ?: 0.0,
        fiberGrams = fiber.toUserDecimalOrNull() ?: 0.0,
        waterMilliliters = water.toUserDecimalOrNull() ?: 0.0
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
                title = "Fuel course",
                subtitle = "Fine-tune energy, macros, fiber, and hydration.",
                actions = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        main = {
            SectionCard(title = "Energy & macros", subtitle = "Daily targets for the work ahead.") {
                NumericField(calories, { calories = it }, "Calories kcal")
                AdaptiveFieldPair(
                    first = {
                        NumericField(protein, { protein = it }, "Protein g")
                    },
                    second = {
                        NumericField(carbs, { carbs = it }, "Carbs g")
                    }
                )
                NumericField(fat, { fat = it }, "Fat g")
            }
        },
        side = {
            NutritionCourseCard(editedGoal)
            SectionCard(title = "Fiber & hydration", subtitle = "Support recovery and a steady daily rhythm.") {
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
                    "Save fuel course",
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
private fun QuickLink(icon: ImageVector, text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ProfileCourseCard(goal: String, trainingDays: Int, targetWeight: Double?) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Explore, contentDescription = null, modifier = Modifier.size(26.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Current heading", style = MaterialTheme.typography.labelMedium)
                    Text(goal, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CourseMetric("Training", if (trainingDays > 0) "$trainingDays days/wk" else "Not set", Modifier.weight(1f))
                CourseMetric("Target", targetWeight?.let { "${it.formatPlain()} kg" } ?: "Not set", Modifier.weight(1f))
            }
            Text(
                "Like a whale holding a long course, favor repeatable habits over sharp turns.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun CourseMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NutritionCourseCard(goal: NutritionGoal) {
    SectionCard(title = "Daily current", subtitle = "Your targets at a glance.", accent = true) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CourseMetric("Energy", "${goal.caloriesKcal.toInt()} kcal", Modifier.weight(1f))
            CourseMetric("Protein", "${goal.proteinGrams.toInt()} g", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(
                "${goal.waterMilliliters.toInt()} ml hydration target",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProfileStatus(message: String, error: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(message, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp))
    }
}

@Composable
private fun AdaptiveFieldPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 380.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                first()
                second()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { first() }
                Box(Modifier.weight(1f)) { second() }
            }
        }
    }
}

@Composable
private fun NumericField(
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
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    )
}

private fun Double.formatPlain(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else toString()
}
