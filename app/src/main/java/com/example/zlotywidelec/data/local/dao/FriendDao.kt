package com.example.zlotywidelec.data.local.dao

import androidx.room.*
import com.example.zlotywidelec.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing friends and their associated information.
 */
@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY email ASC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends")
    suspend fun getFriendsList(): List<FriendEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Update
    suspend fun updateFriend(friend: FriendEntity)

    @Delete
    suspend fun deleteFriend(friend: FriendEntity)

    @Query("SELECT * FROM friends WHERE email = :email LIMIT 1")
    suspend fun getFriendByEmail(email: String): FriendEntity?
}
