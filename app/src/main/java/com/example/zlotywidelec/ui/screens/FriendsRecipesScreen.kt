package com.example.zlotywidelec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.zlotywidelec.data.local.entity.FriendEntity
import com.example.zlotywidelec.data.local.entity.RecipeIngredientEntity
import com.example.zlotywidelec.ui.viewmodel.RecipeViewModel
import com.example.zlotywidelec.ui.viewmodel.SettingsViewModel

// Screen for managing friends and their synchronization settings
@Composable
fun FriendsRecipesScreen(
    viewModel: RecipeViewModel,
    settingsViewModel: SettingsViewModel,
    onAddToShoppingList: (List<RecipeIngredientEntity>) -> Unit
) {
    val friends by settingsViewModel.friends.collectAsState()
    val userAccount by settingsViewModel.userAccount.collectAsState()
    
    var showAddFriendDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (userAccount != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddFriendDialog = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Dodaj znajomego") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (userAccount == null) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Zaloguj się w ustawieniach, aby móc dodawać znajomych i synchronizować ich przepisy.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else if (friends.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Lista znajomych jest pusta", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(friends, key = { it.email }) { friend ->
                        FriendItem(
                            friend = friend,
                            onUpdateSync = { recipes, fridge, shopping ->
                                settingsViewModel.updateFriendSyncSettings(friend, recipes, fridge, shopping)
                            },
                            onDelete = { settingsViewModel.deleteFriend(friend) }
                        )
                    }
                }
            }
        }
    }

    if (showAddFriendDialog) {
        var email by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = { Text("Dodaj znajomego") },
            text = {
                Column {
                    Text("Wpisz adres e-mail znajomego, aby udostępnić mu swoje dane i umożliwić pobieranie jego przepisów.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (email.contains("@")) {
                            settingsViewModel.shareWithFriend(email)
                            showAddFriendDialog = false
                        }
                    },
                    enabled = email.contains("@")
                ) {
                    Text("Dodaj")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

// Individual friend item with sync settings
@Composable
fun FriendItem(
    friend: FriendEntity,
    onUpdateSync: (Boolean, Boolean, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (friend.name.isNotEmpty()) friend.name else friend.email,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (friend.name.isNotEmpty()) {
                        Text(
                            text = friend.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Close, contentDescription = "Usuń znajomego", tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            Text("Ustawienia synchronizacji:", style = MaterialTheme.typography.labelSmall)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SyncToggle(
                    label = "Przepisy",
                    checked = friend.syncRecipes,
                    onCheckedChange = { onUpdateSync(it, friend.syncFridge, friend.syncShopping) }
                )
                SyncToggle(
                    label = "Lodówka",
                    checked = friend.syncFridge,
                    onCheckedChange = { onUpdateSync(friend.syncRecipes, it, friend.syncShopping) }
                )
                SyncToggle(
                    label = "Zakupy",
                    checked = friend.syncShopping,
                    onCheckedChange = { onUpdateSync(friend.syncRecipes, friend.syncFridge, it) }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Usuń znajomego") },
            text = { Text("Czy na pewno chcesz przestać synchronizować dane z ${friend.email}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

// Toggle for sync settings
@Composable
fun SyncToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
