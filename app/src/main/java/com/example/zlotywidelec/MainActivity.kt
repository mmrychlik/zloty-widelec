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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
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
import com.example.zlotywidelec.data.sync.DriveSyncManager
import com.example.zlotywidelec.data.sync.GoogleDriveService
import com.example.zlotywidelec.ui.screens.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.example.zlotywidelec.ui.theme.*
import com.example.zlotywidelec.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            /* App dependency initialization */
            val context = LocalContext.current
            val database = remember { AppDatabase.getDatabase(context) }
            val backupManager = remember { DataBackupManager(context, database.ingredientDao(), database.recipeDao()) }
            val googleDriveService = remember { GoogleDriveService(context) }
            val syncManager = remember { DriveSyncManager(googleDriveService) }
            
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(
                    database.ingredientDao(),
                    database.recipeDao(),
                    database.friendDao(),
                    backupManager,
                    googleDriveService,
                    syncManager
                )
            )
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            
            ZlotyWidelecTheme(darkTheme = isDarkMode) {
                ZlotyWidelecApp(settingsViewModel, database, backupManager, googleDriveService, syncManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZlotyWidelecApp(
    settingsViewModel: SettingsViewModel,
    database: AppDatabase,
    backupManager: DataBackupManager,
    googleDriveService: GoogleDriveService,
    syncManager: DriveSyncManager
) {
    val photoStorageUri by settingsViewModel.photoStorageUri.collectAsState()
    var showFolderPrompt by remember { mutableStateOf(false) }

    /* Google Sign In handling */
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            settingsViewModel.updateGoogleAccount(account)
        } catch (e: com.google.android.gms.common.api.ApiException) {
            e.printStackTrace()
            val errorMsg = when (e.statusCode) {
                10 -> "Błąd programisty (10): Sprawdź SHA-1 i nazwę pakietu w Google Cloud Console."
                7 -> "Błąd sieci. Sprawdź połączenie."
                12501 -> "Logowanie anulowane."
                else -> "Błąd logowania Google (${e.statusCode}): ${e.message}"
            }
            settingsViewModel.showMessage(errorMsg)
        } catch (e: Exception) {
            e.printStackTrace()
            settingsViewModel.showMessage("Nieoczekiwany błąd: ${e.message}")
        }
    }

    /* Folder picker for photos */
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

    /* Photo folder setup prompt */
    if (showFolderPrompt && photoStorageUri == null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Konfiguracja folderu zdjęć") },
            text = { Text("Wybierz folder, w którym będą przechowywane zdjęcia Twoich przepisów. Jest to wymagane do poprawnego działania kopii zapasowej.") },
            confirmButton = {
                Button(onClick = { folderPickerLauncher.launch(null) }) {
                    Text("Wybierz folder")
                }
            }
        )
    }

    /* ViewModels initialization */
    val shoppingViewModel: ShoppingViewModel = viewModel(
        factory = ShoppingViewModelFactory(database.ingredientDao(), syncManager)
    )
    val fridgeViewModel: FridgeViewModel = viewModel(
        factory = FridgeViewModelFactory(database.ingredientDao(), syncManager)
    )
    val recipeViewModel: RecipeViewModel = viewModel(
        factory = RecipeViewModelFactory(database.ingredientDao(), database.recipeDao(), database.friendDao(), backupManager, syncManager)
    )

    val message by settingsViewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    /* Snackbar message handling */
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            settingsViewModel.clearMessage()
        }
    }

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

    /* Main Navigation Layout */
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
    ) {
        Scaffold(
            topBar = {
                Column {
                    /* Top App Bar with Search functionality */
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
                                            fontSize = 20.sp
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
                                val userAccount by settingsViewModel.userAccount.collectAsState()
                                if (userAccount != null) {
                                    IconButton(onClick = { settingsViewModel.syncWithGoogleDrive() }) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = "Synchronizuj",
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            }
        ) { padding ->
            /* Screen switching logic */
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentDestination) {
                    AppDestinations.SHOPPING_LIST -> ShoppingListScreen(viewModel = shoppingViewModel)
                    AppDestinations.MY_FRIDGE -> MyFridgeScreen(viewModel = fridgeViewModel)
                    AppDestinations.RECIPES -> RecipesScreen(
                        viewModel = recipeViewModel,
                        onAddToShoppingList = { ingredients: List<com.example.zlotywidelec.data.local.entity.RecipeIngredientEntity> ->
                            shoppingViewModel.addIngredientsFromRecipe(ingredients)
                            settingsViewModel.showMessage("Dodano brakujące składniki do listy")
                        },
                        onUseFromFridge = { ingredients ->
                            recipeViewModel.useRecipeIngredients(ingredients)
                            settingsViewModel.showMessage("Zużyto składniki z lodówki")
                        }
                    )
                    AppDestinations.FRIENDS_RECIPES -> FriendsRecipesScreen(
                        viewModel = recipeViewModel,
                        settingsViewModel = settingsViewModel,
                        onAddToShoppingList = { ingredients: List<com.example.zlotywidelec.data.local.entity.RecipeIngredientEntity> ->
                            shoppingViewModel.addIngredientsFromRecipe(ingredients)
                            settingsViewModel.showMessage("Dodano brakujące składniki do listy")
                        },
                        onUseFromFridge = { ingredients ->
                            recipeViewModel.useRecipeIngredients(ingredients)
                            settingsViewModel.showMessage("Zużyto składniki z lodówki")
                        }
                    )
                    AppDestinations.SETTINGS -> SettingsScreen(
                        viewModel = settingsViewModel,
                        onSignInClick = {
                            googleSignInLauncher.launch(settingsViewModel.googleDriveService.googleSignInClient.signInIntent)
                        }
                    )
                }
            }
        }
    }
}

/* App destination enum with icons and labels */
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
