package com.example.exchangeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.exchangeapp.data.local.entity.UserInteractionEntity

@Dao
interface UserInteractionDao {
    @Query("SELECT * FROM user_interactions WHERE userId = :userId")
    suspend fun getUserInteractions(userId: String): List<UserInteractionEntity>
    
    @Query("SELECT * FROM user_interactions WHERE userId = :userId AND itemId = :itemId")
    suspend fun getInteraction(userId: String, itemId: String): UserInteractionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateInteraction(interaction: UserInteractionEntity)
    
    @Query("UPDATE user_interactions SET clickCount = clickCount + 1, lastInteractionTime = :timestamp WHERE userId = :userId AND itemId = :itemId")
    suspend fun incrementClickCount(userId: String, itemId: String, timestamp: Long)
}
