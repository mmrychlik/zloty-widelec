package com.example.zlotywidelec.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.zlotywidelec.ui.theme.BeigeBackground
import com.example.zlotywidelec.ui.theme.DarkText

@Composable
fun RecipesScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BeigeBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "WIP", color = DarkText)
    }
}
