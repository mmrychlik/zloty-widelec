package com.example.zlotywidelec.data.local.entity

import androidx.room.Entity

/**
 * Entity representing a product suggestion used for autocomplete.
 */
@Entity(tableName = "product_suggestions", primaryKeys = ["name", "defaultUnit"])
data class ProductSuggestionEntity(
    val name: String,
    val defaultUnit: String,
    val tag: String = ""
)
