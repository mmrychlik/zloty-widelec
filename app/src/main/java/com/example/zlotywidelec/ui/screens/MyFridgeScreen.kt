package com.example.zlotywidelec.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zlotywidelec.data.local.entity.FridgeItemEntity
import com.example.zlotywidelec.ui.theme.BeigeAccent
import com.example.zlotywidelec.ui.theme.BeigeBackground
import com.example.zlotywidelec.ui.theme.DarkText
import com.example.zlotywidelec.ui.viewmodel.FridgeViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MyFridgeScreen(viewModel: FridgeViewModel) {
    val fridgeItems by viewModel.fridgeItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(fridgeItems, searchQuery) {
        if (searchQuery.isBlank()) {
            fridgeItems
        } else {
            fridgeItems.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BeigeAccent,
                contentColor = DarkText
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj produkt")
            }
        },
        containerColor = BeigeBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Szukaj produktów...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BeigeAccent,
                    unfocusedBorderColor = DarkText.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            if (filteredItems.isEmpty()) {
                if (searchQuery.isNotEmpty()) {
                    NoResultsFridgeMessage(searchQuery)
                } else {
                    EmptyFridgeMessage()
                }
            } else {
                FridgeItemList(
                    items = filteredItems,
                    onDelete = { viewModel.deleteItem(it) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddFridgeItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, quantity ->
                viewModel.addItem(name, quantity)
                showAddDialog = false
            }
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
                tint = DarkText.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Twoja lodówka jest pusta",
                color = DarkText.copy(alpha = 0.5f),
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun NoResultsFridgeMessage(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Brak produktów dla: \"$query\"",
            color = DarkText.copy(alpha = 0.5f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
fun FridgeItemList(
    items: List<FridgeItemEntity>,
    onDelete: (FridgeItemEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            FridgeItemRow(item = item, onDelete = { onDelete(item) })
        }
    }
}

@Composable
fun FridgeItemRow(
    item: FridgeItemEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = DarkText
                )
                Text(
                    text = "Ilość: ${item.quantity}",
                    fontSize = 14.sp,
                    color = DarkText.copy(alpha = 0.7f)
                )
                item.expirationDate?.let { dateMillis ->
                    val date = Instant.ofEpochMilli(dateMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    Text(
                        text = "Ważność: ${date.format(formatter)}",
                        fontSize = 12.sp,
                        color = if (date.isBefore(LocalDate.now())) Color.Red else DarkText.copy(alpha = 0.5f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun AddFridgeItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj do lodówki") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa produktu") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText
                    )
                )
                TextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Ilość (np. 2 szt, 500g)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, quantity) },
                colors = ButtonDefaults.buttonColors(containerColor = BeigeAccent)
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = DarkText)
            }
        },
        containerColor = Color.White
    )
}
