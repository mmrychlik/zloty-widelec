package com.example.zlotywidelec.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Entity representing an ingredient in the shopping list or fridge.
 */
@Serializable
@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val unit: String = "",
    val tag: String = "",
    val isChecked: Boolean = false,
    val isInFridge: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
