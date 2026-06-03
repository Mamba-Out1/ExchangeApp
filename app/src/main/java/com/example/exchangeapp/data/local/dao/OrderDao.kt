package com.example.exchangeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.exchangeapp.data.local.entity.OrderEntity

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE user1Id = :userId OR user2Id = :userId ORDER BY createdAt DESC")
    suspend fun getOrdersByUserId(userId: String): List<OrderEntity>
    
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)
    
    @Update
    suspend fun updateOrder(order: OrderEntity)
}
