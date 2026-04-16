package com.example.zlotywidelec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.ui.theme.BeigeBackground
import com.example.zlotywidelec.ui.theme.DarkText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: RecipeEntity,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.title, color = DarkText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cofnij",
                            tint = DarkText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = BeigeBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Opis",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Text(
                text = recipe.description,
                fontSize = 16.sp,
                color = DarkText.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Składniki",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Text(
                text = recipe.ingredients.ifBlank { "Brak podanych składników." },
                fontSize = 16.sp,
                color = DarkText.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Instrukcje",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Text(
                text = recipe.instructions.ifBlank { "Brak podanych instrukcji." },
                fontSize = 16.sp,
                color = DarkText.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
