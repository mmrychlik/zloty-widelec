package com.example.zlotywidelec.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.zlotywidelec.ui.viewmodel.SettingsViewModel

@Composable
fun AboutSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Złoty Widelec",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Twoja inteligentna kuchnia",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Wersja 0.0.1",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSignInClick: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val message by viewModel.message.collectAsState()
    val photoStorageUri by viewModel.photoStorageUri.collectAsState()
    val userAccount by viewModel.userAccount.collectAsState()

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showExportDataDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.setPhotoStorageUri(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.handleExportUri(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        checkedBorderColor = Color.Transparent,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Folder na zdjęcia przepisów",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = photoStorageUri?.let { "Ustawiono" } ?: "Nie ustawiono",
                    fontSize = 14.sp,
                    color = if (photoStorageUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(if (photoStorageUri == null) "Wybierz folder" else "Zmień folder")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Google Drive (Synchronizacja)",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (userAccount != null) {
                    Text(
                        text = "Zalogowano jako: ${userAccount?.email}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = { viewModel.signOutGoogle() },
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Wyloguj")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.syncWithGoogleDrive() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Synchronizuj dane teraz")
                    }
                    Text(
                        "Synchronizacja pobierze dane od znajomych i wyśle Twoje dane do chmury.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Niepołączono",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Połącz z Google Drive")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            TextButton(
                onClick = { showExportDataDialog = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Eksportuj dane (.zip)",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            TextButton(
                onClick = { importLauncher.launch(arrayOf("application/zip")) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Importuj dane (.zip)",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

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
                        text = "Wyczyść dane lokalne",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AboutSection()
        }
    }

    if (showExportDataDialog) {
        ExportDataDialog(
            onDismiss = { showExportDataDialog = false },
            onConfirm = { shopping, fridge, recipes ->
                exportLauncher.launch("zloty_widelec_backup.json")
                viewModel.exportDataAfterSelection(shopping, fridge, recipes)
            }
        )
    }

    if (showClearDataDialog) {
        ClearDataDialog(
            onDismiss = { showClearDataDialog = false },
            onConfirm = { shopping, fridge, suggestions, recipes ->
                if (shopping) viewModel.clearShoppingList()
                if (fridge) viewModel.clearFridge()
                if (suggestions) viewModel.clearProductSuggestions()
                if (recipes) viewModel.clearRecipes()
            }
        )
    }
}

@Composable
fun ExportDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Boolean, Boolean) -> Unit
) {
    var exportShopping by remember { mutableStateOf(true) }
    var exportFridge by remember { mutableStateOf(true) }
    var exportRecipes by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Co chcesz wyeksportować?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportShopping = !exportShopping }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = exportShopping, onCheckedChange = { exportShopping = it })
                    Text("Lista zakupów", modifier = Modifier.padding(start = 8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportFridge = !exportFridge }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = exportFridge, onCheckedChange = { exportFridge = it })
                    Text("Lodówka", modifier = Modifier.padding(start = 8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportRecipes = !exportRecipes }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = exportRecipes, onCheckedChange = { exportRecipes = it })
                    Text("Własne przepisy", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(exportShopping, exportFridge, exportRecipes)
                    onDismiss()
                },
                enabled = exportShopping || exportFridge || exportRecipes
            ) {
                Text("Eksportuj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
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
                        .clickable { clearRecipes = !clearRecipes }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = clearRecipes, onCheckedChange = { clearRecipes = it })
                    Text("Własne przepisy", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(clearShopping, clearFridge, clearSuggestions, clearRecipes)
                    onDismiss()
                },
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
