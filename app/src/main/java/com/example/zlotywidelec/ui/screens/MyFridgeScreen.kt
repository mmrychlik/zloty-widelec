package com.example.zlotywidelec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zlotywidelec.data.local.dao.IngredientNameAndUnit
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.ui.viewmodel.FridgeSortOrder
import com.example.zlotywidelec.ui.viewmodel.FridgeViewModel
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun MyFridgeScreen(viewModel: FridgeViewModel) {
    val fridgeItems by viewModel.fridgeItems.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItemForDetails by remember { mutableStateOf<IngredientEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj produkt")
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (sortOrder == FridgeSortOrder.ALPHABETICAL) "Sortowanie: A-Z" else "Sortowanie: Data",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                IconButton(onClick = { viewModel.toggleSortOrder() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Zmień sortowanie",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (fridgeItems.isEmpty()) {
                EmptyFridgeMessage()
            } else {
                FridgeItemList(
                    items = fridgeItems,
                    onDelete = { viewModel.deleteItem(it) },
                    onShowDetails = { selectedItemForDetails = it }
                )
            }
        }
    }

    if (showAddDialog) {
        AddFridgeItemDialog(
            suggestions = suggestions,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, unit ->
                viewModel.addItem(name, amount, unit)
                showAddDialog = false
            }
        )
    }

    selectedItemForDetails?.let { item ->
        IngredientDetailsDialog(
            item = item,
            onDismiss = { selectedItemForDetails = null }
        )
    }
}

@Composable
fun EmptyFridgeMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Kitchen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Twoja lodówka jest pusta",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun FridgeItemList(
    items: List<IngredientEntity>,
    onDelete: (IngredientEntity) -> Unit,
    onShowDetails: (IngredientEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            FridgeItemRow(
                item = item,
                onDelete = { onDelete(item) },
                onShowDetails = { onShowDetails(item) }
            )
        }
    }
}

@Composable
fun FridgeItemRow(
    item: IngredientEntity,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val daysAgo = remember(item.addedAt) {
        val diff = System.currentTimeMillis() - item.addedAt
        TimeUnit.MILLISECONDS.toDays(diff)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ilość: ${if (item.amount % 1.0 == 0.0) item.amount.toInt() else item.amount} ${item.unit}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• dodano ${if (daysAgo == 0L) "dzisiaj" else "$daysAgo dni temu"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opcje",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = { Text("Szczegóły", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            showMenu = false
                            onShowDetails()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Usuń", color = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun IngredientDetailsDialog(
    item: IngredientEntity,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val dateString = remember(item.addedAt) { sdf.format(Date(item.addedAt)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ilość: ${if (item.amount % 1.0 == 0.0) item.amount.toInt() else item.amount} ${item.unit}")
                Text("Data dodania: $dateString")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFridgeItemDialog(
    suggestions: List<IngredientNameAndUnit>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    
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
    
    var selectedPrefix by remember { mutableStateOf(prefixOptions[0].first) }
    var selectedBaseUnit by remember { mutableStateOf("szt.") }
    var customUnit by remember { mutableStateOf("") }
    
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
        title = { Text("Dodaj do lodówki", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Name
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
                        label = { Text("Nazwa produktu") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    if (filteredSuggestions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expandedNameSuggestions,
                            onDismissRequest = { expandedNameSuggestions = false },
                            modifier = Modifier.fillMaxWidth()
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

                // Row 2: Amount
                TextField(
                    value = amountStr,
                    onValueChange = { if (it.isEmpty() || it.replace(",", ".").toDoubleOrNull() != null || it.endsWith(".") || it.endsWith(",")) amountStr = it },
                    label = { Text("Ilość") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Row 3: Prefix and Base Unit
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
                        ExposedDropdownMenuBox(
                            expanded = expandedPrefixDropdown,
                            onExpandedChange = { expandedPrefixDropdown = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            TextField(
                                value = prefixOptions.find { it.first == selectedPrefix }?.second ?: "—",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Przed.") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPrefixDropdown) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedPrefixDropdown,
                                onDismissRequest = { expandedPrefixDropdown = false }
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

                    ExposedDropdownMenuBox(
                        expanded = expandedBaseUnitDropdown,
                        onExpandedChange = { expandedBaseUnitDropdown = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = baseUnitOptions.find { it.first == selectedBaseUnit }?.second ?: selectedBaseUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jedn.") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBaseUnitDropdown) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedBaseUnitDropdown,
                            onDismissRequest = { expandedBaseUnitDropdown = false }
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

                if (selectedBaseUnit == "Inne") {
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
                            label = { Text("Własna jednostka") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (filteredCustomUnitSuggestions.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = expandedCustomUnitSuggestions,
                                onDismissRequest = { expandedCustomUnitSuggestions = false },
                                modifier = Modifier.fillMaxWidth()
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
                        onConfirm(trimmedName, amount, finalUnit)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
            ) {
                Text("Dodaj")
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
