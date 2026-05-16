package com.example.zlotywidelec.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.zlotywidelec.data.local.dao.IngredientNameAndUnit
import com.example.zlotywidelec.data.local.dao.RecipeWithIngredients
import com.example.zlotywidelec.ui.viewmodel.RecipeSortOrder
import com.example.zlotywidelec.ui.viewmodel.RecipeViewModel

@Composable
fun RecipesScreen(viewModel: RecipeViewModel) {
    val recipes by viewModel.filteredUserRecipes.collectAsState()
    val availability by viewModel.recipeAvailability.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val filterTags by viewModel.filterTags.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var recipeToEdit by remember { mutableStateOf<RecipeWithIngredients?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val recipeTagOptions = listOf(
        "obiad" to "Obiad",
        "śniadanie" to "Śniadanie",
        "kolacja" to "Kolacja",
        "deser" to "Deser",
        "przekąska" to "Przekąska",
        "sałatka" to "Sałatka",
        "zupa" to "Zupa"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj przepis")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
             Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Sort Box
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Sortowanie", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.widthIn(min = 160.dp)
                    ) {
                        listOf("DATE" to "Data", "NAME" to "Nazwa", "AVAILABILITY" to "Dostępność").forEach { (type, label) ->
                             val isSelected = when(type) {
                                "NAME" -> sortOrder == RecipeSortOrder.NAME_ASC || sortOrder == RecipeSortOrder.NAME_DESC
                                "DATE" -> sortOrder == RecipeSortOrder.DATE_ASC || sortOrder == RecipeSortOrder.DATE_DESC
                                "AVAILABILITY" -> sortOrder == RecipeSortOrder.AVAILABILITY_ASC || sortOrder == RecipeSortOrder.AVAILABILITY_DESC
                                else -> false
                            }
                            val arrow = when(type) {
                                "DATE" -> when(sortOrder) {
                                    RecipeSortOrder.DATE_ASC -> " ↗"
                                    RecipeSortOrder.DATE_DESC -> " ↘"
                                    else -> ""
                                }
                                "NAME" -> when(sortOrder) {
                                    RecipeSortOrder.NAME_ASC -> " ↗"
                                    RecipeSortOrder.NAME_DESC -> " ↘"
                                    else -> ""
                                }
                                "AVAILABILITY" -> when(sortOrder) {
                                    RecipeSortOrder.AVAILABILITY_ASC -> " ↗"
                                    RecipeSortOrder.AVAILABILITY_DESC -> " ↘"
                                    else -> ""
                                }
                                else -> ""
                            }
                            DropdownMenuItem(
                                text = { Text(label + arrow) },
                                onClick = {
                                    viewModel.toggleSortOrder(type)
                                },
                                modifier = Modifier.background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Filter Box
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFilterMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Filtrowanie", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.widthIn(min = 160.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Box(modifier = Modifier.requiredHeightIn(max = 380.dp)) {
                            Column(modifier = Modifier.verticalScroll(scrollState)) {
                                DropdownMenuItem(
                                    text = { Text("Wszystkie") },
                                    onClick = { viewModel.toggleFilterTag("") },
                                    modifier = Modifier.background(if (filterTags.isEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                )
                                recipeTagOptions.forEach { (tag, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { viewModel.toggleFilterTag(tag) },
                                        modifier = Modifier.background(if (filterTags.contains(tag)) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                    )
                                }
                            }
                            if (scrollState.canScrollBackward) {
                                Text(
                                    "^",
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (scrollState.canScrollForward) {
                                Text(
                                    "^",
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .rotate(180f),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (recipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Brak własnych przepisów", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 18.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recipes, key = { it.recipe.id }) { recipe ->
                        val percent = availability[recipe.recipe.id.toString()] ?: 0
                        RecipeItem(
                            recipe = recipe, 
                            availabilityPercent = percent,
                            onDelete = { viewModel.deleteRecipe(recipe.recipe) },
                            onEdit = { recipeToEdit = recipe }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecipeDialog(
            suggestions = suggestions,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, instructions, imageUrl, tag, ingredients ->
                viewModel.addRecipe(name, instructions, imageUrl, tag, ingredients)
            }
        )
    }

    recipeToEdit?.let { rwI ->
        AddRecipeDialog(
            initialRecipe = rwI,
            suggestions = suggestions,
            onDismiss = { recipeToEdit = null },
            onConfirm = { name, instructions, imageUrl, tag, ingredients ->
                viewModel.updateRecipe(
                    rwI.recipe.copy(
                        name = name,
                        instructions = instructions,
                        imageUrl = imageUrl,
                        tag = tag
                    ),
                    ingredients
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeDialog(
    initialRecipe: RecipeWithIngredients? = null,
    suggestions: List<IngredientNameAndUnit> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, List<Triple<String, Double, String>>) -> Unit
) {
    var name by remember { mutableStateOf(initialRecipe?.recipe?.name ?: "") }
    var instructions by remember { mutableStateOf(initialRecipe?.recipe?.instructions ?: "") }
    var imageUri by remember {
        mutableStateOf(initialRecipe?.recipe?.imageUrl?.let {
            if (it.startsWith("content://")) Uri.parse(it) else null
        })
    }
    var remoteImageUrl by remember {
        mutableStateOf(initialRecipe?.recipe?.imageUrl?.let {
            if (!it.startsWith("content://")) it else ""
        } ?: "")
    }
    var selectedTag by remember { mutableStateOf(initialRecipe?.recipe?.tag ?: "") }

    val context = LocalContext.current
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                }
                imageUri = uri
            }
        }
    )

    val recipeTagOptions = listOf(
        "obiad" to "Obiad",
        "śniadanie" to "Śniadanie",
        "kolacja" to "Kolacja",
        "deser" to "Deser",
        "przekąska" to "Przekąska",
        "sałatka" to "Sałatka",
        "zupa" to "Zupa"
    )

    val ingredients = remember {
        mutableStateListOf<Triple<String, String, String>>().apply {
            if (initialRecipe != null) {
                addAll(initialRecipe.ingredients.map { Triple(it.name, it.amount.toString(), it.unit) })
            }
            if (isEmpty()) add(Triple("", "", ""))
        }
    }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    val unitSuggestions = remember(suggestions) {
        suggestions.map { it.unit }.distinct().filter { it.isNotBlank() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRecipe == null) "Dodaj własny przepis" else "Edytuj przepis", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name field
                Column {
                    Text(
                        text = "Nazwa przepisu",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors
                    )
                }

                // Image Picker
                Column {
                    Text(
                        text = "Zdjęcie",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null || remoteImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = imageUri ?: remoteImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                                Text(
                                    "Wybierz zdjęcie",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }

                // Category selection
                Column {
                    Text(
                        text = "Kategoria",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(recipeTagOptions) { (tag, label) ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = if (selectedTag == tag) "" else tag },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }

                // Ingredients list
                Column {
                    Text(
                        text = "Składniki",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    ingredients.forEachIndexed { index, (iName, iAmount, iUnit) ->
                        var expandedNameSuggestions by remember { mutableStateOf(false) }
                        var expandedUnitSuggestions by remember { mutableStateOf(false) }
                        
                        fun String.normalize(): String {
                            val temp = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
                            return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(temp, "").lowercase()
                        }

                        val filteredNameSuggestions = remember(iName) {
                            if (iName.length >= 2) {
                                val normalizedSearch = iName.normalize()
                                suggestions.filter { it.name.normalize().contains(normalizedSearch) }
                            } else {
                                emptyList()
                            }
                        }

                        val filteredUnitSuggestions = remember(iUnit) {
                            if (iUnit.length >= 1) {
                                val normalizedSearch = iUnit.normalize()
                                unitSuggestions.filter { it.normalize().contains(normalizedSearch) }
                            } else {
                                emptyList()
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ExposedDropdownMenuBox(
                                        expanded = expandedNameSuggestions,
                                        onExpandedChange = { expandedNameSuggestions = it }
                                    ) {
                                        TextField(
                                            value = iName,
                                            onValueChange = {
                                                ingredients[index] = Triple(it, iAmount, iUnit)
                                                expandedNameSuggestions = it.length >= 2 && filteredNameSuggestions.isNotEmpty()
                                            },
                                            placeholder = { Text("Nazwa", fontSize = 14.sp) },
                                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                                            colors = textFieldColors
                                        )

                                        if (filteredNameSuggestions.isNotEmpty()) {
                                            ExposedDropdownMenu(
                                                expanded = expandedNameSuggestions,
                                                onDismissRequest = { expandedNameSuggestions = false },
                                                modifier = Modifier.fillMaxWidth(),
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ) {
                                                filteredNameSuggestions.take(5).forEach { suggestion ->
                                                    DropdownMenuItem(
                                                        text = { Text(suggestion.name) },
                                                        onClick = {
                                                            ingredients[index] = Triple(suggestion.name, iAmount, suggestion.unit)
                                                            expandedNameSuggestions = false
                                                        },
                                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (ingredients.size > 1) {
                                    IconButton(onClick = { ingredients.removeAt(index) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Usuń",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = iAmount,
                                    onValueChange = { ingredients[index] = Triple(iName, it, iUnit) },
                                    placeholder = { Text("Ilość", fontSize = 14.sp) },
                                    modifier = Modifier.width(100.dp),
                                    colors = textFieldColors,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                    )
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    ExposedDropdownMenuBox(
                                        expanded = expandedUnitSuggestions,
                                        onExpandedChange = { expandedUnitSuggestions = it }
                                    ) {
                                        TextField(
                                            value = iUnit,
                                            onValueChange = {
                                                ingredients[index] = Triple(iName, iAmount, it)
                                                expandedUnitSuggestions = it.isNotEmpty() && filteredUnitSuggestions.isNotEmpty()
                                            },
                                            placeholder = { Text("Jedn.", fontSize = 14.sp) },
                                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                                            colors = textFieldColors
                                        )

                                        if (filteredUnitSuggestions.isNotEmpty()) {
                                            ExposedDropdownMenu(
                                                expanded = expandedUnitSuggestions,
                                                onDismissRequest = { expandedUnitSuggestions = false },
                                                modifier = Modifier.fillMaxWidth(),
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ) {
                                                filteredUnitSuggestions.take(5).forEach { suggestion ->
                                                    DropdownMenuItem(
                                                        text = { Text(suggestion) },
                                                        onClick = {
                                                            ingredients[index] = Triple(iName, iAmount, suggestion)
                                                            expandedUnitSuggestions = false
                                                        },
                                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { ingredients.add(Triple("", "", "")) },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dodaj składnik", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Instructions field
                Column {
                    Text(
                        text = "Instrukcja przygotowania",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    TextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        colors = textFieldColors,
                        singleLine = false
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalIngredients = ingredients
                        .filter { it.first.isNotBlank() }
                        .map { Triple(it.first, it.second.replace(",", ".").toDoubleOrNull() ?: 1.0, it.third) }
                    val finalImageUrl = imageUri?.toString() ?: remoteImageUrl
                    onConfirm(name, instructions, finalImageUrl, selectedTag, finalIngredients)
                    onDismiss()
                },
                enabled = name.isNotBlank() && instructions.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(if (initialRecipe == null) "Dodaj przepis" else "Zapisz zmiany")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
