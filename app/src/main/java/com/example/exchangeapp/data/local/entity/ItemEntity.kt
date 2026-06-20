package com.example.exchangeapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    indices = [
        // 加速 getItemsByUserId 的 WHERE userId 查询
        Index(value = ["userId"]),
        // 复合索引加速 getAllAvailableItems 的 WHERE status + ORDER BY createdAt，
        // 同时也覆盖 getItemsByTag 中的 status 过滤（最左前缀）
        Index(value = ["status", "createdAt"])
    ]
)
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
