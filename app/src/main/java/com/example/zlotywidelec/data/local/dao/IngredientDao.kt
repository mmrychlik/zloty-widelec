package com.example.zlotywidelec.data.local.dao

import androidx.room.*
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients WHERE isInFridge = 0 ORDER BY isChecked ASC, id DESC")
    fun getShoppingListItems(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE isInFridge = 1 ORDER BY name ASC")
    fun getFridgeItems(): Flow<List<IngredientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: IngredientEntity)

    @Update
    suspend fun updateIngredient(ingredient: IngredientEntity)

    @Delete
    suspend fun deleteIngredient(ingredient: IngredientEntity)

    @Query("UPDATE ingredients SET isInFridge = 1, isChecked = 0, addedAt = :timestamp WHERE isChecked = 1 AND isInFridge = 0")
    suspend fun moveCheckedToFridge(timestamp: Long = System.currentTimeMillis())

    @Query("SELECT DISTINCT name, unit, tag FROM ingredients ORDER BY name ASC")
    fun getAllUniqueIngredients(): Flow<List<IngredientNameAndUnit>>

    @Query("SELECT name, defaultUnit as unit, tag FROM product_suggestions ORDER BY name ASC")
    fun getAllProductSuggestions(): Flow<List<IngredientNameAndUnit>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProductSuggestion(suggestion: ProductSuggestionEntity)

    @Query("DELETE FROM product_suggestions")
    suspend fun deleteAllProductSuggestions()

    @Query("DELETE FROM ingredients WHERE isInFridge = 0")
    suspend fun deleteAllShoppingItems()

    @Query("DELETE FROM ingredients WHERE isInFridge = 1")
    suspend fun deleteAllFridgeItems()

    @Query("SELECT * FROM ingredients WHERE isInFridge = 0")
    suspend fun getAllShoppingItemsSync(): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE isInFridge = 1")
    suspend fun getAllFridgeItemsSync(): List<IngredientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)
}

data class IngredientNameAndUnit(
    val name: String,
    val unit: String,
    val tag: String = ""
)
