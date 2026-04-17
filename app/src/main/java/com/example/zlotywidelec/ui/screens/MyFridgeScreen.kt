package com.example.zlotywidelec.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.ui.theme.BeigeAccent
import com.example.zlotywidelec.ui.theme.BeigeBackground
import com.example.zlotywidelec.ui.theme.DarkText
import com.example.zlotywidelec.ui.viewmodel.FridgeViewModel

@Composable
fun MyFridgeScreen(viewModel: FridgeViewModel) {
    val fridgeItems by viewModel.fridgeItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

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
            if (fridgeItems.isEmpty()) {
                    EmptyFridgeMessage()
            } else {
                FridgeItemList(
                    items = fridgeItems,
                    onDelete = { viewModel.deleteItem(it) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddFridgeItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, unit ->
                viewModel.addItem(name, amount, unit)
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
fun FridgeItemList(
    items: List<IngredientEntity>,
    onDelete: (IngredientEntity) -> Unit
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
    item: IngredientEntity,
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
                    text = "Ilość: ${if (item.amount % 1.0 == 0.0) item.amount.toInt() else item.amount} ${item.unit}",
                    fontSize = 14.sp,
                    color = DarkText.copy(alpha = 0.7f)
                )
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
    onConfirm: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Ilość") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = DarkText,
                            unfocusedTextColor = DarkText
                        )
                    )
                    TextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Jednostka") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = DarkText,
                            unfocusedTextColor = DarkText
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank()) {
                        val amount = amountStr.toDoubleOrNull() ?: 1.0
                        onConfirm(name, amount, unit)
                    }
                },
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
