package com.example.exchangeapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["status", "createdAt"])
    ]
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val estimatedPrice: Double,
    val images: String,
    val tags: String,
    val wantedItemName: String = "",
    val wantedTags: String = "[]",
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)
