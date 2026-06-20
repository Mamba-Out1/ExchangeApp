package com.example.exchangeapp.ui.screen.chat

import com.example.exchangeapp.domain.model.ChatMessage
import com.example.exchangeapp.domain.model.MessageType
import com.example.exchangeapp.domain.repository.ChatRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.usecase.MarkMessagesAsReadUseCase
import com.example.exchangeapp.domain.usecase.SendMessageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * ChatViewModel单元测试
 *
 * **验证需求: Requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8**
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ChatViewModel
    private lateinit var mockChatRepository: ChatRepository
    private lateinit var mockSendMessageUseCase: SendMessageUseCase
    private lateinit var mockMarkMessagesAsReadUseCase: MarkMessagesAsReadUseCase
    private lateinit var mockCurrentUserProvider: CurrentUserProvider

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockChatRepository = mockk()
        mockSendMessageUseCase = mockk()
        mockMarkMessagesAsReadUseCase = mockk()
        mockCurrentUserProvider = mockk()
        
        viewModel = ChatViewModel(
            chatRepository = mockChatRepository,
            sendMessageUseCase = mockSendMessageUseCase,
            markMessagesAsReadUseCase = mockMarkMessagesAsReadUseCase,
            currentUserProvider = mockCurrentUserProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始状态应为空闲`() {
        // When: ViewModel创建
        // Then: 初始状态应为空闲
        assertTrue(viewModel.chatState.value is ChatState.Idle)
        assertTrue(viewModel.messages.value.isEmpty())
        assertEquals(0, viewModel.unreadCount.value)
        assertEquals("", viewModel.messageInput.value)
    }

    @Test
    fun `当用户未登录时初始化会话应返回错误`() = runTest {
        // Given: 用户未登录
        every { mockCurrentUserProvider.getCurrentUserId() } returns null
        
        // When: 初始化会话
        viewModel.initializeConversation("otherUserId")
        
        // Then: 状态应为错误
        assertTrue(viewModel.chatState.value is ChatState.Error)
        val errorState = viewModel.chatState.value as ChatState.Error
        assertEquals("用户未登录", errorState.message)
    }

    @Test
    fun `当用户已登录时应能初始化会话`() = runTest {
        // Given: 用户已登录
        val currentUserId = "currentUserId"
        val otherUserId = "otherUserId"
        val conversationId = "currentUserId_otherUserId" // 排序后
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns currentUserId
        coEvery { mockChatRepository.getConversation(currentUserId, otherUserId) } returns emptyList()
        coEvery { mockChatRepository.observeConversation(conversationId) } returns flowOf(emptyList())
        coEvery { mockMarkMessagesAsReadUseCase(conversationId) } returns Result.success(Unit)
        
        // When: 初始化会话
        viewModel.initializeConversation(otherUserId)
        
        // Then: 应调用Repository加载历史消息
        coVerify { mockChatRepository.getConversation(currentUserId, otherUserId) }
        
        // 会话ID应按用户ID排序生成
        // 注意: generateConversationId是私有方法，通过初始化流程验证
    }

    @Test
    fun `发送文字消息应调用UseCase`() = runTest {
        // Given: 用户已登录并初始化会话
        val currentUserId = "currentUserId"
        val otherUserId = "otherUserId"
        val messageContent = "测试消息"
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns currentUserId
        coEvery { mockChatRepository.getConversation(currentUserId, otherUserId) } returns emptyList()
        coEvery { mockChatRepository.observeConversation(any()) } returns flowOf(emptyList())
        coEvery { mockMarkMessagesAsReadUseCase(any()) } returns Result.success(Unit)
        coEvery { mockSendMessageUseCase(any()) } returns Result.success(Unit)
        
        viewModel.initializeConversation(otherUserId)
        
        // 设置消息输入
        viewModel.updateMessageInput(messageContent)
        
        // When: 发送文字消息
        viewModel.sendTextMessage()
        
        // Then: 应调用SendMessageUseCase
        coVerify { mockSendMessageUseCase(any()) }
    }

    @Test
    fun `发送空消息应返回错误`() = runTest {
        // Given: 用户已登录并初始化会话
        val currentUserId = "currentUserId"
        val otherUserId = "otherUserId"
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns currentUserId
        coEvery { mockChatRepository.getConversation(currentUserId, otherUserId) } returns emptyList()
        coEvery { mockChatRepository.observeConversation(any()) } returns flowOf(emptyList())
        coEvery { mockMarkMessagesAsReadUseCase(any()) } returns Result.success(Unit)
        
        viewModel.initializeConversation(otherUserId)
        
        // 清空消息输入
        viewModel.updateMessageInput("")
        
        // When: 发送空消息
        viewModel.sendTextMessage()
        
        // Then: 状态应为错误
        assertTrue(viewModel.chatState.value is ChatState.Error)
        val errorState = viewModel.chatState.value as ChatState.Error
        assertEquals("消息内容不能为空", errorState.message)
    }

    @Test
    fun `发送图片消息应调用UseCase`() = runTest {
        // Given: 用户已登录并初始化会话
        val currentUserId = "currentUserId"
        val otherUserId = "otherUserId"
        val imageUrl = "base64image"
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns currentUserId
        coEvery { mockChatRepository.getConversation(currentUserId, otherUserId) } returns emptyList()
        coEvery { mockChatRepository.observeConversation(any()) } returns flowOf(emptyList())
        coEvery { mockMarkMessagesAsReadUseCase(any()) } returns Result.success(Unit)
        coEvery { mockSendMessageUseCase(any()) } returns Result.success(Unit)
        
        viewModel.initializeConversation(otherUserId)
        
        // When: 发送图片消息
        viewModel.sendImageMessage(imageUrl)
        
        // Then: 应调用SendMessageUseCase
        coVerify { mockSendMessageUseCase(any()) }
    }

    @Test
    fun `标记消息已读应调用UseCase`() = runTest {
        // Given: 用户已登录并初始化会话
        val currentUserId = "currentUserId"
        val otherUserId = "otherUserId"
        val conversationId = "currentUserId_otherUserId"
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns currentUserId
        coEvery { mockChatRepository.getConversation(currentUserId, otherUserId) } returns emptyList()
        coEvery { mockChatRepository.observeConversation(conversationId) } returns flowOf(emptyList())
        coEvery { mockMarkMessagesAsReadUseCase(conversationId) } returns Result.success(Unit)
        
        // When: 初始化会话（会自动标记消息已读）
        viewModel.initializeConversation(otherUserId)
        
        // Then: 应调用MarkMessagesAsReadUseCase
        coVerify { mockMarkMessagesAsReadUseCase(conversationId) }
    }

    @Test
    fun `更新消息输入应更新状态`() = runTest {
        // When: 更新消息输入
        viewModel.updateMessageInput("测试消息")
        
        // Then: 消息输入状态应更新
        assertEquals("测试消息", viewModel.messageInput.value)
    }

    @Test
    fun `重置状态应返回到空闲`() = runTest {
        // Given: 设置错误状态
        viewModel.updateMessageInput("测试消息")
        
        // When: 重置状态
        viewModel.resetState()
        
        // Then: 状态应重置为空闲
        assertTrue(viewModel.chatState.value is ChatState.Idle)
    }

    @Test
    fun `计算未读消息数量`() = runTest {
        // Given: 模拟消息数据
        val currentUserId = "currentUserId"
        val otherUserId = "otherUserId"
        val messages = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = "conversation1",
                senderId = otherUserId,
                receiverId = currentUserId,
                content = "消息1",
                messageType = MessageType.TEXT,
                imageUrl = null,
                timestamp = System.currentTimeMillis(),
                isRead = false
            ),
            ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = "conversation1",
                senderId = currentUserId,
                receiverId = otherUserId,
                content = "消息2",
                messageType = MessageType.TEXT,
                imageUrl = null,
                timestamp = System.currentTimeMillis(),
                isRead = true
            ),
            ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = "conversation1",
                senderId = otherUserId,
                receiverId = currentUserId,
                content = "消息3",
                messageType = MessageType.TEXT,
                imageUrl = null,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns currentUserId
        coEvery { mockChatRepository.getConversation(currentUserId, otherUserId) } returns messages
        coEvery { mockChatRepository.observeConversation(any()) } returns flowOf(messages)
        coEvery { mockMarkMessagesAsReadUseCase(any()) } returns Result.success(Unit)
        
        // When: 初始化会话（会自动更新未读数量）
        viewModel.initializeConversation(otherUserId)
        
        // Then: 未读消息数量应为2（两条来自对方且未读的消息）
        // 注意：由于Flow收集是异步的，我们需要等待一下
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.unreadCount.value)
    }
}
