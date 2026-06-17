package com.example.exchangeapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [
        // 加速按会话拉取消息并按时间排序的查询
        Index(value = ["conversationId", "timestamp"]),
        // 复合索引加速 getUnreadCount 的 WHERE receiverId + isRead 未读计数查询
        Index(value = ["receiverId", "isRead"])
    ]
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
