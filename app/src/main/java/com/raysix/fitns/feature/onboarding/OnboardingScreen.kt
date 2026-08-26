package com.raysix.fitns.feature.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.ActivityLevelSelector
import com.raysix.fitns.core.design.BiologicalSexDropdown
import com.raysix.fitns.core.design.ErrorBanner
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.FitnessGoalSelector
import com.raysix.fitns.core.input.toUserDecimalOrNull
import com.raysix.fitns.domain.model.UserProfile
import com.raysix.fitns.domain.usecase.NutritionGoalEstimator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.onboardingCompleted) {
        if (uiState.onboardingCompleted == true) {
            onDone()
        }
    }
    if (uiState.onboardingCompleted != false) {
        return
    }

    var age by rememberSaveable { mutableStateOf("") }
    var physiology by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var targetWeight by rememberSaveable { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf("") }
    var trainingDays by rememberSaveable { mutableStateOf("") }
    var goalName by rememberSaveable { mutableStateOf("") }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var profileSetupStarted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.profile, uiState.nutritionGoal) {
        if (initialized) return@LaunchedEffect
        age = uiState.profile.age?.toString().orEmpty()
        physiology = uiState.profile.sexOrPhysiology.orEmpty()
        height = uiState.profile.heightCm?.formatPlain().orEmpty()
        weight = uiState.profile.weightKg?.formatPlain().orEmpty()
        targetWeight = uiState.profile.targetWeightKg?.formatPlain().orEmpty()
        activity = uiState.profile.activityLevel
        trainingDays = uiState.profile.trainingDaysPerWeek.toString()
        goalName = uiState.profile.goal
        initialized = true
    }

    LaunchedEffect(uiState.googleAccount) {
        if (uiState.googleAccount != null) profileSetupStarted = true
    }

    val estimatedGoal = NutritionGoalEstimator.estimate(
        weightKg = weight.toUserDecimalOrNull(),
        goal = goalName.ifBlank { "Maintain" },
        activityLevel = activity.ifBlank { "Moderate" }
    )
    val canSaveProfile = weight.toUserDecimalOrNull()?.let { it in 20.0..500.0 } == true &&
        activity.isNotBlank() && goalName.isNotBlank()

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val success = viewModel.handleSignInResult(data)
        if (!success && data != null) {
            viewModel.setSignInError("Google sign-in failed. Please try again.")
        }
    }

    AdaptiveTwoColumn(
        header = {
            ScreenHeader(
                title = "Welcome to FitNS",
                subtitle = "Set up your profile so nutrition and workout targets match your goal."
            )
        },
        main = {
            if (!profileSetupStarted) {
                SectionCard(
                    title = "Continue with an account",
                    subtitle = "Sign in or create a local FitNS profile."
                ) {
                    val googleAccount = uiState.googleAccount
                    if (googleAccount == null) {
                        Surface(
                            onClick = {
                                val intent = viewModel.createSignInIntent()
                                if (intent != null) {
                                    signInLauncher.launch(intent)
                                } else {
                                    viewModel.setSignInError(
                                        "Google sign-in is not configured yet. Add GOOGLE_WEB_CLIENT_ID to local.properties and rebuild."
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Row(
                                Modifier.padding(vertical = 13.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = GoogleGLogo,
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Continue with Google",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        }
                    }
                    uiState.signInError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }
                }
                SectionCard(
                    title = "Create a FitNS profile",
                    subtitle = "No account required. Your profile is saved locally and queued for your configured n8n sync."
                ) {
                    OnboardingActionButton(
                        text = "Create profile",
                        onClick = { profileSetupStarted = true }
                    )
                }
            } else {
                SectionCard(title = "Your profile", subtitle = "These inputs calculate your first nutrition targets automatically.") {
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
            }
        },
        side = {
            if (!profileSetupStarted) {
                SectionCard(title = "Personalized from the start") {
                    Text("FitNS uses your weight, activity level, and goal to calculate your initial calorie, macro, fiber, and water targets.")
                    Text(
                        "You can fine-tune every nutrition target later in Settings.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                SectionCard(
                    title = "Calculated nutrition targets",
                    subtitle = "Updated automatically from your profile inputs."
                ) {
                    GoalSummaryRow("Calories", "${estimatedGoal.caloriesKcal.formatPlain()} kcal")
                    GoalSummaryRow("Protein", "${estimatedGoal.proteinGrams.formatPlain()} g")
                    GoalSummaryRow("Carbohydrates", "${estimatedGoal.carbohydrateGrams.formatPlain()} g")
                    GoalSummaryRow("Fat", "${estimatedGoal.fatGrams.formatPlain()} g")
                    GoalSummaryRow("Fiber", "${estimatedGoal.fiberGrams.formatPlain()} g")
                    GoalSummaryRow("Water", "${estimatedGoal.waterMilliliters.formatPlain()} ml")
                    Text(
                        "These are starting values. Change them anytime in Settings → Nutrition goals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                uiState.errorMessage?.let { ErrorBanner(it) }
                OnboardingActionButton(
                    text = if (uiState.saving) "Saving..." else "Create profile",
                    enabled = canSaveProfile && !uiState.saving,
                    onClick = {
                        viewModel.save(
                            profile = UserProfile(
                                age = age.toIntOrNull(),
                                sexOrPhysiology = physiology.ifBlank { null },
                                heightCm = height.toUserDecimalOrNull(),
                                weightKg = weight.toUserDecimalOrNull(),
                                targetWeightKg = targetWeight.toUserDecimalOrNull(),
                                activityLevel = activity.ifBlank { "Moderate" },
                                trainingDaysPerWeek = trainingDays.toIntOrNull() ?: 0,
                                goal = goalName.ifBlank { "Maintain" }
                            ),
                            goal = estimatedGoal,
                            onComplete = onDone
                        )
                    }
                )
                if (!canSaveProfile) {
                    Text(
                        "Enter a valid body weight and choose an activity level and goal to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@Composable
private fun GoalSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun OnboardingActionButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 14.dp)
        )
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

private val GoogleGLogo: ImageVector by lazy {
    ImageVector.Builder(
        name = "GoogleG",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF4285F4))) {
            moveTo(22.56f, 12.25f)
            curveToRelative(0f, -0.78f, -0.07f, -1.53f, -0.2f, -2.25f)
            horizontalLineTo(12f)
            verticalLineToRelative(4.51f)
            horizontalLineToRelative(5.92f)
            curveToRelative(-0.26f, 1.37f, -1.04f, 2.53f, -2.21f, 3.31f)
            verticalLineToRelative(2.77f)
            horizontalLineToRelative(3.57f)
            curveToRelative(2.08f, -1.92f, 3.28f, -4.74f, 3.28f, -8.34f)
            close()
        }
        path(fill = SolidColor(Color(0xFF34A853))) {
            moveTo(12f, 23f)
            curveToRelative(3.03f, 0f, 5.58f, -1f, 7.44f, -2.71f)
            lineToRelative(-3.57f, -2.77f)
            curveToRelative(-1f, 0.66f, -2.28f, 1.06f, -3.87f, 1.06f)
            curveToRelative(-2.97f, 0f, -5.5f, -2.01f, -6.4f, -4.71f)
            horizontalLineTo(1.95f)
            verticalLineToRelative(2.85f)
            curveTo(3.8f, 20.55f, 7.57f, 23f, 12f, 23f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFBBC05))) {
            moveTo(5.6f, 13.87f)
            curveToRelative(-0.2f, -0.6f, -0.31f, -1.24f, -0.31f, -1.87f)
            reflectiveCurveToRelative(0.11f, -1.27f, 0.31f, -1.87f)
            verticalLineTo(7.28f)
            horizontalLineTo(1.95f)
            curveTo(1.35f, 8.47f, 1f, 9.79f, 1f, 11.13f)
            curveToRelative(0f, 1.34f, 0.35f, 2.66f, 0.95f, 3.85f)
            lineToRelative(3.65f, -1.11f)
            close()
        }
        path(fill = SolidColor(Color(0xFFEA4335))) {
            moveTo(12f, 4.98f)
            curveToRelative(1.65f, 0f, 3.13f, 0.57f, 4.29f, 1.68f)
            lineToRelative(3.22f, -3.22f)
            curveTo(17.57f, 1.45f, 15.03f, 0f, 12f, 0f)
            curveTo(7.57f, 0f, 3.8f, 2.45f, 1.95f, 6.02f)
            lineToRelative(3.65f, 2.85f)
            curveToRelative(0.9f, -2.7f, 3.43f, -4.71f, 6.4f, -4.71f)
            close()
        }
    }.build()
}
