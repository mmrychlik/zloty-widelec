package com.example.zlotywidelec.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fridge_items")
data class FridgeItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: String,
    val expirationDate: Long? = null,
    val isFromSmartFridge: Boolean = false
)