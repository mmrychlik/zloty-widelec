package com.example.zlotywidelec.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val imageUrl: String = "",
    val instructions: String,
    val tag: String = "",
    val isUserCreated: Boolean = false,
    val ownerName: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
