package com.example.exchangeapp.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "user_interactions",
    primaryKeys = ["userId", "itemId"]
)
data class UserInteractionEntity(
    val userId: String,
    val itemId: String,
    val clickCount: Int,
    val isFavorite: Boolean,
    val lastInteractionTime: Long
)
