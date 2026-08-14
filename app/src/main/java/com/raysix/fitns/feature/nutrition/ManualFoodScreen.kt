package com.raysix.fitns.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raysix.fitns.core.design.AdaptiveGutterLayout
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.ScreenHeader
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.domain.model.DataQuality
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.domain.model.Micronutrients

data class ManualFoodInput(
    val name: String,
    val brand: String?,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbohydrates: Double,
    val sugar: Double,
    val fat: Double,
    val saturatedFat: Double,
    val fiber: Double,
    val salt: Double,
    val sodiumMilligrams: Double?,
    val mealType: MealType,
    val notes: String,
    val micronutrients: Micronutrients = Micronutrients(),
    val dataQuality: DataQuality = DataQuality.Verified
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualFoodScreen(
    barcodeLookup: BarcodeLookupUiState,
    onBarcodeChange: (String) -> Unit,
    onLookupBarcode: () -> Unit,
    onPrefillConsumed: () -> Unit,
    onScanLabel: () -> Unit,
    onScanBarcode: () -> Unit,
    onScanMeal: () -> Unit,
    onSave: (ManualFoodInput) -> Unit,
    onCancel: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var grams by rememberSaveable { mutableStateOf("100") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var sugar by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var saturatedFat by rememberSaveable { mutableStateOf("") }
    var fiber by rememberSaveable { mutableStateOf("") }
    var salt by rememberSaveable { mutableStateOf("") }
    var sodium by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var mealTypeName by rememberSaveable { mutableStateOf(MealType.Snack.name) }
    val mealType = MealType.entries.firstOrNull { it.name == mealTypeName } ?: MealType.Snack

    LaunchedEffect(barcodeLookup.prefillInput) {
        barcodeLookup.prefillInput?.let { input ->
            name = input.name
            brand = input.brand.orEmpty()
            grams = input.grams.formatPlain()
            calories = input.calories.formatPlain()
            protein = input.protein.formatPlain()
            carbs = input.carbohydrates.formatPlain()
            sugar = input.sugar.formatPlain()
            fat = input.fat.formatPlain()
            saturatedFat = input.saturatedFat.formatPlain()
            fiber = input.fiber.formatPlain()
            salt = input.salt.formatPlain()
            sodium = input.sodiumMilligrams?.formatPlain().orEmpty()
            notes = input.notes
            mealTypeName = input.mealType.name
            onPrefillConsumed()
        }
    }

    AdaptiveGutterLayout(
        header = {
            ScreenHeader(
                title = "Add Food",
                subtitle = "Scan, photograph, or enter details for a food. Review every detail before saving.",
                actions = { TextButton(onClick = onCancel) { Text("Close") } }
            )
        },
        gutterWidthFraction = 0.4f,
        gutter = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ScanMealCard(onScanMeal = onScanMeal)
                BarcodeLookupCard(
                    state = barcodeLookup,
                    onBarcodeChange = onBarcodeChange,
                    onLookup = onLookupBarcode,
                    onScanBarcode = onScanBarcode
                )
                SectionCard(title = "Nutrition label", subtitle = "Photograph the nutrition table to fill in the values. OCR results are a draft and must be reviewed.") {
                    OutlinedButton(
                        onClick = onScanLabel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Scan nutrition label")
                    }
                }
            }
        },
        main = {
            SectionCard(title = "Product") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                    NumericField(value = grams, onValueChange = { grams = it }, label = "Amount in grams")
                    PortionPresetChips(
                        currentGrams = grams,
                        onSelectGrams = { grams = it }
                    )
                }
            }
            SectionCard(title = "Nutrition per serving") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumericField(value = calories, onValueChange = { calories = it }, label = "Calories")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(value = protein, onValueChange = { protein = it }, label = "Protein g", modifier = Modifier.weight(1f))
                        NumericField(value = carbs, onValueChange = { carbs = it }, label = "Carbs g", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(value = fat, onValueChange = { fat = it }, label = "Fat g", modifier = Modifier.weight(1f))
                        NumericField(value = sugar, onValueChange = { sugar = it }, label = "Sugar g", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(value = saturatedFat, onValueChange = { saturatedFat = it }, label = "Saturated fat g", modifier = Modifier.weight(1f))
                        NumericField(value = fiber, onValueChange = { fiber = it }, label = "Fiber g", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(value = salt, onValueChange = { salt = it }, label = "Salt g", modifier = Modifier.weight(1f))
                        NumericField(value = sodium, onValueChange = { sodium = it }, label = "Sodium mg", modifier = Modifier.weight(1f))
                    }
                }
            }
            SectionCard(title = "Meal") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack).forEach { type ->
                            FilterChip(
                                selected = mealType == type,
                                onClick = { mealTypeName = type.name },
                                label = { Text(type.name) }
                            )
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = {
                        onSave(
                            ManualFoodInput(
                                name = name,
                                brand = brand.ifBlank { null },
                                grams = grams.toDoubleOrNull() ?: 0.0,
                                calories = calories.toDoubleOrNull() ?: 0.0,
                                protein = protein.toDoubleOrNull() ?: 0.0,
                                carbohydrates = carbs.toDoubleOrNull() ?: 0.0,
                                sugar = sugar.toDoubleOrNull() ?: 0.0,
                                fat = fat.toDoubleOrNull() ?: 0.0,
                                saturatedFat = saturatedFat.toDoubleOrNull() ?: 0.0,
                                fiber = fiber.toDoubleOrNull() ?: 0.0,
                                salt = salt.toDoubleOrNull() ?: 0.0,
                                sodiumMilligrams = sodium.toDoubleOrNull(),
                                mealType = mealType,
                                notes = notes
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 13.dp)
                    )
                }
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun ScanMealCard(onScanMeal: () -> Unit) {
    ModernCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Scan a meal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Photograph your plate and get estimated macros for each item, powered by your n8n instance.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                onClick = onScanMeal,
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Take photo & analyze",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PortionPresetChips(currentGrams: String, onSelectGrams: (String) -> Unit) {
    val presets = listOf("30", "50", "100", "150", "200", "250")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { preset ->
            FilterChip(
                selected = currentGrams == preset,
                onClick = { onSelectGrams(preset) },
                label = { Text("${preset} g") }
            )
        }
    }
}

@Composable
private fun BarcodeLookupCard(
    state: BarcodeLookupUiState,
    onBarcodeChange: (String) -> Unit,
    onLookup: () -> Unit,
    onScanBarcode: () -> Unit
) {
    SectionCard(title = "Barcode Lookup") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.barcode,
                onValueChange = onBarcodeChange,
                label = { Text("Barcode") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Surface(
                onClick = onLookup,
                enabled = !state.loading,
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.loading) "Looking up..." else "Lookup product",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            OutlinedButton(
                onClick = onScanBarcode,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan barcode with camera")
            }
            state.statusMessage?.let { message ->
                Text(
                    text = message,
                    color = if (message.startsWith("Product found")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    )
}

private fun Double.formatPlain(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
