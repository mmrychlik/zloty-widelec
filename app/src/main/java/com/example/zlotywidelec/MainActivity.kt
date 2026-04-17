package com.example.zlotywidelec

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
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zlotywidelec.data.local.AppDatabase
import com.example.zlotywidelec.ui.screens.*
import com.example.zlotywidelec.ui.theme.*
import com.example.zlotywidelec.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZlotyWidelecTheme {
                ZlotyWidelecApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZlotyWidelecApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val shoppingViewModel: ShoppingViewModel = viewModel(
        factory = ShoppingViewModelFactory(database.ingredientDao())
    )
    val fridgeViewModel: FridgeViewModel = viewModel(
        factory = FridgeViewModelFactory(database.ingredientDao())
    )

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.SHOPPING_LIST) }
    var showMenu by remember { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

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
                    },
                )
            }
        },
        containerColor = Color.White,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = Color.White,
            navigationBarContentColor = DarkText
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (isSearchActive && currentDestination.showSearch) {
                            IconButton(onClick = { 
                                isSearchActive = false
                                shoppingViewModel.setSearchQuery("")
                                fridgeViewModel.setSearchQuery("")
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Cofnij",
                                    tint = DarkText
                                )
                            }
                        }
                    },
                    title = {
                        if (isSearchActive && currentDestination.showSearch) {
                            val searchQuery = when (currentDestination) {
                                AppDestinations.SHOPPING_LIST -> shoppingViewModel.searchQuery.collectAsState().value
                                AppDestinations.MY_FRIDGE -> fridgeViewModel.searchQuery.collectAsState().value
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
                                            else -> {}
                                        }
                                    },
                                    textStyle = TextStyle(
                                        color = DarkText,
                                        fontSize = 20.sp // Slightly adjusted to look better in the bar
                                    ),
                                    cursorBrush = SolidColor(DarkText),
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
                                                color = DarkText.copy(alpha = 0.5f),
                                                fontSize = 20.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        } else {
                            Text(currentDestination.label, color = DarkText)
                        }
                    },
                    actions = {
                        if (!isSearchActive && currentDestination.showSearch) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Szukaj", tint = DarkText)
                            }
                        }
                        
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = DarkText)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = Color.White
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ustawienia", color = DarkText) },
                                    onClick = {
                                        showMenu = false
                                        isSearchActive = false
                                        currentDestination = AppDestinations.SETTINGS
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("O aplikacji", color = DarkText) },
                                    onClick = {
                                        showMenu = false
                                        isSearchActive = false
                                        currentDestination = AppDestinations.ABOUT
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BeigeBackground
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(BeigeBackground)
            ) {
                when (currentDestination) {
                    AppDestinations.SHOPPING_LIST -> ShoppingListScreen(viewModel = shoppingViewModel)
                    AppDestinations.MY_FRIDGE -> MyFridgeScreen(viewModel = fridgeViewModel)
                    AppDestinations.RECIPES -> RecipesScreen()
                    AppDestinations.CHEFS_RECIPES -> ChefsRecipesScreen()
                    AppDestinations.SETTINGS -> SettingsScreen()
                    AppDestinations.ABOUT -> AboutScreen()
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
    RECIPES("Przepisy", Icons.AutoMirrored.Filled.MenuBook),
    MY_FRIDGE("Lodówka", Icons.Default.Kitchen, showSearch = true),
    CHEFS_RECIPES("Szef", Icons.Default.Restaurant),
    SETTINGS("Ustawienia", Icons.Default.MoreVert, false),
    ABOUT("O aplikacji", Icons.Default.MoreVert, false)
}
