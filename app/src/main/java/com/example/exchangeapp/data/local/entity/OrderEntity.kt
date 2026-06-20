package com.example.exchangeapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    indices = [
        // getOrdersByUserId 使用 WHERE user1Id = :userId OR user2Id = :userId，
        // 分别建立索引使 OR 两侧条件都能命中索引
        Index(value = ["user1Id"]),
        Index(value = ["user2Id"])
    ]
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val item1Id: String,
    val item2Id: String,
    val user1Id: String,
    val user2Id: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val rating: Int? = null
)
