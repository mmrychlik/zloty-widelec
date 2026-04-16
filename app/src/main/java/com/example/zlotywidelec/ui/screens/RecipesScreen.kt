package com.example.zlotywidelec.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.zlotywidelec.data.local.entity.FridgeItemEntity
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.ui.theme.BeigeAccent
import com.example.zlotywidelec.ui.theme.BeigeBackground
import com.example.zlotywidelec.ui.theme.DarkText
import com.example.zlotywidelec.ui.theme.GrayText
import com.example.zlotywidelec.ui.viewmodel.FridgeViewModel
import com.example.zlotywidelec.ui.viewmodel.RecipeViewModel

@Composable
fun RecipesScreen(
    viewModel: RecipeViewModel,
    fridgeViewModel: FridgeViewModel,
    onRecipeClick: (RecipeEntity) -> Unit
) {
    val recipes by viewModel.allRecipes.collectAsState()
    val fridgeItems by fridgeViewModel.fridgeItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredRecipes = remember(recipes, searchQuery) {
        if (searchQuery.isBlank()) {
            recipes
        } else {
            recipes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
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
                Icon(Icons.Default.Add, contentDescription = "Dodaj przepis")
            }
        },
        containerColor = BeigeBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Szukaj przepisów...") },
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

            if (filteredRecipes.isEmpty()) {
                if (searchQuery.isNotEmpty()) {
                    NoResultsMessage(searchQuery)
                } else {
                    EmptyRecipesMessage()
                }
            } else {
                RecipeList(
                    recipes = filteredRecipes,
                    fridgeItems = fridgeItems,
                    onAddItemsToShoppingList = { viewModel.addItemsToShoppingList(it) },
                    onCookRecipe = { ingredients ->
                        fridgeViewModel.removeIngredients(ingredients)
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddRecipeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description, ingredients, instructions, imageUrl ->
                viewModel.addRecipe(title, description, ingredients, instructions, imageUrl)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RecipeList(
    recipes: List<RecipeEntity>,
    fridgeItems: List<FridgeItemEntity>,
    onAddItemsToShoppingList: (List<Pair<String, String>>) -> Unit,
    onCookRecipe: (List<Pair<String, String>>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(recipes) { recipe ->
            ExpandableRecipeRow(
                recipe = recipe,
                fridgeItems = fridgeItems,
                onAddItemsToShoppingList = onAddItemsToShoppingList,
                onCookRecipe = onCookRecipe
            )
        }
    }
}

@Composable
fun ExpandableRecipeRow(
    recipe: RecipeEntity,
    fridgeItems: List<FridgeItemEntity>,
    onAddItemsToShoppingList: (List<Pair<String, String>>) -> Unit,
    onCookRecipe: (List<Pair<String, String>>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showCookConfirmation by remember { mutableStateOf(false) }

    val parsedIngredients = remember(recipe.ingredients) {
        recipe.ingredients.split(",").filter { it.isNotBlank() }.map {
            val parts = it.split(":")
            if (parts.size >= 2) parts[0].trim() to parts[1].trim()
            else it.trim() to ""
        }
    }

    val matchingIngredientsCount = remember(parsedIngredients, fridgeItems) {
        parsedIngredients.count { (name, _) ->
            fridgeItems.any { it.name.contains(name, ignoreCase = true) }
        }
    }
    val percentage = if (parsedIngredients.isEmpty()) 0 else (matchingIngredientsCount * 100 / parsedIngredients.size)

    if (showCookConfirmation) {
        AlertDialog(
            onDismissRequest = { showCookConfirmation = false },
            title = { Text("Gotuj", color = DarkText) },
            text = { Text("Czy na pewno chcesz ugotować to danie? Składniki zostaną usunięte z lodówki.", color = DarkText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCookRecipe(parsedIngredients)
                        showCookConfirmation = false
                    }
                ) {
                    Text("Tak", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCookConfirmation = false }) {
                    Text("Anuluj", color = GrayText)
                }
            },
            containerColor = Color.White
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEEDCBF).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .background(BeigeAccent, RoundedCornerShape(12.dp))
                        .size(40.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = DarkText
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        text = "$percentage% składników",
                        fontSize = 12.sp,
                        color = DarkText,
                        fontWeight = FontWeight.Bold
                    )
                }

                    val painter = rememberAsyncImagePainter(
                        model = recipe.imageUrl ?: "https://via.placeholder.com/150?text=Brak+zdjęcia"
                    )
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    val selectedItems = remember { mutableStateMapOf<Int, Boolean>() }

                    Text(
                        text = "Składniki:",
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    parsedIngredients.forEachIndexed { index, (name, qty) ->
                        val fridgeItem = fridgeItems.find { it.name.contains(name, ignoreCase = true) }
                        val fridgeQty = fridgeItem?.quantity ?: "0 szt"
                        val hasIngredient = fridgeItem != null
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedItems[index] ?: false,
                                onCheckedChange = { selectedItems[index] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BeigeAccent,
                                    uncheckedColor = GrayText
                                )
                            )
                            Text(
                                text = "$name $qty",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                color = DarkText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "|",
                                fontSize = 14.sp,
                                color = DarkText.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Kitchen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (hasIngredient) Color(0xFF4CAF50) else GrayText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ": $fridgeQty",
                                fontSize = 14.sp,
                                color = DarkText.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Instrukcje:",
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        text = recipe.instructions,
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 14.sp,
                        color = DarkText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showCookConfirmation = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Gotuj", color = Color.White)
                        }
                        Button(
                            onClick = {
                                val toAdd = parsedIngredients.filterIndexed { index, _ -> 
                                    selectedItems[index] == true 
                                }
                                onAddItemsToShoppingList(toAdd)
                            },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = BeigeAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Dodaj do listy", color = DarkText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyRecipesMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = DarkText.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Zapisz swoje ulubione przepisy!",
                color = DarkText.copy(alpha = 0.5f),
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun NoResultsMessage(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Brak wyników dla: \"$query\"",
            color = DarkText.copy(alpha = 0.5f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
fun AddRecipeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj przepis") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = DarkText)
                            Text("Dodaj zdjęcie", color = DarkText)
                        }
                    }
                }

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nazwa przepisu") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText
                    )
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Krótki opis") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText
                    )
                )
                TextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    label = { Text("Składniki (np. Jajka:2 szt,Mleko:1 l)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText
                    )
                )
                TextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instrukcje") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                        onConfirm(title, description, ingredients, instructions, imageUri?.toString()) 
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BeigeAccent)
            ) {
                Text("Zapisz")
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
