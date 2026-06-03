package com.example.exchangeapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val item1Id: String,
    val item2Id: String,
    val user1Id: String,
    val user2Id: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?
)
