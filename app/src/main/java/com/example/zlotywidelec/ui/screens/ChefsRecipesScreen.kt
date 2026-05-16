package com.example.zlotywidelec.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.zlotywidelec.data.local.dao.RecipeWithIngredients
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.data.local.entity.RecipeIngredientEntity
import com.example.zlotywidelec.ui.viewmodel.RecipeSortOrder
import com.example.zlotywidelec.ui.viewmodel.RecipeViewModel

@Composable
fun ChefsRecipesScreen(viewModel: RecipeViewModel) {
    val recipes by viewModel.filteredChefRecipes.collectAsState()
    val availability by viewModel.recipeAvailability.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val filterTags by viewModel.filterTags.collectAsState()
    
    var showSortMenu by remember { mutableStateOf(false) }
    var sortHeaderWidth by remember { mutableIntStateOf(0) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var filterHeaderWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    var recipeToEdit by remember { mutableStateOf<RecipeWithIngredients?>(null) }

    val recipeTagOptions = listOf(
        "obiad" to "Obiad",
        "śniadanie" to "Śniadanie",
        "kolacja" to "Kolacja",
        "deser" to "Deser",
        "przekąska" to "Przekąska",
        "sałatka" to "Sałatka",
        "zupa" to "Zupa"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Sort & Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Sort Box
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .onSizeChanged { sortHeaderWidth = it.width }
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Sortowanie", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(with(density) { sortHeaderWidth.toDp() })
                    ) {
                        listOf("DATE" to "Data", "NAME" to "Nazwa", "AVAILABILITY" to "Dostępność").forEach { (type, label) ->
                            val isSelected = when(type) {
                                "NAME" -> sortOrder == RecipeSortOrder.NAME_ASC || sortOrder == RecipeSortOrder.NAME_DESC
                                "DATE" -> sortOrder == RecipeSortOrder.DATE_ASC || sortOrder == RecipeSortOrder.DATE_DESC
                                "AVAILABILITY" -> sortOrder == RecipeSortOrder.AVAILABILITY_DESC
                                else -> false
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.toggleSortOrder(type)
                                    showSortMenu = false
                                },
                                modifier = Modifier.background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Filter Box
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .onSizeChanged { filterHeaderWidth = it.width }
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFilterMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Filtrowanie", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(with(density) { filterHeaderWidth.toDp() })
                    ) {
                        DropdownMenuItem(
                            text = { Text("Wszystkie") },
                            onClick = { viewModel.toggleFilterTag(""); showFilterMenu = false },
                            modifier = Modifier.background(if (filterTags.isEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                        )
                        recipeTagOptions.forEach { (tag, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { viewModel.toggleFilterTag(tag); showFilterMenu = false },
                                modifier = Modifier.background(if (filterTags.contains(tag)) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                            )
                        }
                    }
                }
            }

            if (recipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak przepisów", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recipes, key = { it.recipe.id }) { recipe ->
                        val percent = availability[recipe.recipe.id.toString()] ?: 0
                        RecipeItem(
                            recipe = recipe,
                            availabilityPercent = percent,
                            onDelete = { viewModel.deleteRecipe(recipe.recipe) },
                            onEdit = { recipeToEdit = recipe }
                        )
                    }
                }
            }
        }
    }

    recipeToEdit?.let { rwI ->
        AddRecipeDialog(
            initialRecipe = rwI,
            onDismiss = { recipeToEdit = null },
            onConfirm = { name, instructions, imageUrl, tag, ingredients ->
                viewModel.updateRecipe(
                    rwI.recipe.copy(
                        name = name,
                        instructions = instructions,
                        imageUrl = imageUrl,
                        tag = tag
                    ),
                    ingredients
                )
            }
        )
    }
}

@Composable
fun RecipeItem(
    recipe: RecipeWithIngredients,
    availabilityPercent: Int,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (recipe.recipe.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = recipe.recipe.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f))
                    }
                }
                
                // Tag Overlay
                if (recipe.recipe.tag.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = recipe.recipe.tag,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 300f
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipe.recipe.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Surface(
                        color = when {
                            availabilityPercent >= 100 -> Color(0xFF4CAF50)
                            availabilityPercent > 50 -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "$availabilityPercent%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null || onEdit != null) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opcje", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                if (onEdit != null) {
                                    DropdownMenuItem(
                                        text = { Text("Edytuj") },
                                        onClick = {
                                            showMenu = false
                                            onEdit()
                                        }
                                    )
                                }
                                if (onDelete != null) {
                                    DropdownMenuItem(
                                        text = { Text("Usuń", color = Color.Red) },
                                        onClick = {
                                            showMenu = false
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (expanded) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(text = "Składniki:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    recipe.ingredients.forEach { ingredient ->
                        Row(modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "• ${ingredient.name}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${if (ingredient.amount % 1.0 == 0.0) ingredient.amount.toInt() else ingredient.amount} ${ingredient.unit}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Przygotowanie:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = recipe.recipe.instructions, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
