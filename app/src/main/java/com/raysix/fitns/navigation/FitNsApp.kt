package com.raysix.fitns.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.raysix.fitns.feature.bodyweight.BodyWeightScreen
import com.raysix.fitns.feature.bodyweight.BodyWeightViewModel
import com.raysix.fitns.feature.dashboard.DashboardScreen
import com.raysix.fitns.feature.dashboard.DashboardViewModel
import com.raysix.fitns.feature.onboarding.OnboardingScreen
import com.raysix.fitns.feature.nutrition.ManualFoodScreen
import com.raysix.fitns.feature.nutrition.NutritionDayScreen
import com.raysix.fitns.feature.nutrition.NutritionViewModel
import com.raysix.fitns.feature.scanner.BarcodeScannerScreen
import com.raysix.fitns.feature.scanner.LabelScanScreen
import com.raysix.fitns.feature.profile.ProfileScreen
import com.raysix.fitns.feature.profile.ProfileViewModel
import com.raysix.fitns.feature.progress.ProgressScreen
import com.raysix.fitns.feature.progress.ProgressViewModel
import com.raysix.fitns.feature.recommendations.RecommendationsScreen
import com.raysix.fitns.feature.recommendations.RecommendationsViewModel
import com.raysix.fitns.feature.settings.SettingsScreen
import com.raysix.fitns.feature.settings.SettingsViewModel
import com.raysix.fitns.feature.workout.WorkoutHistoryScreen
import com.raysix.fitns.feature.workout.WorkoutStartScreen
import com.raysix.fitns.feature.workout.WorkoutViewModel

private enum class Route(val value: String, val label: String, val icon: ImageVector) {
    Onboarding("onboarding", "Get Started", Icons.Outlined.Person),
    Dashboard("dashboard", "Today", Icons.Outlined.Home),
    Nutrition("nutrition", "Nutrition", Icons.Outlined.Restaurant),
    AddFood("add-food", "Add", Icons.Outlined.Restaurant),
    LabelScan("label-scan", "Label", Icons.Outlined.DocumentScanner),
    BarcodeScan("barcode-scan", "Barcode", Icons.Outlined.QrCodeScanner),
    Workout("workout", "Workout", Icons.Outlined.FitnessCenter),
    BodyWeight("bodyweight", "Weight", Icons.Outlined.MonitorWeight),
    Progress("progress", "Progress", Icons.AutoMirrored.Outlined.ShowChart),
    Recommendations("recommendations", "Tips", Icons.Outlined.Lightbulb),
    History("history", "History", Icons.Outlined.FitnessCenter),
    Profile("profile", "Profile", Icons.Outlined.Person),
    Settings("settings", "Settings", Icons.Outlined.Settings)
}

@Composable
fun FitNsApp() {
    val navController = rememberNavController()
    val bottomRoutes = listOf(Route.Dashboard, Route.Nutrition, Route.Workout, Route.Progress, Route.Profile)
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val onOnboarding = currentDestination?.route == Route.Onboarding.value

    Scaffold(
        bottomBar = {
            if (!onOnboarding) {
                NavigationBar {
                    bottomRoutes.forEach { route ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { destination ->
                                destination.route == route.value
                            } == true,
                            onClick = {
                                navController.navigate(route.value) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(Route.Dashboard.value) {
                                        saveState = true
                                    }
                                }
                            },
                            label = { Text(route.label) },
                            icon = {
                                Icon(
                                    imageVector = route.icon,
                                    contentDescription = route.label
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Onboarding.value,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Onboarding.value) {
                OnboardingScreen(
                    onDone = {
                        navController.navigate(Route.Dashboard.value) {
                            popUpTo(Route.Onboarding.value) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Route.Dashboard.value) {
                val viewModel: DashboardViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                DashboardScreen(
                    dashboard = uiState.dashboard,
                    workoutSummary = uiState.workoutSummary,
                    readiness = uiState.readiness,
                    coach = uiState.coach,
                    mealBreakdown = uiState.mealBreakdown,
                    micronutrients = uiState.micronutrients,
                    message = uiState.message,
                    onAddFood = { navController.navigate(Route.AddFood.value) },
                    onStartWorkout = { navController.navigate(Route.Workout.value) },
                    onAddWater = viewModel::addWater
                )
            }
            composable(Route.Nutrition.value) {
                val viewModel: NutritionViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                NutritionDayScreen(
                    dashboard = uiState.dashboard,
                    foodHistory = uiState.foodHistory,
                    foodFavorites = uiState.foodFavorites,
                    micronutrients = uiState.micronutrients,
                    errorMessage = uiState.errorMessage,
                    onAddFood = { navController.navigate(Route.AddFood.value) },
                    onDuplicateFood = viewModel::duplicateFood,
                    onDeleteFood = viewModel::deleteFood,
                    onUseFavorite = viewModel::useFavorite,
                    onSaveFavorite = viewModel::saveFavorite,
                    onDeleteFavorite = viewModel::deleteFavorite
                )
            }
            composable(Route.AddFood.value) {
                val viewModel: NutritionViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                ManualFoodScreen(
                    barcodeLookup = uiState.barcodeLookup,
                    onBarcodeChange = viewModel::updateBarcode,
                    onLookupBarcode = viewModel::lookupBarcode,
                    onPrefillConsumed = viewModel::clearBarcodePrefill,
                    onScanLabel = { navController.navigate(Route.LabelScan.value) },
                    onScanBarcode = { navController.navigate(Route.BarcodeScan.value) },
                    onSave = { input ->
                        viewModel.addFood(input) {
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Route.LabelScan.value) {
                val addFoodEntry = remember(navController) { navController.getBackStackEntry(Route.AddFood.value) }
                val nutritionViewModel: NutritionViewModel = hiltViewModel(addFoodEntry)
                LabelScanScreen(
                    onApply = { input ->
                        nutritionViewModel.applyLabelValues(input)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Route.BarcodeScan.value) {
                val addFoodEntry = remember(navController) { navController.getBackStackEntry(Route.AddFood.value) }
                val nutritionViewModel: NutritionViewModel = hiltViewModel(addFoodEntry)
                BarcodeScannerScreen(
                    onBarcodeDetected = { barcode ->
                        nutritionViewModel.onBarcodeScanned(barcode)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Route.Workout.value) {
                val viewModel: WorkoutViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                WorkoutStartScreen(
                    uiState = uiState,
                    onAddExercise = viewModel::addExercise,
                    onAddWorkout = viewModel::addWorkout,
                    onSavePlan = viewModel::saveWorkoutPlan,
                    onSaveTemplateAsPlan = viewModel::saveTemplateAsPlan,
                    onDeletePlan = viewModel::deleteWorkoutPlan,
                    onShowHistory = { navController.navigate(Route.History.value) }
                )
            }
            composable(Route.History.value) {
                val viewModel: WorkoutViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                WorkoutHistoryScreen(
                    history = uiState.history,
                    progressionHint = viewModel::progressionHint,
                    onDeleteWorkout = viewModel::deleteWorkout
                )
            }
            composable(Route.BodyWeight.value) {
                val viewModel: BodyWeightViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                BodyWeightScreen(
                    uiState = uiState,
                    onAddEntry = viewModel::addEntry,
                    onDeleteEntry = viewModel::deleteEntry
                )
            }
            composable(Route.Progress.value) {
                val viewModel: ProgressViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                ProgressScreen(uiState = uiState)
            }
            composable(Route.Recommendations.value) {
                val viewModel: RecommendationsViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                RecommendationsScreen(uiState = uiState)
            }
            composable(Route.Profile.value) {
                val viewModel: ProfileViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                ProfileScreen(
                    uiState = uiState,
                    onSave = viewModel::save,
                    onOpenWeight = { navController.navigate(Route.BodyWeight.value) },
                    onOpenTips = { navController.navigate(Route.Recommendations.value) },
                    onOpenSettings = { navController.navigate(Route.Settings.value) }
                )
            }
            composable(Route.Settings.value) {
                val viewModel: SettingsViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                SettingsScreen(
                    uiState = uiState,
                    onN8nBaseUrlChange = viewModel::updateN8nBaseUrl,
                    onSyncEnabledChange = viewModel::updateSyncEnabled,
                    onTemporaryPhotosOnlyChange = viewModel::updateTemporaryPhotosOnly,
                    onTestConnection = viewModel::testConnection,
                    onRetrySyncNow = viewModel::retrySyncNow,
                    onGenerateExport = viewModel::generateLocalJsonExport
                )
            }
        }
    }
}
