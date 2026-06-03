package com.example.exchangeapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["conversationId", "timestamp"])]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val messageType: String,
    val imageUrl: String?,
    val timestamp: Long,
    val isRead: Boolean
)
