package com.example.exchangeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.exchangeapp.data.local.entity.ItemEntity

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE status = 'AVAILABLE' ORDER BY createdAt DESC")
    suspend fun getAllAvailableItems(): List<ItemEntity>
    
    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemById(itemId: String): ItemEntity?
    
    @Query("SELECT * FROM items WHERE userId = :userId")
    suspend fun getItemsByUserId(userId: String): List<ItemEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)
    
    @Update
    suspend fun updateItem(item: ItemEntity)
    
    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)
    
    @Query("SELECT * FROM items WHERE tags LIKE '%' || :tag || '%' AND status = 'AVAILABLE'")
    suspend fun getItemsByTag(tag: String): List<ItemEntity>
}
