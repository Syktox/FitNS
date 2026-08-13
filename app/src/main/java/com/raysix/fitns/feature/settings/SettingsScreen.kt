package com.raysix.fitns.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.AdaptiveTwoColumn
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onN8nBaseUrlChange: (String) -> Unit,
    onBearerTokenChange: (String) -> Unit,
    onSaveBearerToken: () -> Unit,
    onSyncEnabledChange: (Boolean) -> Unit,
    onTemporaryPhotosOnlyChange: (Boolean) -> Unit,
    onTestConnection: () -> Unit,
    onRetrySyncNow: () -> Unit,
    onGenerateExport: () -> Unit
) {
    AdaptiveTwoColumn(
        header = {
            ScreenHeader(
                title = "Settings",
                subtitle = "Sync, privacy, and local export controls."
            )
        },
        main = {
            SectionCard(
                title = "n8n Connection",
                trailing = {
                    StatusPill(uiState.connectionStatus)
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.n8nSettings.baseUrl,
                        onValueChange = onN8nBaseUrlChange,
                        label = { Text("Base URL") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.bearerTokenInput,
                        onValueChange = onBearerTokenChange,
                        label = { Text(if (uiState.bearerTokenConfigured) "Replace bearer token" else "Bearer token") },
                        supportingText = { Text(if (uiState.bearerTokenConfigured) "A token is stored securely on this device." else "Required by the protected n8n webhooks.") },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        onClick = onSaveBearerToken,
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.bearerTokenConfigured) "Update token" else "Save token",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    Surface(
                        onClick = onTestConnection,
                        enabled = !uiState.testingConnection,
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.testingConnection) "Testing..." else "Test connection",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    SettingRow(
                        label = "Enable sync",
                        supportingText = "Queue local changes for n8n when enabled.",
                        checked = uiState.n8nSettings.syncEnabled,
                        onCheckedChange = onSyncEnabledChange
                    )
                    Text("Pending sync items: ${uiState.pendingSyncCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        onClick = onRetrySyncNow,
                        enabled = uiState.n8nSettings.syncEnabled && uiState.pendingSyncCount > 0,
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Sync now",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        side = {
            SectionCard(title = "Privacy") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingRow(
                        label = "Store photos temporarily only",
                        supportingText = "Photo analysis should not persist raw images longer than needed.",
                        checked = uiState.temporaryPhotosOnly,
                        onCheckedChange = onTemporaryPhotosOnlyChange
                    )
                    Text("Photo uploads require explicit consent for each analysis.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        onClick = onGenerateExport,
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Prepare JSON export",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    uiState.exportStatus?.let { status ->
                        Text(status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                    uiState.exportPreview?.let { preview ->
                        ModernCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface) {
                            Text(
                                text = preview,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SettingRow(label: String, supportingText: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(supportingText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = when {
        status.contains("successful", ignoreCase = true) -> MaterialTheme.colorScheme.primary
        status.contains("testing", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
        status.contains("not tested", ignoreCase = true) -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(status, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
