package com.example.zlotywidelec.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val imageUrl: String = "",
    val instructions: String,
    val tag: String = "",
    val isUserCreated: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
