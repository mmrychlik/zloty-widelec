package com.example.zlotywidelec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zlotywidelec.data.local.AppDatabase
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.ui.screens.*
import com.example.zlotywidelec.ui.theme.DarkText
import com.example.zlotywidelec.ui.theme.ZlotyWidelecTheme
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
        factory = ShoppingViewModelFactory(database.shoppingDao())
    )
    val fridgeViewModel: FridgeViewModel = viewModel(
        factory = FridgeViewModelFactory(database.fridgeDao())
    )
    val recipeViewModel: RecipeViewModel = viewModel(
        factory = RecipeViewModelFactory(database.recipeDao(), database.shoppingDao())
    )

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.SHOPPING_LIST) }
    var selectedRecipe by remember { mutableStateOf<RecipeEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    if (selectedRecipe != null) {
        RecipeDetailScreen(
            recipe = selectedRecipe!!,
            onBack = { selectedRecipe = null }
        )
    } else {
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
                        onClick = { currentDestination = destination },
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
                        title = { Text(currentDestination.label, color = DarkText) },
                        actions = {
                            if (currentDestination == AppDestinations.MY_FRIDGE) {
                                IconButton(onClick = { fridgeViewModel.syncWithSmartFridge() }) {
                                    Icon(Icons.Default.Sync, contentDescription = "Sync", tint = DarkText)
                                }
                            }
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
                                        currentDestination = AppDestinations.SETTINGS
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("O aplikacji", color = DarkText) },
                                    onClick = {
                                        showMenu = false
                                        currentDestination = AppDestinations.ABOUT
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White
                        )
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    when (currentDestination) {
                        AppDestinations.SHOPPING_LIST -> ShoppingListScreen(viewModel = shoppingViewModel)
                        AppDestinations.RECIPES -> RecipesScreen(
                            viewModel = recipeViewModel,
                            fridgeViewModel = fridgeViewModel,
                            onRecipeClick = { selectedRecipe = it }
                        )
                        AppDestinations.MY_FRIDGE -> MyFridgeScreen(viewModel = fridgeViewModel)
                        AppDestinations.CHEFS_RECIPES -> ChefsRecipesScreen(
                            viewModel = recipeViewModel,
                            onRecipeClick = { selectedRecipe = it }
                        )
                        AppDestinations.SETTINGS -> SettingsScreen()
                        AppDestinations.ABOUT -> AboutScreen()
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val isMainDestination: Boolean = true
) {
    SHOPPING_LIST("Lista", Icons.AutoMirrored.Filled.List),
    RECIPES("Przepisy", Icons.AutoMirrored.Filled.MenuBook),
    MY_FRIDGE("Lodówka", Icons.Default.Kitchen),
    CHEFS_RECIPES("Szef", Icons.Default.Restaurant),
    SETTINGS("Ustawienia", Icons.Default.Kitchen, false),
    ABOUT("O aplikacji", Icons.Default.Kitchen, false)
}
