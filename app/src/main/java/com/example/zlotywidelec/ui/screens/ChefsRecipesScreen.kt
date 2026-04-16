package com.example.zlotywidelec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.ui.theme.BeigeBackground
import com.example.zlotywidelec.ui.theme.DarkText
import com.example.zlotywidelec.ui.viewmodel.RecipeViewModel

@Composable
fun ChefsRecipesScreen(
    viewModel: RecipeViewModel,
    onRecipeClick: (RecipeEntity) -> Unit
) {
    val recipes = viewModel.chefsRecipes

    Scaffold(
        containerColor = BeigeBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recipes) { recipe ->
                    ChefRecipeRow(recipe = recipe, onClick = { onRecipeClick(recipe) })
                }
            }
        }
    }
}

@Composable
fun ChefRecipeRow(recipe: RecipeEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = DarkText.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = recipe.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkText
                )
                Text(
                    text = recipe.description,
                    fontSize = 14.sp,
                    color = DarkText.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
        }
    }
}
