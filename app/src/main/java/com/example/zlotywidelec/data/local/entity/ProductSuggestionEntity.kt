package com.example.zlotywidelec.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_suggestions")
data class ProductSuggestionEntity(
    @PrimaryKey
    val name: String,
    val defaultUnit: String
)
