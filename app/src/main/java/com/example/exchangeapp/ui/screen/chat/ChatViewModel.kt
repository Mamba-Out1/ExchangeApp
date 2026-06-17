package com.example.exchangeapp.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.ChatMessage
import com.example.exchangeapp.domain.model.MessageType
import com.example.exchangeapp.domain.repository.ChatRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.usecase.MarkMessagesAsReadUseCase
import com.example.exchangeapp.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 聊天界面的ViewModel
 *
 * 观察聊天消息(使用Flow)，发送文字消息(调用SendMessageUseCase)，
 * 发送图片消息，标记消息已读(调用MarkMessagesAsReadUseCase)
 *
 * **Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8**
 *
 * Requirements:
 * - 9.1: THE App SHALL提供聊天界面
 * - 9.2: WHEN User点击联系卖家, THE App SHALL打开与该User的聊天窗口
 * - 9.3: THE App SHALL显示Chat_Record的历史消息
 * - 9.4: THE App SHALL允许User发送文字消息
 * - 9.5: THE App SHALL允许User发送图片消息
 * - 9.6: WHEN User发送消息, THE App SHALL在1秒内显示消息已发送状态
 * - 9.7: THE App SHALL按时间顺序显示Chat_Record
 * - 9.8: THE App SHALL显示未读消息数量标记
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var conversationId: String? = null
    private var otherUserId: String? = null
    private var messageObservationJob: Job? = null

    /**
     * 初始化聊天会话
     *
     * @param otherUserId 聊天对方的用户ID
     */
    fun initializeConversation(otherUserId: String) {
        this.otherUserId = otherUserId
        val currentUserId = currentUserProvider.getCurrentUserId()
        
        if (currentUserId == null) {
            _chatState.value = ChatState.Error("用户未登录")
            return
        }

        // 生成会话ID (遵循ChatRepositoryImpl中的格式)
        conversationId = generateConversationId(currentUserId, otherUserId)
        
        // 加载历史消息
        loadHistoricalMessages()
        
        // 开始观察聊天消息
        startObservingMessages()
        
        // 标记消息为已读
        markMessagesAsRead()
    }

    /**
     * 更新消息输入
     */
    fun updateMessageInput(input: String) {
        _messageInput.value = input
    }

    /**
     * 发送文字消息
     */
    fun sendTextMessage() {
        val content = _messageInput.value.trim()
        if (content.isEmpty()) {
            _chatState.value = ChatState.Error("消息内容不能为空")
            return
        }

        val currentUserId = currentUserProvider.getCurrentUserId()
        val otherId = otherUserId
        val conversationId = this.conversationId

        if (currentUserId == null || otherId == null || conversationId == null) {
            _chatState.value = ChatState.Error("聊天会话未初始化")
            return
        }

        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = currentUserId,
            receiverId = otherId,
            content = content,
            messageType = MessageType.TEXT,
            imageUrl = null,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        sendMessage(message)
    }

    /**
     * 发送图片消息
     *
     * @param imageUrl 图片URL或base64编码
     */
    fun sendImageMessage(imageUrl: String) {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val otherId = otherUserId
        val conversationId = this.conversationId

        if (currentUserId == null || otherId == null || conversationId == null) {
            _chatState.value = ChatState.Error("聊天会话未初始化")
            return
        }

        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = currentUserId,
            receiverId = otherId,
            content = "图片消息",
            messageType = MessageType.IMAGE,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        sendMessage(message)
    }

    /**
     * 标记消息为已读
     */
    fun markMessagesAsRead() {
        val conversationId = this.conversationId ?: return

        viewModelScope.launch {
            try {
                markMessagesAsReadUseCase(conversationId).fold(
                    onSuccess = {
                        // 消息已成功标记为已读
                        updateUnreadCount()
                    },
                    onFailure = { error ->
                        // 标记已读失败，记录错误但不影响用户体验
                        println("标记消息已读失败: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                println("标记消息已读异常: ${e.message}")
            }
        }
    }

    /**
     * 获取未读消息数量
     */
    private fun updateUnreadCount() {
        val messages = _messages.value
        val currentUserId = currentUserProvider.getCurrentUserId()
        
        if (currentUserId != null) {
            val unread = messages.count { message ->
                message.receiverId == currentUserId && !message.isRead
            }
            _unreadCount.value = unread
        }
    }

    /**
     * 加载历史消息
     */
    private fun loadHistoricalMessages() {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val otherId = otherUserId ?: return

        if (currentUserId == null) return

        viewModelScope.launch {
            try {
                val historicalMessages = chatRepository.getConversation(currentUserId, otherId)
                // 按时间顺序排序消息（符合要求9.7）
                _messages.update { currentMessages ->
                    // 合并现有消息和历史消息，去重
                    (currentMessages + historicalMessages)
                        .distinctBy { it.id }
                        .sortedBy { it.timestamp }
                }
            } catch (e: Exception) {
                // 历史消息加载失败不影响实时观察
                println("加载历史消息失败: ${e.message}")
            }
        }
    }

    /**
     * 开始观察聊天消息
     */
    private fun startObservingMessages() {
        val conversationId = this.conversationId ?: return

        // 停止之前的观察
        messageObservationJob?.cancel()

        messageObservationJob = viewModelScope.launch {
            try {
                _chatState.value = ChatState.Loading
                
                // 使用ChatRepository观察消息流
                chatRepository.observeConversation(conversationId).collectLatest { messages ->
                    // 按时间顺序排序消息（符合要求9.7）
                    _messages.value = messages.sortedBy { it.timestamp }
                    updateUnreadCount()
                    _chatState.value = ChatState.Success
                }
            } catch (e: Exception) {
                _chatState.value = ChatState.Error("加载消息失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 发送消息到服务器
     */
    private fun sendMessage(message: ChatMessage) {
        _chatState.value = ChatState.Sending

        viewModelScope.launch {
            try {
                // 先添加到本地列表以实现即时反馈（符合要求9.6：在1秒内显示消息已发送状态）
                _messages.update { currentMessages ->
                    currentMessages + message
                }

                // 调用UseCase发送消息
                val result = sendMessageUseCase(message)
                
                result.fold(
                    onSuccess = {
                        // 消息发送成功
                        _chatState.value = ChatState.Success
                        _messageInput.value = "" // 清空输入框
                    },
                    onFailure = { error ->
                        // 消息发送失败，显示错误但保持消息在列表中
                        _chatState.value = ChatState.Error("消息发送失败: ${error.message ?: "未知错误"}")
                    }
                )
            } catch (e: Exception) {
                _chatState.value = ChatState.Error("发送消息异常: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 根据两个用户ID生成会话ID
     *
     * 遵循ChatRepositoryImpl中的格式，确保(a, b)与(b, a)得到相同的会话标识
     */
    private fun generateConversationId(userId1: String, userId2: String): String {
        val sorted = listOf(userId1, userId2).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _chatState.value = ChatState.Idle
    }

    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        messageObservationJob?.cancel()
    }
}

/**
 * 聊天状态密封类
 */
sealed class ChatState {
    /**
     * 空闲状态，初始状态
     */
    object Idle : ChatState()

    /**
     * 加载中状态，表示正在加载聊天记录
     */
    object Loading : ChatState()

    /**
     * 消息发送中状态
     */
    object Sending : ChatState()

    /**
     * 成功状态
     */
    object Success : ChatState()

    /**
     * 错误状态，包含错误信息
     */
    data class Error(val message: String) : ChatState()
}
