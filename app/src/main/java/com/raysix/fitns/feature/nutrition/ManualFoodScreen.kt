package com.raysix.fitns.feature.nutrition

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.raysix.fitns.R
import com.raysix.fitns.core.design.ModernCard
import com.raysix.fitns.core.design.SectionCard
import com.raysix.fitns.core.input.toUserDecimalOrNull
import com.raysix.fitns.domain.model.CustomFood
import com.raysix.fitns.domain.model.DataQuality
import com.raysix.fitns.domain.model.FoodFavoritePreset
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.FoodSearchSections
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.domain.model.Micronutrients
import com.raysix.fitns.domain.model.MicronutrientValue
import com.raysix.fitns.domain.model.NutrientKey
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.round
import kotlin.math.roundToInt

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

private enum class AddFoodStage { Choose, Edit }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualFoodScreen(
    barcodeLookup: BarcodeLookupUiState,
    foodSearch: FoodSearchSections,
    onFoodSearchQueryChange: (String) -> Unit,
    onSelectRecentFood: (FoodLogEntry) -> Unit,
    onSelectFavoriteFood: (FoodFavoritePreset) -> Unit,
    onSelectCustomFood: (CustomFood) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onLookupBarcode: () -> Unit,
    onPrefillConsumed: () -> Unit,
    onScanLabel: () -> Unit,
    onScanBarcode: () -> Unit,
    onScanMeal: () -> Unit,
    onSave: (ManualFoodInput) -> Unit,
    onCancel: () -> Unit
) {
    var stageName by rememberSaveable { mutableStateOf(AddFoodStage.Choose.name) }
    val stage = AddFoodStage.entries.firstOrNull { it.name == stageName } ?: AddFoodStage.Choose
    var name by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var grams by rememberSaveable { mutableStateOf("") }
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
    var showMealPicker by rememberSaveable { mutableStateOf(false) }
    var selectedMealTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    var showNutritionDetails by rememberSaveable { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(false) }
    var autoScaleNutrition by rememberSaveable { mutableStateOf(false) }
    var micronutrients by rememberSaveable(stateSaver = MicronutrientsSaver) {
        mutableStateOf(Micronutrients())
    }
    var dataQualityName by rememberSaveable { mutableStateOf(DataQuality.Verified.name) }
    var baselineGrams by rememberSaveable { mutableStateOf("") }
    var baselineCalories by rememberSaveable { mutableStateOf("") }
    var baselineProtein by rememberSaveable { mutableStateOf("") }
    var baselineCarbs by rememberSaveable { mutableStateOf("") }
    var baselineSugar by rememberSaveable { mutableStateOf("") }
    var baselineFat by rememberSaveable { mutableStateOf("") }
    var baselineSaturatedFat by rememberSaveable { mutableStateOf("") }
    var baselineFiber by rememberSaveable { mutableStateOf("") }
    var baselineSalt by rememberSaveable { mutableStateOf("") }
    var baselineSodium by rememberSaveable { mutableStateOf("") }
    var baselineMicronutrients by rememberSaveable(stateSaver = MicronutrientsSaver) {
        mutableStateOf(Micronutrients())
    }

    val selectedMealType = selectedMealTypeName?.let { selectedName ->
        MealType.entries.firstOrNull { it.name == selectedName }
    }
    val dataQuality = DataQuality.entries.firstOrNull { it.name == dataQualityName } ?: DataQuality.Verified
    val gramsValue = grams.toUserDecimalOrNull()
    val numericValues = listOf(calories, protein, carbs, sugar, fat, saturatedFat, fiber, salt, sodium)
    val nutritionValuesAreValid = numericValues.all { value ->
        value.isBlank() || value.toUserDecimalOrNull()?.let { it >= 0.0 } == true
    }
    val interactionLocked = barcodeLookup.loading || barcodeLookup.savingFood
    val canSave = name.isNotBlank() && gramsValue?.let { it > 0.0 } == true && nutritionValuesAreValid &&
        !interactionLocked
    val calorieValue = calories.toUserDecimalOrNull() ?: 0.0
    val proteinValue = protein.toUserDecimalOrNull() ?: 0.0
    val carbsValue = carbs.toUserDecimalOrNull() ?: 0.0
    val fatValue = fat.toUserDecimalOrNull() ?: 0.0

    fun setPortion(value: String, rescale: Boolean) {
        if (rescale && autoScaleNutrition) {
            val sourceGrams = baselineGrams.toUserDecimalOrNull()
            val next = value.toUserDecimalOrNull()
            if (sourceGrams != null && sourceGrams > 0.0 && next != null && next > 0.0) {
                val factor = next / sourceGrams
                calories = baselineCalories.scaledBy(factor)
                protein = baselineProtein.scaledBy(factor)
                carbs = baselineCarbs.scaledBy(factor)
                sugar = baselineSugar.scaledBy(factor)
                fat = baselineFat.scaledBy(factor)
                saturatedFat = baselineSaturatedFat.scaledBy(factor)
                fiber = baselineFiber.scaledBy(factor)
                salt = baselineSalt.scaledBy(factor)
                sodium = baselineSodium.scaledBy(factor)
                micronutrients = baselineMicronutrients.scaledBy(factor)
            }
        }
        grams = value
    }

    fun resetDraft() {
        name = ""
        brand = ""
        grams = ""
        calories = ""
        protein = ""
        carbs = ""
        sugar = ""
        fat = ""
        saturatedFat = ""
        fiber = ""
        salt = ""
        sodium = ""
        notes = ""
        showMealPicker = false
        selectedMealTypeName = null
        showNutritionDetails = false
        showNotes = false
        autoScaleNutrition = false
        micronutrients = Micronutrients()
        dataQualityName = DataQuality.Verified.name
        baselineGrams = ""
        baselineCalories = ""
        baselineProtein = ""
        baselineCarbs = ""
        baselineSugar = ""
        baselineFat = ""
        baselineSaturatedFat = ""
        baselineFiber = ""
        baselineSalt = ""
        baselineSodium = ""
        baselineMicronutrients = Micronutrients()
    }

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
            showMealPicker = false
            selectedMealTypeName = null
            micronutrients = input.micronutrients
            dataQualityName = input.dataQuality.name
            baselineGrams = input.grams.formatPlain()
            baselineCalories = input.calories.formatPlain()
            baselineProtein = input.protein.formatPlain()
            baselineCarbs = input.carbohydrates.formatPlain()
            baselineSugar = input.sugar.formatPlain()
            baselineFat = input.fat.formatPlain()
            baselineSaturatedFat = input.saturatedFat.formatPlain()
            baselineFiber = input.fiber.formatPlain()
            baselineSalt = input.salt.formatPlain()
            baselineSodium = input.sodiumMilligrams?.formatPlain().orEmpty()
            baselineMicronutrients = input.micronutrients
            showNutritionDetails = listOf(input.sugar, input.saturatedFat, input.fiber, input.salt)
                .any { it > 0.0 } || input.sodiumMilligrams != null
            showNotes = input.notes.isNotBlank()
            autoScaleNutrition = true
            stageName = AddFoodStage.Edit.name
            onPrefillConsumed()
        }
    }

    BackHandler(enabled = stage == AddFoodStage.Edit) {
        if (!barcodeLookup.savingFood) stageName = AddFoodStage.Choose.name
    }

    if (showMealPicker) {
        MealDestinationDialog(
            selected = selectedMealType,
            onSelected = { selectedMealTypeName = it.name },
            onDismiss = {
                showMealPicker = false
                selectedMealTypeName = null
            },
            onConfirm = { confirmedMealType ->
                showMealPicker = false
                selectedMealTypeName = null
                onSave(
                    ManualFoodInput(
                        name = name.trim(),
                        brand = brand.trim().ifBlank { null },
                        grams = gramsValue ?: 0.0,
                        calories = calorieValue,
                        protein = proteinValue,
                        carbohydrates = carbsValue,
                        sugar = sugar.toUserDecimalOrNull() ?: 0.0,
                        fat = fatValue,
                        saturatedFat = saturatedFat.toUserDecimalOrNull() ?: 0.0,
                        fiber = fiber.toUserDecimalOrNull() ?: 0.0,
                        salt = salt.toUserDecimalOrNull() ?: 0.0,
                        sodiumMilligrams = sodium.toUserDecimalOrNull(),
                        mealType = confirmedMealType,
                        notes = notes.trim(),
                        micronutrients = micronutrients,
                        dataQuality = dataQuality
                    )
                )
            },
            title = "Add to which meal?",
            description = "Choose where the meal should appear in today's diary.",
            confirmLabel = "Add food"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        FoodLoggingTopBar(
            title = if (stage == AddFoodStage.Choose) "Add food" else "Review food",
            subtitle = if (stage == AddFoodStage.Choose) {
                "Search, scan, or create an entry"
            } else {
                "Everything stays editable before you log it"
            },
            onBack = {
                if (!barcodeLookup.savingFood) {
                    if (stage == AddFoodStage.Edit) stageName = AddFoodStage.Choose.name else onCancel()
                }
            },
            onClose = if (stage == AddFoodStage.Edit) {
                { if (!barcodeLookup.savingFood) onCancel() }
            } else {
                null
            }
        )

        if (stage == AddFoodStage.Choose) {
            ChooseFoodStage(
                foodSearch = foodSearch,
                barcodeLookup = barcodeLookup,
                onFoodSearchQueryChange = onFoodSearchQueryChange,
                onSelectRecentFood = onSelectRecentFood,
                onSelectFavoriteFood = onSelectFavoriteFood,
                onSelectCustomFood = onSelectCustomFood,
                onBarcodeChange = onBarcodeChange,
                onLookupBarcode = onLookupBarcode,
                onScanBarcode = onScanBarcode,
                onScanLabel = onScanLabel,
                onScanMeal = onScanMeal,
                onManualEntry = {
                    resetDraft()
                    stageName = AddFoodStage.Edit.name
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            FoodEditorStage(
                name = name,
                onNameChange = { name = it },
                brand = brand,
                onBrandChange = { brand = it },
                grams = grams,
                onGramsChange = { setPortion(it, true) },
                calories = calories,
                onCaloriesChange = { calories = it; autoScaleNutrition = false },
                protein = protein,
                onProteinChange = { protein = it; autoScaleNutrition = false },
                carbs = carbs,
                onCarbsChange = { carbs = it; autoScaleNutrition = false },
                fat = fat,
                onFatChange = { fat = it; autoScaleNutrition = false },
                sugar = sugar,
                onSugarChange = { sugar = it; autoScaleNutrition = false },
                saturatedFat = saturatedFat,
                onSaturatedFatChange = { saturatedFat = it; autoScaleNutrition = false },
                fiber = fiber,
                onFiberChange = { fiber = it; autoScaleNutrition = false },
                salt = salt,
                onSaltChange = { salt = it; autoScaleNutrition = false },
                sodium = sodium,
                onSodiumChange = { sodium = it; autoScaleNutrition = false },
                notes = notes,
                onNotesChange = { notes = it },
                showNutritionDetails = showNutritionDetails,
                onToggleNutritionDetails = { showNutritionDetails = !showNutritionDetails },
                showNotes = showNotes,
                onToggleNotes = { showNotes = !showNotes },
                autoScaleNutrition = autoScaleNutrition,
                macroValues = MacroValues(calorieValue, proteinValue, carbsValue, fatValue),
                barcodeLookup = barcodeLookup,
                modifier = Modifier.weight(1f)
            )
            PersistentFoodActionBar(
                label = "Add • ${calorieValue.coerceAtLeast(0.0).roundToIntSafe()} kcal",
                supportingText = "${gramsValue?.formatPlain() ?: "—"} g · choose meal next",
                enabled = canSave,
                loading = barcodeLookup.savingFood,
                onClick = {
                    selectedMealTypeName = null
                    showMealPicker = true
                },
                modifier = Modifier.imePadding()
            )
        }
    }
}

@Composable
private fun ChooseFoodStage(
    foodSearch: FoodSearchSections,
    barcodeLookup: BarcodeLookupUiState,
    onFoodSearchQueryChange: (String) -> Unit,
    onSelectRecentFood: (FoodLogEntry) -> Unit,
    onSelectFavoriteFood: (FoodFavoritePreset) -> Unit,
    onSelectCustomFood: (CustomFood) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onLookupBarcode: () -> Unit,
    onScanBarcode: () -> Unit,
    onScanLabel: () -> Unit,
    onScanMeal: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ScrollPane(modifier = Modifier.weight(0.46f).fillMaxHeight()) {
                    CaptureAndCreatePanel(
                        barcodeLookup = barcodeLookup,
                        onBarcodeChange = onBarcodeChange,
                        onLookupBarcode = onLookupBarcode,
                        onScanBarcode = onScanBarcode,
                        onScanLabel = onScanLabel,
                        onScanMeal = onScanMeal,
                        onManualEntry = onManualEntry
                    )
                }
                ScrollPane(modifier = Modifier.weight(0.54f).fillMaxHeight()) {
                    FoodLibraryPanel(
                        foodSearch = foodSearch,
                        onQueryChange = onFoodSearchQueryChange,
                        onSelectRecentFood = onSelectRecentFood,
                        onSelectFavoriteFood = onSelectFavoriteFood,
                        onSelectCustomFood = onSelectCustomFood
                    )
                }
            }
        } else {
            ScrollPane(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                CaptureAndCreatePanel(
                    barcodeLookup = barcodeLookup,
                    onBarcodeChange = onBarcodeChange,
                    onLookupBarcode = onLookupBarcode,
                    onScanBarcode = onScanBarcode,
                    onScanLabel = onScanLabel,
                    onScanMeal = onScanMeal,
                    onManualEntry = onManualEntry
                )
                FoodLibraryPanel(
                    foodSearch = foodSearch,
                    onQueryChange = onFoodSearchQueryChange,
                    onSelectRecentFood = onSelectRecentFood,
                    onSelectFavoriteFood = onSelectFavoriteFood,
                    onSelectCustomFood = onSelectCustomFood
                )
            }
        }
    }
}

@Composable
private fun ScrollPane(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun CaptureAndCreatePanel(
    barcodeLookup: BarcodeLookupUiState,
    onBarcodeChange: (String) -> Unit,
    onLookupBarcode: () -> Unit,
    onScanBarcode: () -> Unit,
    onScanLabel: () -> Unit,
    onScanMeal: () -> Unit,
    onManualEntry: () -> Unit
) {
    MealScanHero(onClick = onScanMeal)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CaptureMethodTile(
            title = "Barcode",
            subtitle = "Point, scan, review",
            icon = Icons.Outlined.QrCodeScanner,
            onClick = onScanBarcode,
            modifier = Modifier.weight(1f).testTag("scan_barcode_method")
        )
        CaptureMethodTile(
            title = "Food label",
            subtitle = "Read nutrition values",
            icon = Icons.Outlined.DocumentScanner,
            onClick = onScanLabel,
            modifier = Modifier.weight(1f).testTag("scan_label_method")
        )
    }
    CaptureMethodTile(
        title = "Create manually",
        subtitle = "Build an entry with full control over portions and nutrition",
        icon = Icons.Filled.EditNote,
        onClick = onManualEntry,
        modifier = Modifier.fillMaxWidth().testTag("manual_food_method")
    )
    BarcodeEntryCard(
        state = barcodeLookup,
        onBarcodeChange = onBarcodeChange,
        onLookup = onLookupBarcode
    )
}

@Composable
private fun MealScanHero(onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 190.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .testTag("scan_meal_method")
    ) {
        Image(
            painter = painterResource(R.drawable.whale_coach),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 2.dp)
                .size(150.dp)
                .alpha(0.28f)
        )
        Column(
            modifier = Modifier.fillMaxWidth(0.82f).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Text(
                "WHALE-SMART MEAL SCAN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
            )
            Text(
                "One photo. A complete meal.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "Capture your plate, review every detected food, then log it together.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Scan meal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun BarcodeEntryCard(
    state: BarcodeLookupUiState,
    onBarcodeChange: (String) -> Unit,
    onLookup: () -> Unit
) {
    ModernCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text("Have the number?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Enter a barcode without opening the camera.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = state.barcode,
            onValueChange = onBarcodeChange,
            label = { Text("Barcode number") },
            leadingIcon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null) },
            trailingIcon = {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onLookup, enabled = state.barcode.isNotBlank()) {
                        Icon(Icons.Filled.Search, contentDescription = "Look up barcode")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onLookup() }),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().testTag("barcode_input")
        )
        state.statusMessage?.let { message ->
            InlineStatus(message = message, kind = state.status.toInlineStatusKind())
        }
    }
}

@Composable
private fun FoodLibraryPanel(
    foodSearch: FoodSearchSections,
    onQueryChange: (String) -> Unit,
    onSelectRecentFood: (FoodLogEntry) -> Unit,
    onSelectFavoriteFood: (FoodFavoritePreset) -> Unit,
    onSelectCustomFood: (CustomFood) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your food library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Reuse something you already logged, then adjust the portion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = foodSearch.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search recent, favorites, custom foods") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().testTag("food_library_search")
        )

        val queryActive = foodSearch.query.isNotBlank()
        val recent = if (queryActive) foodSearch.recent + foodSearch.searchResults else foodSearch.recent.take(5)
        val favorites = if (queryActive) foodSearch.favorites else foodSearch.favorites.take(4)
        val custom = if (queryActive) foodSearch.customFoods else foodSearch.customFoods.take(4)
        if (recent.isEmpty() && favorites.isEmpty() && custom.isEmpty()) {
            ModernCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    if (queryActive) "No matching foods" else "Your library is ready for its first food",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (queryActive) "Try a different name or create a manual entry."
                    else "Foods you log will appear here for quick reuse.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            if (favorites.isNotEmpty()) {
                FoodResultSection("Favorites", Icons.Outlined.Star) {
                    favorites.forEach { favorite ->
                        FoodChoiceRow(
                            name = favorite.name,
                            subtitle = favorite.brand ?: "Saved favorite",
                            grams = favorite.servingSizeGrams,
                            calories = favorite.nutritionPer100g.caloriesKcal * favorite.servingSizeGrams / 100.0,
                            onClick = { onSelectFavoriteFood(favorite) }
                        )
                    }
                }
            }
            if (recent.isNotEmpty()) {
                FoodResultSection(if (queryActive) "Matches" else "Recently logged", Icons.Outlined.History) {
                    recent.distinctBy { it.name.lowercase() to it.brand.orEmpty().lowercase() }.take(8).forEach { entry ->
                        FoodChoiceRow(
                            name = entry.name,
                            subtitle = entry.brand ?: entry.mealType.displayLabel(),
                            grams = entry.grams,
                            calories = entry.nutrition.caloriesKcal,
                            onClick = { onSelectRecentFood(entry) }
                        )
                    }
                }
            }
            if (custom.isNotEmpty()) {
                FoodResultSection("Custom foods", Icons.Filled.EditNote) {
                    custom.forEach { food ->
                        FoodChoiceRow(
                            name = food.name,
                            subtitle = food.brand ?: "Custom food",
                            grams = food.servingSizeGrams,
                            calories = food.nutritionPer100g.caloriesKcal * food.servingSizeGrams / 100.0,
                            onClick = { onSelectCustomFood(food) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodResultSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ModernCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun FoodChoiceRow(
    name: String,
    subtitle: String,
    grams: Double,
    calories: Double,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$subtitle · ${grams.formatPlain()} g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${calories.roundToIntSafe()} kcal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

private data class MacroValues(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

@Composable
private fun FoodEditorStage(
    name: String,
    onNameChange: (String) -> Unit,
    brand: String,
    onBrandChange: (String) -> Unit,
    grams: String,
    onGramsChange: (String) -> Unit,
    calories: String,
    onCaloriesChange: (String) -> Unit,
    protein: String,
    onProteinChange: (String) -> Unit,
    carbs: String,
    onCarbsChange: (String) -> Unit,
    fat: String,
    onFatChange: (String) -> Unit,
    sugar: String,
    onSugarChange: (String) -> Unit,
    saturatedFat: String,
    onSaturatedFatChange: (String) -> Unit,
    fiber: String,
    onFiberChange: (String) -> Unit,
    salt: String,
    onSaltChange: (String) -> Unit,
    sodium: String,
    onSodiumChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    showNutritionDetails: Boolean,
    onToggleNutritionDetails: () -> Unit,
    showNotes: Boolean,
    onToggleNotes: () -> Unit,
    autoScaleNutrition: Boolean,
    macroValues: MacroValues,
    barcodeLookup: BarcodeLookupUiState,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScrollPane(modifier = Modifier.weight(0.48f).fillMaxHeight()) {
                    barcodeLookup.statusMessage?.let {
                        InlineStatus(message = it, kind = barcodeLookup.status.toInlineStatusKind())
                    }
                    EditorEssentials(
                        name = name,
                        onNameChange = onNameChange,
                        grams = grams,
                        onGramsChange = onGramsChange
                    )
                }
                ScrollPane(modifier = Modifier.weight(0.52f).fillMaxHeight()) {
                    barcodeLookup.saveError?.let { InlineStatus(it, InlineStatusKind.Error) }
                    EditorNutrition(
                        grams = grams,
                        calories = calories,
                        onCaloriesChange = onCaloriesChange,
                        protein = protein,
                        onProteinChange = onProteinChange,
                        carbs = carbs,
                        onCarbsChange = onCarbsChange,
                        fat = fat,
                        onFatChange = onFatChange,
                        sugar = sugar,
                        onSugarChange = onSugarChange,
                        saturatedFat = saturatedFat,
                        onSaturatedFatChange = onSaturatedFatChange,
                        fiber = fiber,
                        onFiberChange = onFiberChange,
                        salt = salt,
                        onSaltChange = onSaltChange,
                        sodium = sodium,
                        onSodiumChange = onSodiumChange,
                        showNutritionDetails = showNutritionDetails,
                        onToggleNutritionDetails = onToggleNutritionDetails
                    )
                    EditorOptionalDetails(
                        brand = brand,
                        onBrandChange = onBrandChange,
                        notes = notes,
                        onNotesChange = onNotesChange,
                        expanded = showNotes,
                        onToggle = onToggleNotes
                    )
                    CompactLiveSummary(
                        macroValues = macroValues,
                        autoScaleNutrition = autoScaleNutrition,
                        compact = true
                    )
                }
            }
        } else {
            ScrollPane(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                barcodeLookup.statusMessage?.let {
                    InlineStatus(message = it, kind = barcodeLookup.status.toInlineStatusKind())
                }
                barcodeLookup.saveError?.let { InlineStatus(it, InlineStatusKind.Error) }
                EditorEssentials(
                    name = name,
                    onNameChange = onNameChange,
                    grams = grams,
                    onGramsChange = onGramsChange
                )
                EditorNutrition(
                    grams = grams,
                    calories = calories,
                    onCaloriesChange = onCaloriesChange,
                    protein = protein,
                    onProteinChange = onProteinChange,
                    carbs = carbs,
                    onCarbsChange = onCarbsChange,
                    fat = fat,
                    onFatChange = onFatChange,
                    sugar = sugar,
                    onSugarChange = onSugarChange,
                    saturatedFat = saturatedFat,
                    onSaturatedFatChange = onSaturatedFatChange,
                    fiber = fiber,
                    onFiberChange = onFiberChange,
                    salt = salt,
                    onSaltChange = onSaltChange,
                    sodium = sodium,
                    onSodiumChange = onSodiumChange,
                    showNutritionDetails = showNutritionDetails,
                    onToggleNutritionDetails = onToggleNutritionDetails
                )
                EditorOptionalDetails(
                    brand = brand,
                    onBrandChange = onBrandChange,
                    notes = notes,
                    onNotesChange = onNotesChange,
                    expanded = showNotes,
                    onToggle = onToggleNotes
                )
                CompactLiveSummary(
                    macroValues = macroValues,
                    autoScaleNutrition = autoScaleNutrition
                )
            }
        }
    }
}

@Composable
private fun CompactLiveSummary(
    macroValues: MacroValues,
    autoScaleNutrition: Boolean,
    compact: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (compact) 11.dp else 13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("ENTRY TOTAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${macroValues.calories.coerceAtLeast(0.0).roundToIntSafe()} kcal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactMacroValue("Protein", macroValues.protein)
                    CompactMacroValue("Carbs", macroValues.carbs)
                    CompactMacroValue("Fat", macroValues.fat)
                }
            }
            if (autoScaleNutrition) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Values scale with the portion until a nutrient is edited.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactMacroValue(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            maxLines = 2
        )
        Text("${value.coerceAtLeast(0.0).roundToIntSafe()} g", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditorEssentials(
    name: String,
    onNameChange: (String) -> Unit,
    grams: String,
    onGramsChange: (String) -> Unit
) {
    var nameTouched by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = "Food & portion", subtitle = "What are you logging right now?") {
        OutlinedTextField(
            value = name,
            onValueChange = {
                nameTouched = true
                onNameChange(it)
            },
            label = { Text("Food name *") },
            singleLine = true,
            isError = nameTouched && name.isBlank(),
            supportingText = if (nameTouched && name.isBlank()) ({ Text("Enter a food name") }) else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().testTag("food_name_input")
        )
        NumericField(
            value = grams,
            onValueChange = onGramsChange,
            label = "Portion *",
            suffix = "g",
            requiredPositive = true,
            modifier = Modifier.testTag("food_grams_input")
        )
    }
}

@Composable
private fun EditorNutrition(
    grams: String,
    calories: String,
    onCaloriesChange: (String) -> Unit,
    protein: String,
    onProteinChange: (String) -> Unit,
    carbs: String,
    onCarbsChange: (String) -> Unit,
    fat: String,
    onFatChange: (String) -> Unit,
    sugar: String,
    onSugarChange: (String) -> Unit,
    saturatedFat: String,
    onSaturatedFatChange: (String) -> Unit,
    fiber: String,
    onFiberChange: (String) -> Unit,
    salt: String,
    onSaltChange: (String) -> Unit,
    sodium: String,
    onSodiumChange: (String) -> Unit,
    showNutritionDetails: Boolean,
    onToggleNutritionDetails: () -> Unit
) {
    SectionCard(
        title = "Nutrition",
        subtitle = "Totals for ${grams.ifBlank { "this portion" }}${if (grams.isBlank()) "" else " g"}."
    ) {
        ResponsiveFieldPair(
            first = { NumericField(calories, onCaloriesChange, "Energy", suffix = "kcal") },
            second = { NumericField(protein, onProteinChange, "Protein", suffix = "g") }
        )
        ResponsiveFieldPair(
            first = { NumericField(carbs, onCarbsChange, "Carbs", suffix = "g") },
            second = { NumericField(fat, onFatChange, "Fat", suffix = "g") }
        )
        Surface(
            onClick = onToggleNutritionDetails,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("More nutrients", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Sugar, saturated fat, fiber, salt and sodium",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    if (showNutritionDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (showNutritionDetails) "Hide more nutrients" else "Show more nutrients"
                )
            }
        }
        AnimatedVisibility(visible = showNutritionDetails) {
            Column(
                modifier = Modifier.padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResponsiveFieldPair(
                    first = { NumericField(sugar, onSugarChange, "Sugar", suffix = "g") },
                    second = { NumericField(saturatedFat, onSaturatedFatChange, "Saturated fat", suffix = "g") }
                )
                ResponsiveFieldPair(
                    first = { NumericField(fiber, onFiberChange, "Fiber", suffix = "g") },
                    second = { NumericField(salt, onSaltChange, "Salt", suffix = "g") }
                )
                NumericField(sodium, onSodiumChange, "Sodium", suffix = "mg")
            }
        }
    }
}

@Composable
private fun EditorOptionalDetails(
    brand: String,
    onBrandChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    ModernCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Surface(
            onClick = onToggle,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Brand & note", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (expanded) "Optional details are open" else "Add optional context",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Hide brand and note" else "Show brand and note"
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = onBrandChange,
                    label = { Text("Brand (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Note (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ResponsiveFieldPair(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 280.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                first()
                second()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { first() }
                Box(Modifier.weight(1f)) { second() }
            }
        }
    }
}

@Composable
private fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    requiredPositive: Boolean = false
) {
    val parsed = value.toUserDecimalOrNull()
    val invalid = if (requiredPositive) {
        value.isNotBlank() && parsed?.let { it <= 0.0 } != false
    } else {
        value.isNotBlank() && parsed?.let { it < 0.0 } != false
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        isError = invalid,
        supportingText = if (invalid) {
            { Text(if (requiredPositive) "Enter an amount above zero" else "Enter zero or a positive number") }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    )
}

private fun BarcodeLookupStatus.toInlineStatusKind(): InlineStatusKind = when (this) {
    BarcodeLookupStatus.Idle, BarcodeLookupStatus.Loading -> InlineStatusKind.Info
    BarcodeLookupStatus.Success -> InlineStatusKind.Success
    BarcodeLookupStatus.Error -> InlineStatusKind.Error
}

private fun String.scaledBy(factor: Double): String {
    val value = toUserDecimalOrNull() ?: return this
    return (value * factor).formatPlain()
}

private fun Micronutrients.scaledBy(factor: Double): Micronutrients = Micronutrients(
    values = values.mapValues { (_, value) -> value.copy(amount = value.amount * factor) }
)

private val MicronutrientsSaver = listSaver<Micronutrients, String>(
    save = { micronutrients ->
        micronutrients.values.map { (key, value) ->
            listOf(
                key.name,
                value.amount.toString(),
                value.dataQuality.name,
                URLEncoder.encode(value.source.orEmpty(), StandardCharsets.UTF_8.name())
            ).joinToString("\t")
        }
    },
    restore = { saved ->
        Micronutrients(
            values = saved.mapNotNull { encoded ->
                val parts = encoded.split('\t', limit = 4)
                val key = parts.getOrNull(0)?.let { value ->
                    NutrientKey.entries.firstOrNull { it.name == value }
                }
                val amount = parts.getOrNull(1)?.toDoubleOrNull()
                val quality = parts.getOrNull(2)?.let { value ->
                    DataQuality.entries.firstOrNull { it.name == value }
                }
                if (key == null || amount == null || quality == null) {
                    null
                } else {
                    key to MicronutrientValue(
                        amount = amount,
                        dataQuality = quality,
                        source = parts.getOrNull(3)
                            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                            ?.ifBlank { null }
                    )
                }
            }.toMap()
        )
    }
)

private fun Double.formatPlain(): String {
    val rounded = round(this * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

private fun Double.roundToIntSafe(): Int = if (isFinite()) roundToInt() else 0
