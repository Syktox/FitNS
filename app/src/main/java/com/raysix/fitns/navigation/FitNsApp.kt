package com.raysix.fitns.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Apps
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ListItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.raysix.fitns.feature.settings.AccountSettingsScreen
import com.raysix.fitns.feature.settings.N8nSettingsScreen
import com.raysix.fitns.feature.settings.NavigationSettingsScreen
import com.raysix.fitns.feature.settings.PrivacySettingsScreen
import com.raysix.fitns.feature.settings.AppearanceSettingsScreen
import com.raysix.fitns.feature.workout.ActiveWorkoutScreen
import com.raysix.fitns.feature.workout.WorkoutHistoryScreen
import com.raysix.fitns.feature.workout.WorkoutStartScreen
import com.raysix.fitns.feature.workout.WorkoutViewModel
import com.raysix.fitns.domain.repository.BottomNavigationDestination

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
    ActiveWorkout("active-workout", "Active", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    BodyWeight("bodyweight", "Weight", Icons.Outlined.MonitorWeight, Icons.Outlined.MonitorWeight),
    Progress("progress", "Progress", Icons.AutoMirrored.Outlined.ShowChart, Icons.AutoMirrored.Filled.ShowChart),
    Recommendations("recommendations", "Coach", Icons.Outlined.Lightbulb, Icons.Outlined.Lightbulb),
    History("history", "History", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    Profile("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person),
    Settings("settings", "Settings", Icons.Outlined.Settings, Icons.Outlined.Settings),
    Account("settings/account", "Account", Icons.Outlined.Person, Icons.Filled.Person),
    N8nSettings("settings/n8n", "n8n & Sync", Icons.Outlined.Settings, Icons.Outlined.Settings),
    PrivacySettings("settings/privacy", "Privacy & Data", Icons.Outlined.Settings, Icons.Outlined.Settings),
    AppearanceSettings("settings/appearance", "Appearance", Icons.Outlined.Settings, Icons.Outlined.Settings),
    NavigationSettings("settings/navigation", "Bottom navigation", Icons.Outlined.Settings, Icons.Outlined.Settings),
    QuickAccess("quick-access", "Quick", Icons.Outlined.Apps, Icons.Filled.Apps)
}

@Composable
fun FitNsApp() {
    val navController = rememberNavController()
    val primaryNavigationViewModel: PrimaryNavigationViewModel = hiltViewModel()
    val selectedDestinations = primaryNavigationViewModel.destinations.collectAsStateWithLifecycle().value
    val bottomRoutes = remember(selectedDestinations) { selectedDestinations.map { it.toRoute() } }
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val showPrimaryNavigation = currentDestination?.route == Route.Dashboard.value ||
        currentDestination?.route in bottomRoutes.map { it.value }
    var showQuickActions by rememberSaveable { mutableStateOf(false) }

    if (showQuickActions) {
        QuickActionsSheet(
            onDismiss = { showQuickActions = false },
            onNavigate = { route ->
                showQuickActions = false
                navigateFromQuickAccess(navController, route)
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showPrimaryNavigation && isCompactScreen()) {
                PillNavigationBar(
                    currentDestination = currentDestination,
                    bottomRoutes = bottomRoutes,
                    navController = navController,
                    onQuickAction = { showQuickActions = true }
                )
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showPrimaryNavigation && isWideScreen()) {
                    SideRail(
                    currentDestination = currentDestination,
                    bottomRoutes = bottomRoutes,
                    navController = navController,
                    onQuickAction = { showQuickActions = true }
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
                        onAddWater = viewModel::addWater,
                        onRemoveWater = viewModel::removeWater,
                        onOpenSettings = { navController.navigate(Route.Settings.value) }
                    )
                }
                composable(Route.Nutrition.value) {
                    val viewModel: NutritionViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    NutritionDayScreen(
                        dashboard = uiState.dashboard,
                        foodHistory = uiState.foodHistory,
                        foodFavorites = uiState.foodFavorites,
                        foodSearch = uiState.foodSearch,
                        savedMeals = uiState.savedMeals,
                        micronutrients = uiState.micronutrients,
                        errorMessage = uiState.errorMessage,
                        confirmationMessage = uiState.confirmationMessage,
                        onAddFood = { navController.navigate(Route.AddFood.value) },
                        onFoodSearchQueryChange = viewModel::updateFoodSearchQuery,
                        onDuplicateFood = viewModel::duplicateFood,
                        onDeleteFood = viewModel::deleteFood,
                        onUseFavorite = viewModel::useFavorite,
                        onSaveFavorite = viewModel::saveFavorite,
                        onDeleteFavorite = viewModel::deleteFavorite,
                        onUseCustomFood = viewModel::useCustomFood,
                        onSaveCustomFood = viewModel::saveCustomFood,
                        onDeleteCustomFood = viewModel::deleteCustomFood,
                        onSaveTodayAsMeal = viewModel::saveTodayAsMeal,
                        onLogSavedMeal = viewModel::logSavedMeal,
                        onDeleteSavedMeal = viewModel::deleteSavedMeal,
                        onCopyYesterday = viewModel::copyYesterday,
                        onCopyPreviousMeal = viewModel::copyPreviousMeal
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
                composable(Route.LabelScan.value) { backStackEntry ->
                    val addFoodEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.AddFood.value) }
                    val nutritionViewModel: NutritionViewModel = hiltViewModel(addFoodEntry)
                    LabelScanScreen(
                        onApply = { input ->
                            nutritionViewModel.applyLabelValues(input)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                composable(Route.BarcodeScan.value) { backStackEntry ->
                    val addFoodEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.AddFood.value) }
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
                        onUpdatePlan = viewModel::updateWorkoutPlan,
                        onSaveTemplateAsPlan = viewModel::saveTemplateAsPlan,
                        onStartPlan = { plan ->
                            viewModel.startWorkoutPlan(plan)
                            navController.navigate(Route.ActiveWorkout.value)
                        },
                        onStartTemplate = { template ->
                            viewModel.startWorkoutTemplate(template)
                            navController.navigate(Route.ActiveWorkout.value)
                        },
                        onDeletePlan = viewModel::deleteWorkoutPlan,
                        onShowHistory = { navController.navigate(Route.History.value) }
                    )
                }
                composable(Route.ActiveWorkout.value) { backStackEntry ->
                    val workoutEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.Workout.value) }
                    val viewModel: WorkoutViewModel = hiltViewModel(workoutEntry)
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    ActiveWorkoutScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onAddExercise = viewModel::addExerciseToActiveSession,
                        onRemoveExercise = viewModel::removeExerciseFromActiveSession,
                        onMoveExercise = viewModel::moveExerciseInActiveSession,
                        onAddSet = viewModel::addSetToActiveExercise,
                        onDeleteSet = viewModel::deleteSetFromActiveExercise,
                        onUpdateSet = viewModel::updateActiveSet,
                        onToggleSetComplete = viewModel::toggleSetCompleted,
                        onFinish = {
                            viewModel.finishActiveWorkout {
                                navController.popBackStack()
                            }
                        },
                        onDiscard = {
                            viewModel.discardActiveWorkout()
                            navController.popBackStack()
                        },
                        onAddRestTime = viewModel::adjustRestTimer,
                        onPauseTimer = viewModel::pauseRestTimer,
                        onResumeTimer = viewModel::resumeRestTimer,
                        onSkipTimer = viewModel::skipRestTimer
                    )
                }
                composable(Route.History.value) {
                    val viewModel: WorkoutViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    WorkoutHistoryScreen(
                        history = uiState.history,
                        progressionHint = viewModel::progressionHint,
                        onDeleteWorkout = viewModel::deleteWorkout,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Route.BodyWeight.value) {
                    val viewModel: BodyWeightViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    BodyWeightScreen(
                        uiState = uiState,
                        onAddEntry = viewModel::addEntry,
                        onDeleteEntry = viewModel::deleteEntry,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Route.Progress.value) {
                    val viewModel: ProgressViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    ProgressScreen(uiState = uiState, onBack = { navController.popBackStack() })
                }
                composable(Route.Recommendations.value) {
                    val viewModel: RecommendationsViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    RecommendationsScreen(uiState = uiState, onBack = { navController.popBackStack() })
                }
                composable(Route.Profile.value) {
                    val viewModel: ProfileViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    ProfileScreen(
                        uiState = uiState,
                        onSave = viewModel::save,
                        onOpenWeight = { navController.navigate(Route.BodyWeight.value) },
                        onOpenTips = { navController.navigate(Route.Recommendations.value) },
                        onOpenSettings = { navController.navigate(Route.Settings.value) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Route.Settings.value) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    SettingsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onOpenAccount = { navController.navigate(Route.Account.value) },
                        onOpenN8n = { navController.navigate(Route.N8nSettings.value) },
                        onOpenPrivacy = { navController.navigate(Route.PrivacySettings.value) },
                        onOpenAppearance = { navController.navigate(Route.AppearanceSettings.value) },
                        onOpenNavigation = { navController.navigate(Route.NavigationSettings.value) },
                        onOpenProfile = { navController.navigate(Route.Profile.value) },
                        onOpenProgress = { navController.navigate(Route.Progress.value) },
                        onOpenCoaching = { navController.navigate(Route.Recommendations.value) }
                    )
                }
                composable(Route.Account.value) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    AccountSettingsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onCreateSignInIntent = viewModel::createSignInIntent,
                        onSignInResult = viewModel::handleSignInResult,
                        onSignOut = viewModel::signOut
                    )
                }
                composable(Route.N8nSettings.value) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    N8nSettingsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onBaseUrlChange = viewModel::updateN8nBaseUrl,
                        onSaveBaseUrl = viewModel::saveN8nBaseUrl,
                        onBearerTokenChange = viewModel::updateBearerToken,
                        onSaveBearerToken = viewModel::saveBearerToken,
                        onSyncEnabledChange = viewModel::updateSyncEnabled,
                        onTestConnection = viewModel::testConnection,
                        onSyncNow = viewModel::retrySyncNow
                    )
                }
                composable(Route.PrivacySettings.value) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    PrivacySettingsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onTemporaryPhotosOnlyChange = viewModel::updateTemporaryPhotosOnly,
                        onGenerateExport = viewModel::generateLocalJsonExport
                    )
                }
                composable(Route.AppearanceSettings.value) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    AppearanceSettingsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onModeChange = viewModel::updateAppearanceMode
                    )
                }
                composable(Route.NavigationSettings.value) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                    NavigationSettingsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onSelectionChange = viewModel::updateBottomNavigation,
                        onReset = viewModel::resetBottomNavigation
                    )
                }
            }
        }
    }
}

private fun BottomNavigationDestination.toRoute(): Route = when (this) {
    BottomNavigationDestination.Today -> Route.Dashboard
    BottomNavigationDestination.Nutrition -> Route.Nutrition
    BottomNavigationDestination.Workout -> Route.Workout
    BottomNavigationDestination.Progress -> Route.Progress
    BottomNavigationDestination.BodyWeight -> Route.BodyWeight
    BottomNavigationDestination.Coaching -> Route.Recommendations
    BottomNavigationDestination.Profile -> Route.Profile
    BottomNavigationDestination.Settings -> Route.Settings
    BottomNavigationDestination.QuickAccess -> Route.QuickAccess
}

@Composable
private fun PillNavigationBar(
    currentDestination: NavDestination?,
    bottomRoutes: List<Route>,
    navController: NavHostController,
    onQuickAction: () -> Unit
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
                    } == true && route != Route.QuickAccess
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (route == Route.QuickAccess) onQuickAction() else navigateToTab(navController, route.value)
                        },
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
@OptIn(ExperimentalMaterial3Api::class)
private fun QuickActionsSheet(onDismiss: () -> Unit, onNavigate: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Quick access",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyColumn(Modifier.fillMaxWidth()) {
            item { QuickAccessSectionTitle("Log") }
            items(QuickLogItems, key = { it.route.value }) { item ->
                QuickAccessRow(item, onNavigate)
            }
            item { QuickAccessSectionTitle("Go to") }
            items(QuickDestinationItems, key = { it.route.value }) { item ->
                QuickAccessRow(item, onNavigate)
            }
            item { Box(Modifier.padding(bottom = 24.dp)) }
        }
    }
}

@Composable
private fun QuickAccessSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
    )
}

@Composable
private fun QuickAccessRow(item: QuickAccessItem, onNavigate: (String) -> Unit) {
    Surface(
        onClick = { onNavigate(item.route.value) },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(item.title) },
            supportingContent = { Text(item.subtitle) },
            leadingContent = { Icon(item.route.icon, contentDescription = null) }
        )
    }
    HorizontalDivider(Modifier.padding(horizontal = 24.dp))
}

@Composable
private fun SideRail(
    currentDestination: NavDestination?,
    bottomRoutes: List<Route>,
    navController: NavHostController,
    onQuickAction: () -> Unit
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
                } == true && route != Route.QuickAccess
                NavigationRailItem(
                    selected = selected,
                    onClick = {
                        if (route == Route.QuickAccess) onQuickAction() else navigateToTab(navController, route.value)
                    },
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

private fun navigateFromQuickAccess(navController: NavHostController, route: String) {
    if (route == Route.LabelScan.value || route == Route.BarcodeScan.value) {
        navController.navigate(Route.AddFood.value) { launchSingleTop = true }
    }
    navController.navigate(route) { launchSingleTop = true }
}

private data class QuickAccessItem(
    val title: String,
    val subtitle: String,
    val route: Route
)

private val QuickLogItems = listOf(
    QuickAccessItem("Add food", "Search or enter food manually", Route.AddFood),
    QuickAccessItem("Scan nutrition label", "Capture nutrients from a package", Route.LabelScan),
    QuickAccessItem("Scan barcode", "Look up a packaged food", Route.BarcodeScan),
    QuickAccessItem("Scan meal", "Analyze a meal photo", Route.MealAnalysis),
    QuickAccessItem("Start workout", "Choose a plan or quick workout", Route.Workout),
    QuickAccessItem("Log body weight", "Add a new weight measurement", Route.BodyWeight)
)

private val QuickDestinationItems = listOf(
    QuickAccessItem("Today", "Daily overview and hydration", Route.Dashboard),
    QuickAccessItem("Nutrition", "Meals and nutrition targets", Route.Nutrition),
    QuickAccessItem("Workout history", "Review previous training", Route.History),
    QuickAccessItem("Progress", "Nutrition, weight, and strength trends", Route.Progress),
    QuickAccessItem("Coaching", "Personalized recommendations", Route.Recommendations),
    QuickAccessItem("Profile", "Health profile and goals", Route.Profile),
    QuickAccessItem("Settings", "Preferences, privacy, and connections", Route.Settings)
)

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(Route.Dashboard.value) {
            saveState = true
        }
    }
}
