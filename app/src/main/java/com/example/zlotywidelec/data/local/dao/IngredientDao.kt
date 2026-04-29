package com.example.zlotywidelec.data.local.dao

import androidx.room.*
import com.example.zlotywidelec.data.local.entity.IngredientEntity
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
}
