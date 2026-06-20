package com.example.exchangeapp.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.ChatMessage
import com.example.exchangeapp.domain.model.MessageType
import com.example.exchangeapp.domain.repository.ChatRepository
import com.example.exchangeapp.domain.repository.UserRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 会话列表界面的ViewModel
 *
 * 加载当前登录用户的所有会话，每个会话展示对方昵称、最后一条消息预览、
 * 时间以及未读消息数量。点击会话进入与对方的聊天界面。
 *
 * **Validates: Requirements 9.1, 9.3, 9.8**
 *
 * Requirements:
 * - 9.1: THE App SHALL提供聊天界面
 * - 9.3: THE App SHALL显示Chat_Record的历史消息
 * - 9.8: THE App SHALL显示未读消息数量标记
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatListState>(ChatListState.Loading)
    val uiState: StateFlow<ChatListState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    /**
     * 加载当前用户的会话列表
     */
    private fun loadConversations() {
        val currentUserId = currentUserProvider.getCurrentUserId()
        if (currentUserId == null) {
            _uiState.value = ChatListState.Error("用户未登录")
            return
        }

        _uiState.value = ChatListState.Loading
        viewModelScope.launch {
            try {
                val latestMessages = chatRepository.getConversationsForUser(currentUserId)
                if (latestMessages.isEmpty()) {
                    _uiState.value = ChatListState.Empty
                    return@launch
                }

                val summaries = latestMessages.map { message ->
                    // 对方用户ID：若当前用户是发送者，则对方为接收者，反之亦然
                    val otherUserId = if (message.senderId == currentUserId) {
                        message.receiverId
                    } else {
                        message.senderId
                    }

                    // 解析对方昵称，无法获取时回退展示
                    val otherUser = runCatching { userRepository.getUserById(otherUserId) }.getOrNull()
                    val otherNickname = otherUser?.nickname?.takeIf { it.isNotBlank() }
                        ?: otherUserId.takeIf { it.isNotBlank() }
                        ?: "用户"

                    // 未读消息数量
                    val unreadCount = runCatching {
                        chatRepository.getUnreadCount(message.conversationId, currentUserId)
                    }.getOrDefault(0)

                    ConversationSummary(
                        otherUserId = otherUserId,
                        otherNickname = otherNickname,
                        lastMessage = buildPreview(message),
                        timestamp = message.timestamp,
                        unreadCount = unreadCount
                    )
                }

                _uiState.value = ChatListState.Success(summaries)
            } catch (e: Exception) {
                _uiState.value = ChatListState.Error("加载会话失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 刷新会话列表
     */
    fun refresh() {
        loadConversations()
    }

    /**
     * 构建会话最后一条消息的预览文本，图片消息以"[图片]"占位。
     */
    private fun buildPreview(message: ChatMessage): String {
        return when (message.messageType) {
            MessageType.IMAGE -> "[图片]"
            MessageType.TEXT -> message.content
        }
    }
}

/**
 * 会话摘要数据类，用于会话列表的单行展示。
 *
 * @param otherUserId 对方用户ID，点击会话时用于进入聊天界面
 * @param otherNickname 对方昵称
 * @param lastMessage 最后一条消息预览
 * @param timestamp 最后一条消息的时间戳
 * @param unreadCount 未读消息数量
 */
data class ConversationSummary(
    val otherUserId: String,
    val otherNickname: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int
)

/**
 * 会话列表状态密封类
 */
sealed class ChatListState {
    /**
     * 加载中状态
     */
    object Loading : ChatListState()

    /**
     * 空状态，表示当前用户没有任何会话
     */
    object Empty : ChatListState()

    /**
     * 成功状态，包含会话摘要列表
     */
    data class Success(val conversations: List<ConversationSummary>) : ChatListState()

    /**
     * 错误状态，包含错误信息
     */
    data class Error(val message: String) : ChatListState()
}
