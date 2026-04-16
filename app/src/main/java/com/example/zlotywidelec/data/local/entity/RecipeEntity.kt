package com.example.zlotywidelec.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val ingredients: String, // Stored as a simple string or JSON for now
    val instructions: String,
    val isFavorite: Boolean = false,
    val imageUrl: String? = null
)