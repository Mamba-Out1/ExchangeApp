package com.example.exchangeapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        // 加速 getUserByPhone 的 WHERE phone 查询
        Index(value = ["phone"])
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val phone: String,
    val nickname: String,
    val passwordHash: String?, // 密码哈希值，null表示未设置密码
    val avatar: String?,
    val campusLocation: String,
    val createdAt: Long
)
