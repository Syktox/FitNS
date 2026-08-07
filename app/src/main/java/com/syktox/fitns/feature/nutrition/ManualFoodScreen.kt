package com.syktox.fitns.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.syktox.fitns.core.design.ScreenHeader
import com.syktox.fitns.core.design.SectionTitle
import com.syktox.fitns.domain.model.MealType
import com.syktox.fitns.domain.model.Micronutrients

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
    val micronutrients: Micronutrients = Micronutrients()
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualFoodScreen(
    barcodeLookup: BarcodeLookupUiState,
    onBarcodeChange: (String) -> Unit,
    onLookupBarcode: () -> Unit,
    onPrefillConsumed: () -> Unit,
    onSave: (ManualFoodInput) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var saturatedFat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var salt by remember { mutableStateOf("") }
    var sodium by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.Snack) }

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
            mealType = input.mealType
            onPrefillConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Add Food",
            subtitle = "Review every detail before saving it to your log."
        )
        BarcodeLookupCard(
            state = barcodeLookup,
            onBarcodeChange = onBarcodeChange,
            onLookup = onLookupBarcode
        )
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Product")
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
                NumericField(value = grams, onValueChange = { grams = it }, label = "Amount in grams")
                PortionPresetChips(
                    currentGrams = grams,
                    onSelectGrams = { grams = it }
                )
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Nutrition per serving")
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
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Meal")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner, MealType.Snack).forEach { type ->
                        FilterChip(
                            selected = mealType == type,
                            onClick = { mealType = type },
                            label = { Text(type.name) }
                        )
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
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
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
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
    onLookup: () -> Unit
) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Barcode Lookup", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.barcode,
                onValueChange = onBarcodeChange,
                label = { Text("Barcode") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onLookup,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.loading) "Looking up..." else "Lookup product")
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
        modifier = modifier
    )
}

private fun Double.formatPlain(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
