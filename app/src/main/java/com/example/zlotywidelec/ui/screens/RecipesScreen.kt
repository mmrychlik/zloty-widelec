package com.example.zlotywidelec.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.zlotywidelec.data.local.dao.IngredientNameAndUnit
import com.example.zlotywidelec.data.local.dao.RecipeWithIngredients
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.data.local.entity.RecipeIngredientEntity
import com.example.zlotywidelec.ui.viewmodel.RecipeSortOrder
import com.example.zlotywidelec.ui.viewmodel.RecipeViewModel

// Main recipes list screen
@Composable
fun RecipesScreen(
    viewModel: RecipeViewModel,
    onAddToShoppingList: (List<RecipeIngredientEntity>) -> Unit,
    onUseFromFridge: (List<RecipeIngredientEntity>) -> Unit
) {
    val recipes by viewModel.allFilteredRecipes.collectAsState()
    val availability by viewModel.recipeAvailability.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val filterTags by viewModel.filterTags.collectAsState()
    val filterOwner by viewModel.filterOwner.collectAsState()
    val availableOwners by viewModel.availableOwners.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var recipeToEdit by remember { mutableStateOf<RecipeWithIngredients?>(null) }
    var recipeToDelete by remember { mutableStateOf<RecipeEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showOwnerMenu by remember { mutableStateOf(false) }

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

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showOwnerMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Znajomi", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showOwnerMenu,
                        onDismissRequest = { showOwnerMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.widthIn(min = 160.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Wszystkie") },
                            onClick = {
                                viewModel.setFilterOwner(null)
                                showOwnerMenu = false
                            },
                            modifier = Modifier.background(if (filterOwner == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                        )
                        DropdownMenuItem(
                            text = { Text("Własne") },
                            onClick = {
                                viewModel.setFilterOwner("WŁASNE")
                                showOwnerMenu = false
                            },
                            modifier = Modifier.background(if (filterOwner == "WŁASNE") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                        )
                        availableOwners.forEach { owner ->
                            DropdownMenuItem(
                                text = { Text(owner) },
                                onClick = {
                                    viewModel.setFilterOwner(owner)
                                    showOwnerMenu = false
                                },
                                modifier = Modifier.background(if (filterOwner == owner) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                            )
                        }
                    }
                }
            }

            if (recipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Brak przepisów", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 18.sp)
                        Text(text = "Dodaj własne lub zsynchronizuj od znajomych", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
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
                            getFridgeAmount = { name, unit -> viewModel.getFridgeAmountForIngredient(name, unit) },
                            onDelete = { recipeToDelete = recipe.recipe },
                            onEdit = { recipeToEdit = recipe },
                            onAddToShoppingList = { onAddToShoppingList(recipe.ingredients) },
                            onUseFromFridge = { onUseFromFridge(recipe.ingredients) }
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
            onConfirm = { name, instructions, imageUrl, videoUrl, tag, ingredients ->
                viewModel.addRecipe(name, instructions, imageUrl, videoUrl, tag, ingredients)
            }
        )
    }

    recipeToDelete?.let { recipe ->
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = { Text("Usuń przepis") },
            text = { Text("Czy na pewno chcesz usunąć przepis \"${recipe.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecipe(recipe)
                        recipeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    recipeToEdit?.let { rwI ->
        AddRecipeDialog(
            initialRecipe = rwI,
            suggestions = suggestions,
            onDismiss = { recipeToEdit = null },
            onConfirm = { name, instructions, imageUrl, videoUrl, tag, ingredients ->
                viewModel.updateRecipe(
                    rwI.recipe.copy(
                        name = name,
                        instructions = instructions,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl,
                        tag = tag
                    ),
                    ingredients
                )
            }
        )
    }
}

// Dialog for adding or editing a recipe
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeDialog(
    initialRecipe: RecipeWithIngredients? = null,
    suggestions: List<IngredientNameAndUnit> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, List<Triple<String, Double, String>>) -> Unit
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
    var videoUri by remember {
        mutableStateOf(initialRecipe?.recipe?.videoUrl?.let {
            if (it.startsWith("content://")) Uri.parse(it) else null
        })
    }
    var remoteVideoUrl by remember {
        mutableStateOf(initialRecipe?.recipe?.videoUrl?.let {
            if (!it.startsWith("content://")) it else ""
        } ?: "")
    }
    var selectedTag by remember { mutableStateOf(initialRecipe?.recipe?.tag ?: "") }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                imageUri = uri
            }
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                videoUri = uri
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (videoUri != null || remoteVideoUrl.isNotEmpty()) "Wideo wybrane" else "Dodaj wideo")
                    }
                }

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
                                            onValueChange = { newValue ->
                                                val capitalized = newValue.replaceFirstChar { 
                                                    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
                                                }
                                                ingredients[index] = Triple(capitalized, iAmount, iUnit)
                                                expandedNameSuggestions = capitalized.length >= 2 && filteredNameSuggestions.isNotEmpty()
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
                                            Icons.Default.Close,
                                            contentDescription = "Usuń",
                                            tint = Color.Red.copy(alpha = 0.7f),
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
                    val finalVideoUrl = videoUri?.toString() ?: remoteVideoUrl
                    onConfirm(name, instructions, finalImageUrl, finalVideoUrl, selectedTag, finalIngredients)
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

// Individual recipe card item
@Composable
fun RecipeItem(
    recipe: RecipeWithIngredients,
    availabilityPercent: Int,
    getFridgeAmount: (String, String) -> Double,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onAddToShoppingList: () -> Unit,
    onUseFromFridge: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showVideoPlayer by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (recipe.recipe.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = recipe.recipe.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        availabilityPercent >= 100 -> Color(0xFF4CAF50)
                        availabilityPercent > 0 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                ) {
                    Text(
                        text = "$availabilityPercent%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!recipe.recipe.isUserCreated && recipe.recipe.ownerName.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = recipe.recipe.ownerName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipe.recipe.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (recipe.recipe.tag.isNotEmpty()) {
                            Text(
                                text = recipe.recipe.tag.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (recipe.recipe.isUserCreated) {
                        Row {
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Default.Edit, contentDescription = "Edytuj", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Close, contentDescription = "Usuń", tint = Color.Red, modifier = Modifier.size(25.dp))
                            }
                        }
                    }
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "SKŁADNIKI",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    
                    recipe.ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ingredient.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            
                            val fridgeAmount = getFridgeAmount(ingredient.name, ingredient.unit)
                            val fridgeAmountFormatted = if (fridgeAmount % 1.0 == 0.0) fridgeAmount.toInt().toString() else "%.2f".format(fridgeAmount)
                            val reqAmountFormatted = if (ingredient.amount % 1.0 == 0.0) ingredient.amount.toInt().toString() else "%.2f".format(ingredient.amount)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$reqAmountFormatted ${ingredient.unit}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("|", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Kitchen,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(": $fridgeAmountFormatted ${ingredient.unit}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "PRZYGOTOWANIE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        recipe.recipe.instructions,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (recipe.recipe.videoUrl.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showVideoPlayer = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Zobacz wideo", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (availabilityPercent > 0) {
                        Button(
                            onClick = onUseFromFridge,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Kitchen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Zużyj składniki z lodówki", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = onAddToShoppingList,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dodaj brakujące do listy", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showVideoPlayer) {
        VideoPlayerDialog(
            videoUrl = recipe.recipe.videoUrl,
            onDismiss = { showVideoPlayer = false }
        )
    }
}

// Dialog for playing recipe video
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialog(videoUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = !isFullscreen,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .aspectRatio(16/9f)
            },
            color = Color.Black,
            shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = Color.White)
                }

                IconButton(
                    onClick = { isFullscreen = !isFullscreen },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Pełny ekran",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
