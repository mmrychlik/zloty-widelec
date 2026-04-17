package com.example.zlotywidelec.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.ui.theme.*
import com.example.zlotywidelec.ui.viewmodel.ShoppingViewModel

@Composable
fun ShoppingListScreen(viewModel: ShoppingViewModel) {
    val items by viewModel.shoppingItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val hasCheckedItems = items.any { it.isChecked }

    Scaffold(
        floatingActionButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasCheckedItems) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.moveCheckedToFridge() },
                        containerColor = BeigeAccent,
                        contentColor = DarkText,
                        icon = { Icon(Icons.Default.Kitchen, contentDescription = "Add to fridge") },
                        text = { Text("Do lodówki") }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = BeigeAccent,
                    contentColor = DarkText
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        },
        containerColor = BeigeBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ShoppingListItem(
                        item = item,
                        onCheckedChange = { viewModel.toggleItemChecked(item) },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                    HorizontalDivider(color = GrayText.copy(alpha = 0.2f))
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, unit ->
                viewModel.addItem(name, amount, unit)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ShoppingListItem(
    item: IngredientEntity,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Usuń",
                tint = if (item.isChecked) GrayText else Color.Red,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = item.name,
                fontSize = 18.sp,
                color = if (item.isChecked) GrayText else DarkText,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
            )
            if (item.amount > 0) {
                Text(
                    text = "${if (item.amount % 1.0 == 0.0) item.amount.toInt() else item.amount} ${item.unit}",
                    fontSize = 14.sp,
                    color = GrayText
                )
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.dp, CheckboxBorder, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(if (item.isChecked) BeigeAccent else Color.Transparent)
                .clickable { onCheckedChange(!item.isChecked) },
            contentAlignment = Alignment.Center
        ) {
            if (item.isChecked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = DarkText
                )
            }
        }
    }
}

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj do listy") },
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
