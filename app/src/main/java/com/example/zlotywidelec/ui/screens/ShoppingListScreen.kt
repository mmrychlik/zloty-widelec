package com.example.zlotywidelec.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.zlotywidelec.data.local.entity.ShoppingItemEntity
import com.example.zlotywidelec.ui.theme.*
import com.example.zlotywidelec.ui.viewmodel.ShoppingViewModel

@Composable
fun ShoppingListScreen(viewModel: ShoppingViewModel) {
    val items by viewModel.shoppingItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) {
            items
        } else {
            items.filter {
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
                Icon(Icons.Default.Add, contentDescription = "Add Item")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Szukaj produktów...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BeigeAccent,
                    unfocusedBorderColor = DarkText.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    ShoppingListItem(
                        item = item,
                        onCheckedChange = { viewModel.toggleItemChecked(item) }
                    )
                    HorizontalDivider(color = GrayText.copy(alpha = 0.2f))
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, quantity ->
                viewModel.addItem(name, quantity)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ShoppingListItem(
    item: ShoppingItemEntity,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (item.isChecked) GrayText else ItemDot)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = item.name,
                fontSize = 18.sp,
                color = if (item.isChecked) GrayText else DarkText,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
            )
            if (item.quantity.isNotEmpty()) {
                Text(
                    text = item.quantity,
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
                    modifier = Modifier.size(16.dp),
                    tint = DarkText
                )
            }
        }
    }
}

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(BeigeBackground)
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(GrayText.copy(alpha = 0.5f))
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Dodaj produkt do listy",
                    fontSize = 18.sp,
                    color = DarkText
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Nazwa", color = GrayText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BeigeAccent.copy(alpha = 0.3f),
                        unfocusedContainerColor = BeigeAccent.copy(alpha = 0.3f),
                        disabledContainerColor = BeigeAccent.copy(alpha = 0.3f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    placeholder = { Text("Ilość (Opcjonalnie)", color = GrayText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BeigeAccent.copy(alpha = 0.3f),
                        unfocusedContainerColor = BeigeAccent.copy(alpha = 0.3f),
                        disabledContainerColor = BeigeAccent.copy(alpha = 0.3f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { if (name.isNotBlank()) onAdd(name, quantity) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BeigeAccent)
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Add", tint = DarkText)
                    }
                }
            }
        }
    }
}
