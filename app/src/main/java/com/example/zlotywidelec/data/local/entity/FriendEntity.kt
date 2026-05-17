package com.example.zlotywidelec.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val email: String,
    val name: String = "",
    val syncRecipes: Boolean = true,
    val syncFridge: Boolean = true,
    val syncShopping: Boolean = true,
    val lastSync: Long = 0
)
