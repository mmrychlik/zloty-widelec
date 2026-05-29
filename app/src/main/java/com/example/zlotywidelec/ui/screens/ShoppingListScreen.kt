package com.example.zlotywidelec.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zlotywidelec.data.local.dao.IngredientNameAndUnit
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.ui.viewmodel.ShoppingSortOrder
import com.example.zlotywidelec.ui.viewmodel.ShoppingViewModel
import java.text.Normalizer

@Composable
fun ShoppingListScreen(viewModel: ShoppingViewModel) {
    val items by viewModel.shoppingItems.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val filterTags by viewModel.filterTags.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<IngredientEntity?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var filterHeaderWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val tagOptions = listOf(
        "warzywa" to "Warzywa",
        "owoce" to "Owoce",
        "pieczywo" to "Pieczywo",
        "zbożowe" to "Zbożowe",
        "mięso" to "Mięso",
        "nabiał" to "Nabiał",
        "ryby" to "Ryby",
        "mrożonki" to "Mrożonki",
        "napoje" to "Napoje",
        "słodycze" to "Słodycze",
        "przyprawy" to "Przyprawy",
        "sosy" to "Sosy"
    )

    var showSortMenu by remember { mutableStateOf(false) }
    var sortHeaderWidth by remember { mutableIntStateOf(0) }
    val hasCheckedItems = items.any { it.isChecked }

    // Shopping list main layout
    Scaffold(
        floatingActionButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = hasCheckedItems,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.moveCheckedToFridge() },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            icon = { Icon(Icons.Default.Kitchen, contentDescription = "Dodaj do lodówki") },
                            text = { Text("Do lodówki") }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ExtendedFloatingActionButton(
                            onClick = { 
                                viewModel.deleteCheckedItems()
                                android.media.MediaPlayer.create(context, com.example.zlotywidelec.R.raw.dragon_studio_cash_register_kaching_376867)?.apply {
                                    setOnCompletionListener { release() }
                                    start()
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Wyczyść zaznaczone") },
                            text = { Text("Kupione") }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }

                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Sort and filter headers
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
                            .onSizeChanged { sortHeaderWidth = it.width }
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Sortowanie",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(with(density) { sortHeaderWidth.toDp() })
                    ) {
                        listOf(
                            "DATE" to "Data",
                            "NAME" to "Nazwa",
                            "CATEGORY" to "Kategoria"
                        ).forEach { (type, label) ->
                            val isSelected = sortOrder.name.startsWith(type)
                            val arrow = when(type) {
                                "DATE" -> when(sortOrder) {
                                    ShoppingSortOrder.DATE_ASC -> " ↗"
                                    ShoppingSortOrder.DATE_DESC -> " ↘"
                                    else -> ""
                                }
                                "NAME" -> when(sortOrder) {
                                    ShoppingSortOrder.NAME_ASC -> " ↗"
                                    ShoppingSortOrder.NAME_DESC -> " ↘"
                                    else -> ""
                                }
                                "CATEGORY" -> when(sortOrder) {
                                    ShoppingSortOrder.CATEGORY_ASC -> " ↗"
                                    ShoppingSortOrder.CATEGORY_DESC -> " ↘"
                                    else -> ""
                                }
                                else -> ""
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(label + arrow)
                                },
                                onClick = {
                                    viewModel.toggleSortOrder(type)
                                },
                                modifier = Modifier.background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .onSizeChanged { filterHeaderWidth = it.width }
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFilterMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Filtrowanie",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(with(density) { filterHeaderWidth.toDp() })
                    ) {
                        val scrollState = rememberScrollState()
                        Box(modifier = Modifier.requiredHeightIn(max = 380.dp)) {
                            Column(modifier = Modifier.verticalScroll(scrollState)) {
                                val isAllSelected = filterTags.isEmpty()
                                DropdownMenuItem(
                                    text = {
                                        Text("Wszystkie")
                                    },
                                    onClick = {
                                        viewModel.toggleFilterTag("")
                                    },
                                    modifier = Modifier.background(
                                        if (isAllSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else Color.Transparent
                                    )
                                )
                                tagOptions.forEach { (tagValue, label) ->
                                    val isSelected = filterTags.contains(tagValue)
                                    DropdownMenuItem(
                                        text = {
                                            Text(label)
                                        },
                                        onClick = {
                                            viewModel.toggleFilterTag(tagValue)
                                        },
                                        modifier = Modifier.background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            else Color.Transparent
                                        )
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

            if (items.isEmpty()) {
                EmptyShoppingListMessage()
            } else {
                // Shopping items list
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 16.dp)
                        .weight(1f, fill = false),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    shape = RectangleShape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        items.forEach { item ->
                            key(item.id) {
                                ShoppingListItem(
                                    item = item,
                                    onCheckedChange = { viewModel.toggleItemChecked(item) },
                                    onDelete = { viewModel.deleteItem(item) },
                                    onLongClick = { itemToEdit = item }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            suggestions = suggestions,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, unit, tag ->
                viewModel.addItem(name, amount, unit, tag)
            }
        )
    }

    itemToEdit?.let { item ->
        AddItemDialog(
            initialItem = item,
            suggestions = suggestions,
            onDismiss = { itemToEdit = null },
            onConfirm = { name, amount, unit, tag ->
                viewModel.updateItem(item, name, amount, unit, tag)
            }
        )
    }
}

@Composable
fun EmptyShoppingListMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Twoja lista zakupów jest pusta",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 18.sp
            )
        }
    }
}

// Single shopping list item row
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingListItem(
    item: IngredientEntity,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onCheckedChange(!item.isChecked) },
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.FiberManualRecord,
                contentDescription = "Usuń",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(12.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = item.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (item.isChecked) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
            )
            if (item.tag.isNotEmpty()) {
                Text(
                    text = item.tag,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = if (item.isChecked) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            if (item.amount > 0) {
                Text(
                    text = "${if (item.amount % 1.0 == 0.0) item.amount.toInt() else item.amount} ${item.unit}",
                    fontSize = 14.sp,
                    color = if (item.isChecked) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(if (item.isChecked) MaterialTheme.colorScheme.secondary else Color.Transparent)
                .clickable { onCheckedChange(!item.isChecked) },
            contentAlignment = Alignment.Center
        ) {
            if (item.isChecked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }
}

// Dialog for adding or editing shopping items
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    initialItem: IngredientEntity? = null,
    suggestions: List<IngredientNameAndUnit>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var amountStr by remember { mutableStateOf(initialItem?.let { if (it.amount > 0) (if (it.amount % 1.0 == 0.0) it.amount.toInt().toString() else it.amount.toString()) else "" } ?: "") }

    val tagOptions = listOf(
        "warzywa" to "Warzywa",
        "owoce" to "Owoce",
        "pieczywo" to "Pieczywo",
        "zbożowe" to "Zbożowe",
        "mięso" to "Mięso",
        "nabiał" to "Nabiał",
        "ryby" to "Ryby",
        "mrożonki" to "Mrożonki",
        "napoje" to "Napoje",
        "słodycze" to "Słodycze",
        "przyprawy" to "Przyprawy",
        "sosy" to "Sosy"
    )
    var selectedTag by remember { mutableStateOf(initialItem?.tag ?: "") }

    val prefixOptions = listOf(
        "" to "—",
        "m" to "mili",
        "da" to "deka",
        "k" to "kilo"
    )
    val baseUnitOptions = listOf(
        "szt." to "sztuki",
        "g" to "gramy",
        "l" to "litry",
        "Inne" to "Inne"
    )

    var selectedPrefix by remember {
        mutableStateOf(initialItem?.let { item ->
            val unit = item.unit
            prefixOptions.filter { it.first.isNotEmpty() }.find { unit.startsWith(it.first) && it.first.length < unit.length }?.first
        } ?: prefixOptions[0].first)
    }
    var selectedBaseUnit by remember {
        mutableStateOf(initialItem?.let { item ->
            val unit = item.unit
            val prefixMatch = prefixOptions.filter { it.first.isNotEmpty() }.find { unit.startsWith(it.first) && it.first.length < unit.length }
            baseUnitOptions.find { it.first == unit || (prefixMatch != null && unit.endsWith(it.first)) }?.first
        } ?: "szt.")
    }
    var customUnit by remember {
        mutableStateOf(initialItem?.let { item ->
            val unit = item.unit
            val prefixMatch = prefixOptions.filter { it.first.isNotEmpty() }.find { unit.startsWith(it.first) && it.first.length < unit.length }
            val baseMatch = baseUnitOptions.find { it.first == unit || (prefixMatch != null && unit.endsWith(it.first)) }
            if (baseMatch == null) unit else ""
        } ?: "")
    }

    var expandedNameSuggestions by remember { mutableStateOf(false) }
    var expandedPrefixDropdown by remember { mutableStateOf(false) }
    var expandedBaseUnitDropdown by remember { mutableStateOf(false) }
    var expandedCustomUnitSuggestions by remember { mutableStateOf(false) }

    fun String.normalize(): String {
        val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(temp, "").lowercase()
    }

    val filteredSuggestions = remember(name) {
        if (name.length >= 2) {
            val normalizedSearch = name.normalize()
            suggestions.filter { it.name.normalize().contains(normalizedSearch) }
        } else {
            emptyList()
        }
    }

    val customUnitSuggestions = remember(suggestions) {
        suggestions.map { it.unit }.filter { unit ->
            val isStandard = baseUnitOptions.any {
                it.first != "Inne" && (unit == it.first || prefixOptions.any { p -> p.first.isNotEmpty() && unit == p.first + it.first })
            }
            !isStandard
        }.distinct().filter { it.isNotBlank() }
    }

    val filteredCustomUnitSuggestions = remember(customUnit, customUnitSuggestions) {
        if (customUnit.length >= 2) {
            val normalizedSearch = customUnit.normalize()
            customUnitSuggestions.filter { it.normalize().contains(normalizedSearch) }
        } else {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "Dodaj do listy" else "Edytuj produkt", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Name selection
                Column {
                    Text(
                        text = "Nazwa produktu",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedNameSuggestions,
                        onExpandedChange = { expandedNameSuggestions = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = name,
                            onValueChange = {
                                name = it
                                expandedNameSuggestions = it.length >= 2 && filteredSuggestions.isNotEmpty()
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        if (filteredSuggestions.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = expandedNameSuggestions,
                                onDismissRequest = { expandedNameSuggestions = false },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                filteredSuggestions.take(5).forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(suggestion.name, style = MaterialTheme.typography.bodyLarge)
                                                Text(suggestion.unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                        },
                                        onClick = {
                                            name = suggestion.name
                                            val unit = suggestion.unit
                                            selectedTag = suggestion.tag

                                            val prefixMatch = prefixOptions.filter { it.first.isNotEmpty() }.find { unit.startsWith(it.first) && it.first.length < unit.length }
                                            val baseMatch = baseUnitOptions.find { it.first == unit || (prefixMatch != null && unit.endsWith(it.first)) }

                                            if (baseMatch != null) {
                                                selectedBaseUnit = baseMatch.first
                                                selectedPrefix = prefixMatch?.first ?: ""
                                            } else {
                                                selectedBaseUnit = "Inne"
                                                customUnit = unit
                                            }

                                            expandedNameSuggestions = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }
                }

                // Tag Selection (Chips)
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
                    items(tagOptions) { (tagValue, label) ->
                        FilterChip(
                            selected = selectedTag == tagValue,
                            onClick = {
                                selectedTag = if (selectedTag == tagValue) "" else tagValue
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                // Amount input
                Column {
                    Text(
                        text = "Ilość",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    TextField(
                        value = amountStr,
                        onValueChange = { if (it.isEmpty() || it.replace(",", ".").toDoubleOrNull() != null || it.endsWith(".") || it.endsWith(",")) amountStr = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Prefix and Base Unit selection
                val showPrefix = selectedBaseUnit == "g" || selectedBaseUnit == "l"
                val filteredPrefixOptions = remember(selectedBaseUnit) {
                    if (selectedBaseUnit == "l") {
                        prefixOptions.filter { it.first == "" || it.first == "m" }
                    } else {
                        prefixOptions
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showPrefix) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Przedr.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            ExposedDropdownMenuBox(
                                expanded = expandedPrefixDropdown,
                                onExpandedChange = { expandedPrefixDropdown = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextField(
                                    value = prefixOptions.find { it.first == selectedPrefix }?.second ?: "—",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPrefixDropdown) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedPrefixDropdown,
                                    onDismissRequest = { expandedPrefixDropdown = false },
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    filteredPrefixOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.second) },
                                            onClick = {
                                                selectedPrefix = option.first
                                                expandedPrefixDropdown = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jedn.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedBaseUnitDropdown,
                            onExpandedChange = { expandedBaseUnitDropdown = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = baseUnitOptions.find { it.first == selectedBaseUnit }?.second ?: selectedBaseUnit,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBaseUnitDropdown) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedBaseUnitDropdown,
                                onDismissRequest = { expandedBaseUnitDropdown = false },
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                baseUnitOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.second) },
                                        onClick = {
                                            selectedBaseUnit = option.first
                                            if (option.first != "g" && option.first != "l") {
                                                selectedPrefix = ""
                                            } else if (option.first == "l" && selectedPrefix != "" && selectedPrefix != "m") {
                                                selectedPrefix = ""
                                            }
                                            expandedBaseUnitDropdown = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom unit input
                if (selectedBaseUnit == "Inne") {
                    Column {
                        Text(
                            text = "Własna jednostka",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedCustomUnitSuggestions,
                            onExpandedChange = { expandedCustomUnitSuggestions = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = customUnit,
                                onValueChange = {
                                    customUnit = it
                                    expandedCustomUnitSuggestions = it.length >= 2 && filteredCustomUnitSuggestions.isNotEmpty()
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            if (filteredCustomUnitSuggestions.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = expandedCustomUnitSuggestions,
                                    onDismissRequest = { expandedCustomUnitSuggestions = false },
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    filteredCustomUnitSuggestions.take(5).forEach { suggestion ->
                                        DropdownMenuItem(
                                            text = { Text(suggestion) },
                                            onClick = {
                                                customUnit = suggestion
                                                expandedCustomUnitSuggestions = false
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isNotBlank()) {
                        val amount = amountStr.replace(",", ".").toDoubleOrNull() ?: 1.0
                        val finalUnit = when (selectedBaseUnit) {
                            "Inne" -> customUnit.trim().ifBlank { "szt." }
                            "szt." -> "szt."
                            else -> selectedPrefix + selectedBaseUnit
                        }
                        onConfirm(trimmedName, amount, finalUnit, selectedTag)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
            ) {
                Text(if (initialItem == null) "Dodaj" else "Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}