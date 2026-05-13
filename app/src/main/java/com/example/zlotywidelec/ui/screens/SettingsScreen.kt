package com.example.zlotywidelec.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zlotywidelec.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tryb ciemny",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(
                checked = isDarkMode,
                onCheckedChange = { viewModel.toggleDarkMode(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.secondary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

        TextButton(
            onClick = { showClearDataDialog = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Wyczyść dane",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showClearDataDialog) {
        ClearDataDialog(
            onDismiss = { showClearDataDialog = false },
            onConfirm = { shopping, fridge, suggestions, recipes ->
                if (shopping) viewModel.clearShoppingList()
                if (fridge) viewModel.clearFridge()
                if (suggestions) viewModel.clearProductSuggestions()
                if (recipes) viewModel.clearRecipes()
                showClearDataDialog = false
            }
        )
    }
}

@Composable
fun ClearDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var clearShopping by remember { mutableStateOf(false) }
    var clearFridge by remember { mutableStateOf(false) }
    var clearSuggestions by remember { mutableStateOf(false) }
    var clearRecipes by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Co chcesz wyczyścić?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { clearShopping = !clearShopping }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = clearShopping, onCheckedChange = { clearShopping = it })
                    Text("Lista zakupów", modifier = Modifier.padding(start = 8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { clearFridge = !clearFridge }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = clearFridge, onCheckedChange = { clearFridge = it })
                    Text("Lodówka", modifier = Modifier.padding(start = 8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { clearSuggestions = !clearSuggestions }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = clearSuggestions, onCheckedChange = { clearSuggestions = it })
                    Text("Podpowiedzi produktów", modifier = Modifier.padding(start = 8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = clearRecipes, onCheckedChange = null, enabled = false)
                    Text("Przepisy (wkrótce)", modifier = Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(clearShopping, clearFridge, clearSuggestions, clearRecipes) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = clearShopping || clearFridge || clearSuggestions || clearRecipes
            ) {
                Text("Wyczyść")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
