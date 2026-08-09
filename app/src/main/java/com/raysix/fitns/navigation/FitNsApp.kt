package com.raysix.fitns.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raysix.fitns.core.design.isCompactScreen
import com.raysix.fitns.core.design.isWideScreen
import com.raysix.fitns.feature.bodyweight.BodyWeightScreen
import com.raysix.fitns.feature.bodyweight.BodyWeightViewModel
import com.raysix.fitns.feature.dashboard.DashboardScreen
import com.raysix.fitns.feature.dashboard.DashboardViewModel
import com.raysix.fitns.feature.onboarding.OnboardingScreen
import com.raysix.fitns.feature.nutrition.ManualFoodScreen
import com.raysix.fitns.feature.nutrition.NutritionDayScreen
import com.raysix.fitns.feature.nutrition.NutritionViewModel
import com.raysix.fitns.feature.profile.ProfileScreen
import com.raysix.fitns.feature.profile.ProfileViewModel
import com.raysix.fitns.feature.progress.ProgressScreen
import com.raysix.fitns.feature.progress.ProgressViewModel
import com.raysix.fitns.feature.recommendations.RecommendationsScreen
import com.raysix.fitns.feature.recommendations.RecommendationsViewModel
import com.raysix.fitns.feature.scanner.BarcodeScannerScreen
import com.raysix.fitns.feature.scanner.LabelScanScreen
import com.raysix.fitns.feature.scanner.MealAnalysisScreen
import com.raysix.fitns.feature.settings.SettingsScreen
import com.raysix.fitns.feature.settings.SettingsViewModel
import com.raysix.fitns.feature.workout.WorkoutHistoryScreen
import com.raysix.fitns.feature.workout.WorkoutStartScreen
import com.raysix.fitns.feature.workout.WorkoutViewModel

private enum class Route(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Onboarding("onboarding", "Get Started", Icons.Outlined.Person, Icons.Filled.Person),
    Dashboard("dashboard", "Today", Icons.Outlined.Home, Icons.Filled.Home),
    Nutrition("nutrition", "Nutrition", Icons.Outlined.Restaurant, Icons.Filled.Restaurant),
    AddFood("add-food", "Add", Icons.Outlined.Restaurant, Icons.Filled.Restaurant),
    LabelScan("label-scan", "Label", Icons.Outlined.DocumentScanner, Icons.Outlined.DocumentScanner),
    BarcodeScan("barcode-scan", "Barcode", Icons.Outlined.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    MealAnalysis("meal-analysis", "Scan Meal", Icons.Outlined.CameraAlt, Icons.Filled.CameraAlt),
    Workout("workout", "Workout", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    BodyWeight("bodyweight", "Weight", Icons.Outlined.MonitorWeight, Icons.Outlined.MonitorWeight),
    Progress("progress", "Progress", Icons.AutoMirrored.Outlined.ShowChart, Icons.AutoMirrored.Filled.ShowChart),
    Recommendations("recommendations", "Tips", Icons.Outlined.Lightbulb, Icons.Outlined.Lightbulb),
    History("history", "History", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    Profile("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person),
    Settings("settings", "Settings", Icons.Outlined.Settings, Icons.Outlined.Settings)
}

@Composable
fun FitNsApp() {
    val navController = rememberNavController()
    val bottomRoutes = listOf(Route.Dashboard, Route.Nutrition, Route.Workout, Route.Progress, Route.Profile)
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val onOnboarding = currentDestination?.route == Route.Onboarding.value

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!onOnboarding && isCompactScreen()) {
                PillNavigationBar(
                    currentDestination = currentDestination,
                    bottomRoutes = bottomRoutes,
                    navController = navController
                )
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!onOnboarding && isWideScreen()) {
                SideRail(
                    currentDestination = currentDestination,
                    bottomRoutes = bottomRoutes,
                    navController = navController
                )
            }
            NavHost(
                navController = navController,
                startDestination = Route.Onboarding.value,
                modifier = Modifier.weight(1f)
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
                        onScanMeal = { navController.navigate(Route.MealAnalysis.value) },
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
                composable(Route.MealAnalysis.value) {
                    MealAnalysisScreen(onClose = { navController.popBackStack() })
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
}

@Composable
private fun PillNavigationBar(
    currentDestination: NavDestination?,
    bottomRoutes: List<Route>,
    navController: NavHostController
) {
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 12.dp
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                bottomRoutes.forEach { route ->
                    val selected = currentDestination?.hierarchy?.any { destination ->
                        destination.route == route.value
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTab(navController, route.value) },
                        label = { Text(route.label, maxLines = 1) },
                        icon = {
                            Icon(
                                imageVector = if (selected) route.selectedIcon else route.icon,
                                contentDescription = route.label
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SideRail(
    currentDestination: NavDestination?,
    bottomRoutes: List<Route>,
    navController: NavHostController
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp)
    ) {
        NavigationRail(
            containerColor = Color.Transparent,
            header = {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
        ) {
            bottomRoutes.forEach { route ->
                val selected = currentDestination?.hierarchy?.any { destination ->
                    destination.route == route.value
                } == true
                NavigationRailItem(
                    selected = selected,
                    onClick = { navigateToTab(navController, route.value) },
                    icon = {
                        Icon(
                            imageVector = if (selected) route.selectedIcon else route.icon,
                            contentDescription = route.label
                        )
                    },
                    label = { Text(route.label, maxLines = 1) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(Route.Dashboard.value) {
            saveState = true
        }
    }
}
