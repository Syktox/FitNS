package com.syktox.fitns.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syktox.fitns.core.design.ScreenHeader
import com.syktox.fitns.core.design.SectionTitle

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onN8nBaseUrlChange: (String) -> Unit,
    onSyncEnabledChange: (Boolean) -> Unit,
    onTemporaryPhotosOnlyChange: (Boolean) -> Unit,
    onTestConnection: () -> Unit,
    onRetrySyncNow: () -> Unit,
    onGenerateExport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Settings",
            subtitle = "Sync, privacy, and local export controls."
        )
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionTitle("n8n Connection")
                    StatusPill(uiState.connectionStatus)
                }
                OutlinedTextField(
                    value = uiState.n8nSettings.baseUrl,
                    onValueChange = onN8nBaseUrlChange,
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onTestConnection,
                    enabled = !uiState.testingConnection
                ) {
                    Text(if (uiState.testingConnection) "Testing..." else "Test connection")
                }
                SettingRow(
                    label = "Enable sync",
                    supportingText = "Queue local changes for n8n when enabled.",
                    checked = uiState.n8nSettings.syncEnabled,
                    onCheckedChange = onSyncEnabledChange
                )
                Text("Pending sync items: ${uiState.pendingSyncCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = onRetrySyncNow,
                    enabled = uiState.n8nSettings.syncEnabled && uiState.pendingSyncCount > 0
                ) {
                    Text("Sync now")
                }
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Privacy")
                SettingRow(
                    label = "Store photos temporarily only",
                    supportingText = "Photo analysis should not persist raw images longer than needed.",
                    checked = uiState.temporaryPhotosOnly,
                    onCheckedChange = onTemporaryPhotosOnlyChange
                )
                Text("Photo uploads require explicit consent for each analysis.")
                Button(onClick = onGenerateExport) {
                    Text("Prepare JSON export")
                }
                uiState.exportStatus?.let { status ->
                    Text(status, color = MaterialTheme.colorScheme.primary)
                }
                uiState.exportPreview?.let { preview ->
                    Card {
                        Text(
                            text = preview,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, supportingText: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
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
            .padding(start = 8.dp)
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(status, color = color, style = MaterialTheme.typography.labelMedium)
    }
}
