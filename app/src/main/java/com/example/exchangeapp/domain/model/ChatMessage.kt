package com.example.exchangeapp.domain.model

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val messageType: MessageType,
    val imageUrl: String?,
    val timestamp: Long,
    val isRead: Boolean
)
