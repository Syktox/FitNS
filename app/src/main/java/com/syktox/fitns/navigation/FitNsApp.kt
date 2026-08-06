package com.syktox.fitns.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syktox.fitns.core.design.ScreenHeader
import com.syktox.fitns.feature.bodyweight.BodyWeightScreen
import com.syktox.fitns.feature.bodyweight.BodyWeightViewModel
import com.syktox.fitns.feature.dashboard.DashboardScreen
import com.syktox.fitns.feature.dashboard.DashboardViewModel
import com.syktox.fitns.feature.nutrition.ManualFoodScreen
import com.syktox.fitns.feature.nutrition.NutritionDayScreen
import com.syktox.fitns.feature.nutrition.NutritionViewModel
import com.syktox.fitns.feature.profile.ProfileScreen
import com.syktox.fitns.feature.profile.ProfileViewModel
import com.syktox.fitns.feature.progress.ProgressScreen
import com.syktox.fitns.feature.progress.ProgressViewModel
import com.syktox.fitns.feature.recommendations.RecommendationsScreen
import com.syktox.fitns.feature.recommendations.RecommendationsViewModel
import com.syktox.fitns.feature.settings.SettingsScreen
import com.syktox.fitns.feature.settings.SettingsViewModel
import com.syktox.fitns.feature.workout.WorkoutHistoryScreen
import com.syktox.fitns.feature.workout.WorkoutStartScreen
import com.syktox.fitns.feature.workout.WorkoutViewModel

private enum class Route(val value: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Today", Icons.Outlined.Home),
    Nutrition("nutrition", "Nutrition", Icons.Outlined.Restaurant),
    AddFood("add-food", "Add", Icons.Outlined.Restaurant),
    Workout("workout", "Workout", Icons.Outlined.FitnessCenter),
    BodyWeight("bodyweight", "Weight", Icons.Outlined.MonitorWeight),
    Progress("progress", "Progress", Icons.AutoMirrored.Outlined.ShowChart),
    Recommendations("recommendations", "Tips", Icons.Outlined.Lightbulb),
    History("history", "History", Icons.Outlined.FitnessCenter),
    Profile("profile", "Profile", Icons.Outlined.Person),
    Settings("settings", "Settings", Icons.Outlined.Settings),
    More("more", "More", Icons.Outlined.MoreHoriz)
}

@Composable
fun FitNsApp() {
    val navController = rememberNavController()
    val bottomRoutes = listOf(Route.Dashboard, Route.Nutrition, Route.Workout, Route.Progress, Route.More)
    val moreRoutes = listOf(Route.BodyWeight, Route.Recommendations, Route.Profile, Route.Settings)
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomRoutes.forEach { route ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { destination ->
                            destination.route == route.value || (route == Route.More && destination.route in moreRoutes.map { it.value })
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
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Dashboard.value,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Dashboard.value) {
                val viewModel: DashboardViewModel = hiltViewModel()
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                DashboardScreen(
                    dashboard = uiState.dashboard,
                    workoutSummary = uiState.workoutSummary,
                    readiness = uiState.readiness,
                    coach = uiState.coach,
                    mealBreakdown = uiState.mealBreakdown,
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
                    onSave = { input ->
                        viewModel.addFood(input) {
                            navController.popBackStack()
                        }
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
            composable(Route.More.value) {
                MoreScreen(
                    onOpenWeight = { navController.navigate(Route.BodyWeight.value) },
                    onOpenTips = { navController.navigate(Route.Recommendations.value) },
                    onOpenProfile = { navController.navigate(Route.Profile.value) },
                    onOpenSettings = { navController.navigate(Route.Settings.value) }
                )
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
                    onSave = viewModel::save
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

@Composable
private fun MoreScreen(
    onOpenWeight: () -> Unit,
    onOpenTips: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "More",
            subtitle = "Account, settings, weight tracking, and coaching tools."
        )
        MoreRouteCard(title = "Weight", subtitle = "Log body weight and review trend progress.", onOpen = onOpenWeight)
        MoreRouteCard(title = "Tips", subtitle = "Review nutrition, recovery, and workout recommendations.", onOpen = onOpenTips)
        MoreRouteCard(title = "Profile", subtitle = "Manage goals, body metrics, and nutrition targets.", onOpen = onOpenProfile)
        MoreRouteCard(title = "Settings", subtitle = "Sync, privacy, connection, and export controls.", onOpen = onOpenSettings)
    }
}

@Composable
private fun MoreRouteCard(title: String, subtitle: String, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle)
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Text("Open")
            }
        }
    }
}
