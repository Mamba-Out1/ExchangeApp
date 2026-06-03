package com.example.exchangeapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val estimatedPrice: Double,
    val images: String, // JSON序列化的图片URL列表
    val tags: String, // JSON序列化的标签列表
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)
