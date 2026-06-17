package com.example.exchangeapp.ui.screen.chat

import com.example.exchangeapp.domain.model.ChatMessage

/**
 * ChatScreen的UI状态类
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentMessage: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val otherUserId: String = "",
    val otherUserName: String = ""
)