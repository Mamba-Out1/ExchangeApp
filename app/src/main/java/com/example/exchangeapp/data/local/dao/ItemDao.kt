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
    
    // 注意: tags 使用前导通配符 LIKE '%' || :tag || '%'，无法命中索引（全表扫描）。
    // 如需进一步优化标签检索，应改用规范化的标签关联表或 FTS，超出本任务范围，故保持原行为不变。
    // status = 'AVAILABLE' 过滤可由 ItemEntity 上的 (status, createdAt) 复合索引加速。
    @Query("SELECT * FROM items WHERE tags LIKE '%' || :tag || '%' AND status = 'AVAILABLE'")
    suspend fun getItemsByTag(tag: String): List<ItemEntity>
}
