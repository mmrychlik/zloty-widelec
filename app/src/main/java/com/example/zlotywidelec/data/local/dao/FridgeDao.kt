package com.example.zlotywidelec.data.local.dao

import androidx.room.*
import com.example.zlotywidelec.data.local.entity.FridgeItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FridgeDao {
    @Query("SELECT * FROM fridge_items")
    fun getAllItems(): Flow<List<FridgeItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FridgeItemEntity)

    @Update
    suspend fun updateItem(item: FridgeItemEntity)

    @Delete
    suspend fun deleteItem(item: FridgeItemEntity)

    @Query("DELETE FROM fridge_items WHERE name = :name")
    suspend fun deleteByName(name: String)
}