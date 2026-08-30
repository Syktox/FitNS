package com.raysix.fitns.feature.nutrition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.raysix.fitns.R
import com.raysix.fitns.core.design.AccentAmber
import com.raysix.fitns.core.design.AccentRose
import com.raysix.fitns.core.design.isCompactHeight
import com.raysix.fitns.domain.model.MealType
import kotlin.math.roundToInt

enum class InlineStatusKind { Info, Success, Error }

@Composable
fun FoodLoggingTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("food_top_back")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            onClose?.let { close ->
                IconButton(onClick = close) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            } ?: Image(
                painter = painterResource(R.drawable.whale_coach),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        }
    }
}

@Composable
private fun OceanTileGlow(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawCircle(
            color = Color.White.copy(alpha = 0.16f),
            radius = size.minDimension * 0.42f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.12f)
        )
        drawCircle(
            color = primary.copy(alpha = 0.09f),
            radius = size.minDimension * 0.28f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.76f)
        )
    }
}

@Composable
private fun CaptureTileContent(
    title: String,
    subtitle: String,
    icon: ImageVector,
    emphasized: Boolean,
    contentColor: Color
) {
    Column(
        modifier = Modifier.padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (emphasized) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (emphasized) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(9.dp).size(22.dp)
            )
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = if (emphasized) contentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CaptureMethodTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    val containerColor = if (emphasized) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (emphasized) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 108.dp),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        border = if (emphasized) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box {
            OceanTileGlow(Modifier.matchParentSize())
            CaptureTileContent(
                title = title,
                subtitle = subtitle,
                icon = icon,
                emphasized = emphasized,
                contentColor = contentColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealTypeSelector(
    selected: MealType,
    onSelected: (MealType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().testTag("meal_type_selector"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack).forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                enabled = enabled,
                label = { Text(type.displayLabel()) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealDestinationDialog(
    selected: MealType?,
    onSelected: (MealType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (MealType) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Add to which meal?",
    description: String = "Choose where the meal should appear in today's diary.",
    confirmLabel: String = "Add to diary"
) {
    val compactHeight = isCompactHeight()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("meal_destination_dialog"),
        icon = if (compactHeight) {
            null
        } else {
            {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.padding(11.dp).size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                style = if (compactHeight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (compactHeight) 8.dp else 16.dp)
            ) {
                Text(
                    text = if (compactHeight) "Choose where the meal should appear." else description,
                    style = if (compactHeight) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (compactHeight) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )
                val mealOptions = listOf(
                    MealType.Breakfast,
                    MealType.Lunch,
                    MealType.Dinner,
                    MealType.Snack
                )
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 360.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mealOptions.chunked(2).forEach { rowOptions ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowOptions.forEach { type ->
                                        MealDestinationChip(
                                            type = type,
                                            selected = selected == type,
                                            onSelected = onSelected,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            mealOptions.forEach { type ->
                                MealDestinationChip(
                                    type = type,
                                    selected = selected == type,
                                    onSelected = onSelected,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                if (selected != null) {
                    Text(
                        text = "Selected: ${selected.displayLabel()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selected?.let(onConfirm) },
                enabled = selected != null,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("meal_destination_confirm")
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun MealDestinationChip(
    type: MealType,
    selected: Boolean,
    onSelected: (MealType) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Surface(
        onClick = { onSelected(type) },
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag("meal_destination_${type.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = type.displayLabel(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MacroSummary(
    calories: Double,
    protein: Double,
    carbohydrates: Double,
    fat: Double,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxWidth < 500.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroMetric(
                    label = "Energy",
                    value = "${calories.coerceAtLeast(0.0).roundToInt()} kcal",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroMetric("Protein", "${protein.coerceAtLeast(0.0).roundToInt()} g", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    MacroMetric("Carbs", "${carbohydrates.coerceAtLeast(0.0).roundToInt()} g", AccentAmber, Modifier.weight(1f))
                    MacroMetric("Fat", "${fat.coerceAtLeast(0.0).roundToInt()} g", AccentRose, Modifier.weight(1f))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroMetric("Energy", "${calories.coerceAtLeast(0.0).roundToInt()} kcal", MaterialTheme.colorScheme.primary, Modifier.weight(1.25f))
                MacroMetric("Protein", "${protein.coerceAtLeast(0.0).roundToInt()} g", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                MacroMetric("Carbs", "${carbohydrates.coerceAtLeast(0.0).roundToInt()} g", AccentAmber, Modifier.weight(1f))
                MacroMetric("Fat", "${fat.coerceAtLeast(0.0).roundToInt()} g", AccentRose, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.13f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun InlineStatus(
    message: String,
    kind: InlineStatusKind,
    modifier: Modifier = Modifier
) {
    val container = when (kind) {
        InlineStatusKind.Info -> MaterialTheme.colorScheme.surfaceContainerHigh
        InlineStatusKind.Success -> MaterialTheme.colorScheme.primaryContainer
        InlineStatusKind.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (kind) {
        InlineStatusKind.Info -> MaterialTheme.colorScheme.onSurface
        InlineStatusKind.Success -> MaterialTheme.colorScheme.onPrimaryContainer
        InlineStatusKind.Error -> MaterialTheme.colorScheme.onErrorContainer
    }
    val icon = when (kind) {
        InlineStatusKind.Info -> Icons.Filled.Info
        InlineStatusKind.Success -> Icons.Filled.CheckCircle
        InlineStatusKind.Error -> Icons.Filled.Error
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = if (kind == InlineStatusKind.Error) {
                    LiveRegionMode.Assertive
                } else {
                    LiveRegionMode.Polite
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PersistentFoodActionBar(
    label: String,
    supportingText: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (enabled) "Ready to make waves" else "Complete the required fields",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onClick,
                enabled = enabled && !loading,
                modifier = Modifier
                    .heightIn(min = 54.dp)
                    .semantics {
                        if (loading) {
                            contentDescription = "$label in progress"
                            stateDescription = "In progress"
                        }
                    }
                    .testTag("food_primary_action"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(label, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun MealType.displayLabel(): String = when (this) {
    MealType.Breakfast -> "Breakfast"
    MealType.Lunch -> "Lunch"
    MealType.Dinner -> "Dinner"
    MealType.Snack -> "Snack"
    MealType.Custom -> "Other"
}
