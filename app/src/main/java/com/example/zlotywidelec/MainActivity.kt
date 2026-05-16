package com.example.zlotywidelec

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zlotywidelec.data.io.DataBackupManager
import com.example.zlotywidelec.data.local.AppDatabase
import com.example.zlotywidelec.ui.screens.*
import com.example.zlotywidelec.ui.theme.*
import com.example.zlotywidelec.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = remember { AppDatabase.getDatabase(context) }
            val backupManager = remember { DataBackupManager(context, database.ingredientDao(), database.recipeDao()) }
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(database.ingredientDao(), database.recipeDao(), backupManager)
            )
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            
            ZlotyWidelecTheme(darkTheme = isDarkMode) {
                ZlotyWidelecApp(settingsViewModel, database, backupManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZlotyWidelecApp(settingsViewModel: SettingsViewModel, database: AppDatabase, backupManager: DataBackupManager) {
    val photoStorageUri by settingsViewModel.photoStorageUri.collectAsState()
    var showFolderPrompt by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { settingsViewModel.setPhotoStorageUri(it) }
    }

    LaunchedEffect(photoStorageUri) {
        if (photoStorageUri == null) {
            showFolderPrompt = true
        }
    }

    if (showFolderPrompt && photoStorageUri == null) {
        AlertDialog(
            onDismissRequest = { /* Mandatory */ },
            title = { Text("Konfiguracja folderu zdjęć") },
            text = { Text("Wybierz folder, w którym będą przechowywane zdjęcia Twoich przepisów. Jest to wymagane do poprawnego działania kopii zapasowej.") },
            confirmButton = {
                Button(onClick = { folderPickerLauncher.launch(null) }) {
                    Text("Wybierz folder")
                }
            }
        )
    }

    val shoppingViewModel: ShoppingViewModel = viewModel(
        factory = ShoppingViewModelFactory(database.ingredientDao())
    )
    val fridgeViewModel: FridgeViewModel = viewModel(
        factory = FridgeViewModelFactory(database.ingredientDao())
    )
    val recipeViewModel: RecipeViewModel = viewModel(
        factory = RecipeViewModelFactory(database.ingredientDao(), database.recipeDao(), backupManager)
    )

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.SHOPPING_LIST) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val navSuiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onBackground,
            selectedTextColor = MaterialTheme.colorScheme.onBackground,
            unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        )
    )

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.filter { it.isMainDestination }.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { 
                        currentDestination = destination
                        isSearchActive = false
                        shoppingViewModel.setSearchQuery("")
                        fridgeViewModel.setSearchQuery("")
                        recipeViewModel.setSearchQuery("")
                    },
                    colors = navSuiteItemColors
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.background,
            navigationBarContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
{
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            if (isSearchActive && currentDestination.showSearch) {
                                IconButton(onClick = { 
                                    isSearchActive = false
                                    shoppingViewModel.setSearchQuery("")
                                    fridgeViewModel.setSearchQuery("")
                                    recipeViewModel.setSearchQuery("")
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Cofnij",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        },
                        title = {
                            if (isSearchActive && currentDestination.showSearch) {
                                val searchQuery = when (currentDestination) {
                                    AppDestinations.SHOPPING_LIST -> shoppingViewModel.searchQuery.collectAsState().value
                                    AppDestinations.MY_FRIDGE -> fridgeViewModel.searchQuery.collectAsState().value
                                    AppDestinations.RECIPES, AppDestinations.FRIENDS_RECIPES -> recipeViewModel.searchQuery.collectAsState().value
                                    else -> ""
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = {
                                            when (currentDestination) {
                                                AppDestinations.SHOPPING_LIST -> shoppingViewModel.setSearchQuery(it)
                                                AppDestinations.MY_FRIDGE -> fridgeViewModel.setSearchQuery(it)
                                                AppDestinations.RECIPES, AppDestinations.FRIENDS_RECIPES -> recipeViewModel.setSearchQuery(it)
                                                else -> {}
                                            }
                                        },
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 20.sp // Slightly adjusted to look better in the bar
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Szukaj...",
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                    fontSize = 20.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            } else {
                                Text(currentDestination.label, color = MaterialTheme.colorScheme.onBackground)
                            }
                        },
                        actions = {
                            if (!isSearchActive && currentDestination.showSearch) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Szukaj", tint = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                            
                            Box {
                                IconButton(onClick = { 
                                    isSearchActive = false
                                    currentDestination = AppDestinations.SETTINGS 
                                }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Ustawienia", tint = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                }
            },
            bottomBar = {
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            }
        )
{ padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentDestination) {
                    AppDestinations.SHOPPING_LIST -> ShoppingListScreen(viewModel = shoppingViewModel)
                    AppDestinations.MY_FRIDGE -> MyFridgeScreen(viewModel = fridgeViewModel)
                    AppDestinations.RECIPES -> RecipesScreen(viewModel = recipeViewModel)
                    AppDestinations.FRIENDS_RECIPES -> FriendsRecipesScreen(viewModel = recipeViewModel)
                    AppDestinations.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val isMainDestination: Boolean = true,
    val showSearch: Boolean = false
) {
    SHOPPING_LIST("Lista", Icons.AutoMirrored.Filled.List, showSearch = true),
    MY_FRIDGE("Lodówka", Icons.Default.Kitchen, showSearch = true),
    RECIPES("Przepisy", Icons.AutoMirrored.Filled.MenuBook, showSearch = true),
    FRIENDS_RECIPES("Znajomi", Icons.Default.People, showSearch = true),
    SETTINGS("Ustawienia", Icons.Default.Settings, false)
}
