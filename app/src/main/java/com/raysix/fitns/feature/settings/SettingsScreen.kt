package com.raysix.fitns.feature.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.raysix.fitns.core.design.AdaptiveColumn
import com.raysix.fitns.core.design.ErrorBanner
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.domain.repository.AppearanceMode
import com.raysix.fitns.domain.repository.BottomNavigationDestination
import java.io.File

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenN8n: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNutritionGoals: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenCoaching: () -> Unit
) {
    AdaptiveColumn {
        SettingsHeader("Settings", onBack)
        SectionCard(title = "Your FitNS") {
            SettingsRow(Icons.Outlined.Person, "Account", uiState.googleAccount?.let { it.displayName.ifBlank { it.email } } ?: "Not signed in", onOpenAccount)
            SettingsRow(
                Icons.Outlined.CloudSync,
                "n8n & Sync",
                when {
                    !uiState.bearerTokenConfigured -> "Not connected"
                    uiState.n8nSettings.syncEnabled -> "Configured · Sync enabled"
                    else -> "Configured · Sync off"
                },
                onOpenN8n
            )
        }
        SectionCard(title = "Preferences") {
            SettingsRow(Icons.Outlined.Person, "Health profile", "Body metrics, activity, and training goal", onOpenProfile)
            SettingsRow(Icons.Outlined.Restaurant, "Nutrition goals", "Calories, macros, fiber, and water", onOpenNutritionGoals)
            SettingsRow(Icons.AutoMirrored.Outlined.ShowChart, "Progress", "Nutrition, weight, and strength trends", onOpenProgress)
            SettingsRow(Icons.Outlined.Lightbulb, "Coaching tips", "Personalized recommendations", onOpenCoaching)
            SettingsRow(Icons.Outlined.Security, "Privacy & Data", "Photos, local data, and export", onOpenPrivacy)
            SettingsRow(Icons.Outlined.DarkMode, "Appearance", uiState.appearanceMode.name, onOpenAppearance)
            SettingsRow(
                Icons.Outlined.DashboardCustomize,
                "Bottom navigation",
                uiState.bottomNavigation.joinToString(" · ") { it.displayName },
                onOpenNavigation
            )
        }
        Text(
            "FitNS stores health and training data locally. Scanner requests and optional sync use your configured n8n endpoint. Local health data is excluded from Android cloud backup and device transfer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun AccountSettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onCreateSignInIntent: () -> Intent?,
    onSignInResult: (Intent?) -> Boolean,
    onSignOut: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> onSignInResult(result.data) }
    AdaptiveColumn {
        SettingsHeader("Account", onBack)
        SectionCard(title = if (uiState.googleAccount == null) "Google account" else "Signed in") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                    Text(
                        text = uiState.googleAccount?.displayName?.firstOrNull()?.uppercase() ?: "F",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(uiState.googleAccount?.displayName?.ifBlank { "Google account" } ?: "Not signed in", fontWeight = FontWeight.SemiBold)
                    Text(uiState.googleAccount?.email ?: "Sign in to connect your identity to FitNS.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (uiState.googleAccount == null) {
                Button(
                    onClick = { onCreateSignInIntent()?.let(launcher::launch) },
                    enabled = uiState.googleSignInConfigured,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (uiState.googleSignInConfigured) "Continue with Google" else "Google sign-in not configured") }
            } else {
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
            }
        }
        Text("Signing out does not delete your local nutrition or workout data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun N8nSettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onSaveBaseUrl: () -> Unit,
    onBearerTokenChange: (String) -> Unit,
    onSaveBearerToken: () -> Unit,
    onSyncEnabledChange: (Boolean) -> Unit,
    onTestConnection: () -> Unit,
    onSyncNow: () -> Unit
) {
    AdaptiveColumn {
        SettingsHeader("n8n & Sync", onBack)
        SectionCard(title = "Connection", subtitle = if (uiState.bearerTokenConfigured) "Credentials configured" else "Setup required") {
            OutlinedTextField(
                value = uiState.n8nSettings.baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text("Base URL") },
                supportingText = { Text("HTTPS endpoints only") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = onSaveBaseUrl, modifier = Modifier.fillMaxWidth()) { Text("Save address") }
            OutlinedTextField(
                value = uiState.bearerTokenInput,
                onValueChange = onBearerTokenChange,
                label = { Text(if (uiState.bearerTokenConfigured) "Replace bearer token" else "Bearer token") },
                supportingText = { Text(if (uiState.bearerTokenConfigured) "A token is encrypted on this device. Its value is never shown." else "Stored encrypted on this device") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = onSaveBearerToken, modifier = Modifier.fillMaxWidth()) { Text(if (uiState.bearerTokenConfigured) "Update token" else "Save token") }
            Button(onClick = onTestConnection, enabled = !uiState.testingConnection, modifier = Modifier.fillMaxWidth()) {
                Text(if (uiState.testingConnection) "Testing…" else "Test connection")
            }
            val isError = listOf("failed", "error", "invalid", "unreachable", "timed out").any { uiState.connectionStatus.contains(it, true) }
            if (isError) ErrorBanner(uiState.connectionStatus) else Text(uiState.connectionStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard(title = "Sync") {
            SwitchRow("Enable sync", "Queue local changes for your n8n workflows.", uiState.n8nSettings.syncEnabled, onSyncEnabledChange)
            Text("${uiState.pendingSyncCount} pending item${if (uiState.pendingSyncCount == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (uiState.failedSyncCount > 0) {
                ErrorBanner(
                    "${uiState.failedSyncCount} item${if (uiState.failedSyncCount == 1) "" else "s"} need attention. " +
                        (uiState.latestSyncError ?: "Check the connection and credentials, then retry.")
                )
            }
            OutlinedButton(onClick = onSyncNow, enabled = uiState.n8nSettings.syncEnabled, modifier = Modifier.fillMaxWidth()) { Text("Sync now") }
        }
    }
}

@Composable
fun PrivacySettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onMealPhotoAnalysisChange: (Boolean) -> Unit,
    onGenerateExport: () -> Unit,
    onDeleteAllLocalData: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = uiState.deletingLocalData) { }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.deletingLocalData) showDeleteConfirmation = false
            },
            title = { Text("Delete all local data?") },
            text = {
                Text(
                    "This permanently deletes your profile, nutrition and workout history, " +
                        "weight data, settings, encrypted credential, pending sync work, " +
                        "captures, and exports from this device. Data already sent to n8n is " +
                        "not deleted. This action cannot be undone."
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    enabled = !uiState.deletingLocalData
                ) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteAllLocalData()
                    },
                    enabled = !uiState.deletingLocalData,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete permanently") }
            }
        )
    }

    AdaptiveColumn {
        SettingsHeader("Privacy & Data") {
            if (!uiState.deletingLocalData) onBack()
        }
        SectionCard(title = "Photo analysis") {
            SwitchRow(
                "Meal photo analysis",
                "When enabled, capturing a meal immediately uploads the photo to your configured n8n endpoint for analysis.",
                uiState.mealPhotoAnalysisEnabled,
                if (uiState.deletingLocalData) null else onMealPhotoAnalysisChange
            )
            SwitchRow("Temporary photos only", "Captured photos stay in temporary app storage and are removed after processing.", true, null)
            Text("You can turn meal photo uploads off here at any time. Barcode lookup contacts n8n only when you request it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard(title = "Your data") {
            Text("Nutrition, workouts, profile, and weight history are stored locally and excluded from Android cloud backup and device transfer. Sync sends those records to n8n only when enabled; scanner requests are sent when you explicitly run them.")
            Button(
                onClick = onGenerateExport,
                enabled = !uiState.deletingLocalData,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Prepare JSON export") }
            uiState.exportStatus?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            uiState.exportFilePath?.let { path ->
                OutlinedButton(
                    onClick = {
                        runCatching {
                            val exportFile = File(path)
                            check(exportFile.isFile) { "Export file is no longer available" }
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.files",
                                exportFile
                            )
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    },
                                    "Share FitNS export"
                                )
                            )
                        }.onFailure {
                            Toast.makeText(
                                context,
                                "The export could not be shared. Prepare a new export and try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    enabled = !uiState.deletingLocalData,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Share export") }
            }
        }
        SectionCard(
            title = "Delete local data",
            subtitle = "This does not delete data that was already sent to your n8n endpoint."
        ) {
            Text(
                "Permanently remove all FitNS data and credentials stored on this device, then return to onboarding.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            uiState.localDataDeletionError?.let { ErrorBanner(it) }
            Button(
                onClick = { showDeleteConfirmation = true },
                enabled = !uiState.deletingLocalData,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.deletingLocalData) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Text("Deleting…", modifier = Modifier.padding(start = 10.dp))
                } else {
                    Text("Delete all local data")
                }
            }
        }
    }
}

@Composable
fun AppearanceSettingsScreen(uiState: SettingsUiState, onBack: () -> Unit, onModeChange: (AppearanceMode) -> Unit) {
    AdaptiveColumn {
        SettingsHeader("Appearance", onBack)
        SectionCard(title = "Theme") {
            AppearanceMode.entries.forEach { mode ->
                Surface(onClick = { onModeChange(mode) }, color = MaterialTheme.colorScheme.surface) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = uiState.appearanceMode == mode, onClick = null)
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(mode.name, fontWeight = FontWeight.SemiBold)
                            if (mode == AppearanceMode.System) Text("Match your device setting", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationSettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSelectionChange: (BottomNavigationDestination, Boolean) -> Unit,
    onReset: () -> Unit
) {
    AdaptiveColumn {
        SettingsHeader("Bottom navigation", onBack)
        SectionCard(
            title = "Choose your buttons",
            subtitle = "Select up to ${BottomNavigationDestination.MaxSelected}. Only your choices appear in the bar."
        ) {
            BottomNavigationDestination.entries.forEach { destination ->
                val selected = destination in uiState.bottomNavigation
                val selectedPosition = uiState.bottomNavigation.indexOf(destination)
                val enabled = if (selected) {
                    uiState.bottomNavigation.size > 1
                } else {
                    uiState.bottomNavigation.size < BottomNavigationDestination.MaxSelected
                }
                NavigationDestinationRow(
                    destination = destination,
                    selected = selected,
                    position = selectedPosition.takeIf { it >= 0 },
                    enabled = enabled,
                    onSelectionChange = { onSelectionChange(destination, it) }
                )
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Reset to default buttons")
            }
        }
        Text(
            "Buttons appear in the order you select them. Remove and select a button again to move it to the end.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun NavigationDestinationRow(
    destination: BottomNavigationDestination,
    selected: Boolean,
    position: Int?,
    enabled: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(Modifier.weight(1f)) {
            Text(destination.displayName, fontWeight = FontWeight.SemiBold)
            Text(
                position?.let { "Button ${it + 1}" } ?: destination.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = selected,
            onCheckedChange = onSelectionChange,
            enabled = enabled
        )
    }
}

private val BottomNavigationDestination.displayName: String
    get() = when (this) {
        BottomNavigationDestination.Today -> "Today"
        BottomNavigationDestination.Nutrition -> "Nutrition"
        BottomNavigationDestination.Workout -> "Workout"
        BottomNavigationDestination.Progress -> "Progress"
        BottomNavigationDestination.BodyWeight -> "Weight"
        BottomNavigationDestination.Coaching -> "Coaching"
        BottomNavigationDestination.Profile -> "Profile"
        BottomNavigationDestination.Settings -> "Settings"
        BottomNavigationDestination.QuickAccess -> "Quick access"
    }

private val BottomNavigationDestination.description: String
    get() = when (this) {
        BottomNavigationDestination.Today -> "Daily overview and hydration"
        BottomNavigationDestination.Nutrition -> "Meals and nutrition targets"
        BottomNavigationDestination.Workout -> "Start and manage workouts"
        BottomNavigationDestination.Progress -> "Nutrition, weight, and strength trends"
        BottomNavigationDestination.BodyWeight -> "Log and review body weight"
        BottomNavigationDestination.Coaching -> "Personalized recommendations"
        BottomNavigationDestination.Profile -> "Health profile and goals"
        BottomNavigationDestination.Settings -> "App preferences and connections"
        BottomNavigationDestination.QuickAccess -> "Open any area or logging tool"
    }

private val BottomNavigationDestination.icon: ImageVector
    get() = when (this) {
        BottomNavigationDestination.Today -> Icons.Outlined.Home
        BottomNavigationDestination.Nutrition -> Icons.Outlined.Restaurant
        BottomNavigationDestination.Workout -> Icons.Outlined.FitnessCenter
        BottomNavigationDestination.Progress -> Icons.AutoMirrored.Outlined.ShowChart
        BottomNavigationDestination.BodyWeight -> Icons.Outlined.MonitorWeight
        BottomNavigationDestination.Coaching -> Icons.Outlined.Lightbulb
        BottomNavigationDestination.Profile -> Icons.Outlined.Person
        BottomNavigationDestination.Settings -> Icons.Outlined.Settings
        BottomNavigationDestination.QuickAccess -> Icons.Outlined.Apps
    }

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Open $title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(title: String, summary: String, checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = onCheckedChange != null)
    }
}
