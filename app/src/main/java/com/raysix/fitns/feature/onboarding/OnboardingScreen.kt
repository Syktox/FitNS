package com.raysix.fitns.feature.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raysix.fitns.R
import com.raysix.fitns.core.design.ActivityLevelSelector
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.BiologicalSexDropdown
import com.raysix.fitns.core.design.BrandGradientViolet
import com.raysix.fitns.core.design.BrandGradient
import com.raysix.fitns.core.design.ErrorBanner
import com.raysix.fitns.core.design.FitnessGoalSelector
import com.raysix.fitns.core.design.GradientHeroCard
import com.raysix.fitns.core.design.OceanBackdrop
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.design.WhaleTailMark
import com.raysix.fitns.core.design.isCompactHeight
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
        if (uiState.onboardingCompleted == true) onDone()
    }
    if (uiState.onboardingCompleted != false) return

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
    val parsedAge = age.toIntOrNull()
    val parsedTrainingDays = trainingDays.toIntOrNull()
    val ageIsValid = age.isBlank() || parsedAge?.let { it in 1..120 } == true
    val trainingDaysAreValid = parsedTrainingDays?.let { it in 0..7 } == true
    val canSaveProfile = weight.toUserDecimalOrNull()?.let { it in 20.0..500.0 } == true &&
        ageIsValid && trainingDaysAreValid && activity.isNotBlank() && goalName.isNotBlank()

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val success = viewModel.handleSignInResult(data)
        if (!success && data != null) {
            viewModel.setSignInError("Google sign-in failed. Please try again.")
        }
    }
    val compactHeight = isCompactHeight()

    OceanBackdrop {
        AdaptiveTwoColumn(
            header = {
                OnboardingHero(
                    profileSetupStarted = profileSetupStarted,
                    compactHeight = compactHeight
                )
            },
            main = {
                if (!profileSetupStarted) {
                    SectionCard(
                        title = "Choose how to begin",
                        subtitle = "Use an account or keep your profile on this device."
                    ) {
                        if (uiState.googleAccount == null) {
                            Surface(
                                enabled = viewModel.isGoogleConfigured,
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
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                                        if (viewModel.isGoogleConfigured) {
                                            "Continue with Google"
                                        } else {
                                            "Google sign-in coming soon"
                                        },
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
                        Text(
                            "OR USE THIS DEVICE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OnboardingActionButton(
                            text = "Create a local profile",
                            onClick = { profileSetupStarted = true }
                        )
                        Text(
                            "Your local profile remains available offline and follows your configured sync settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    SectionCard(
                        title = "Your baseline",
                        subtitle = "These inputs calculate your first nutrition targets."
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                            ResponsiveFieldPair(
                                first = { modifier -> IntegerField(age, { age = it }, "Age", 3, modifier) },
                                second = { modifier -> IntegerField(trainingDays, { trainingDays = it }, "Training days", 1, modifier) }
                            )
                            BiologicalSexDropdown(
                                value = physiology,
                                onValueChange = { physiology = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            ResponsiveFieldPair(
                                first = { modifier -> NumericField(height, { height = it }, "Height cm", modifier) },
                                second = { modifier -> NumericField(weight, { weight = it }, "Weight kg", modifier) }
                            )
                            NumericField(targetWeight, { targetWeight = it }, "Target weight kg")
                            ActivityLevelSelector(selected = activity, onSelected = { activity = it })
                            FitnessGoalSelector(selected = goalName, onSelected = { goalName = it })
                        }
                    }
                    uiState.errorMessage?.let { ErrorBanner(it) }
                    OnboardingActionButton(
                        text = if (uiState.saving) "Saving…" else "Create profile",
                        enabled = canSaveProfile && !uiState.saving,
                        onClick = {
                            viewModel.save(
                                profile = UserProfile(
                                    age = parsedAge,
                                    sexOrPhysiology = physiology.ifBlank { null },
                                    heightCm = height.toUserDecimalOrNull(),
                                    weightKg = weight.toUserDecimalOrNull(),
                                    targetWeightKg = targetWeight.toUserDecimalOrNull(),
                                    activityLevel = activity.ifBlank { "Moderate" },
                                    trainingDaysPerWeek = parsedTrainingDays ?: 0,
                                    goal = goalName.ifBlank { "Maintain" }
                                ),
                                goal = estimatedGoal,
                                onComplete = onDone
                            )
                        }
                    )
                    if (!canSaveProfile) {
                        Text(
                            "Use a valid weight, age (if entered), and 0–7 training days, then choose your activity level and goal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            side = {
                if (!profileSetupStarted) {
                    SectionCard(
                        title = "A useful starting point",
                        subtitle = "Enough structure to guide you, with room to adjust."
                    ) {
                        BenefitRow(
                            number = "01",
                            title = "Targets with context",
                            detail = "Calories, macros, fiber, and water begin from your current profile."
                        )
                        BenefitRow(
                            number = "02",
                            title = "Built to adapt",
                            detail = "Change every target later as your routine or priorities evolve."
                        )
                        BenefitRow(
                            number = "03",
                            title = "One calm overview",
                            detail = "Food, hydration, and training stay connected on your dashboard."
                        )
                    }
                    OnboardingMotivationCard()
                } else {
                    SectionCard(
                        title = "Calculated targets",
                        subtitle = "Updated live from your profile inputs."
                    ) {
                        GoalSummaryRow("Calories", "${estimatedGoal.caloriesKcal.formatPlain()} kcal")
                        GoalSummaryRow("Protein", "${estimatedGoal.proteinGrams.formatPlain()} g")
                        GoalSummaryRow("Carbs", "${estimatedGoal.carbohydrateGrams.formatPlain()} g")
                        GoalSummaryRow("Fat", "${estimatedGoal.fatGrams.formatPlain()} g")
                        GoalSummaryRow("Fiber", "${estimatedGoal.fiberGrams.formatPlain()} g")
                        GoalSummaryRow("Water", "${estimatedGoal.waterMilliliters.formatPlain()} ml")
                        Text(
                            "These are starting values. Change them anytime in Settings → Nutrition goals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun OnboardingMotivationCard() {
    val onHero = MaterialTheme.colorScheme.onPrimary
    GradientHeroCard(brush = BrandGradient, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "YOUR STRENGTH RUNS DEEP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = onHero.copy(alpha = 0.78f)
                )
                Text(
                    "Small choices. Powerful current.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = onHero
                )
                Text(
                    "FitNS turns today's effort into a clear next step tomorrow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onHero.copy(alpha = 0.86f)
                )
            }
            WhaleTailMark(
                modifier = Modifier.size(58.dp),
                tint = onHero.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun OnboardingHero(profileSetupStarted: Boolean, compactHeight: Boolean) {
    val onHero = MaterialTheme.colorScheme.onPrimary
    GradientHeroCard(brush = BrandGradientViolet, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compactHeight) 4.dp else 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WhaleTailMark(Modifier.size(25.dp), tint = onHero.copy(alpha = 0.9f))
                    Text(
                        if (profileSetupStarted) "PROFILE SETUP" else "BLUE WHALE SYSTEM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = onHero.copy(alpha = 0.82f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    if (profileSetupStarted) "Set your course" else "Build your baseline",
                    style = if (compactHeight) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = onHero
                )
                Text(
                    if (profileSetupStarted) {
                        "A few details turn broad guidance into targets that fit."
                    } else {
                        "Start with a clear plan for nutrition, hydration, and training."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onHero.copy(alpha = 0.9f)
                )
            }
            Image(
                painter = painterResource(R.drawable.whale_coach),
                contentDescription = null,
                modifier = Modifier.padding(start = 10.dp).size(if (compactHeight) 72.dp else 94.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun BenefitRow(number: String, title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                number,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ResponsiveFieldPair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 310.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GoalSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)
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
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun IntegerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxDigits: Int,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { candidate ->
            if (candidate.length <= maxDigits && candidate.all(Char::isDigit)) {
                onValueChange(candidate)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

private fun Double.formatPlain(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

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
