package com.example.exchangeapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val phone: String,
    val nickname: String,
    val avatar: String?,
    val campusLocation: String,
    val createdAt: Long
)
