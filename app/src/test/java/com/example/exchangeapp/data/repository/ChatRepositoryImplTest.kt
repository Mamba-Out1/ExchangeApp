package com.example.exchangeapp.data.repository

import app.cash.turbine.test
import com.example.exchangeapp.data.local.entity.ChatMessageEntity
import com.example.exchangeapp.data.repository.fake.FakeChatDao
import com.example.exchangeapp.domain.model.ChatMessage
import com.example.exchangeapp.domain.model.MessageType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [ChatRepositoryImpl] 单元测试。
 *
 * 使用内存版假 DAO（[FakeChatDao]）验证：
 * - Entity <-> Model 转换的往返正确性（含消息类型枚举）；
 * - 会话标识生成的对称性；
 * - 未知消息类型字符串的默认值回退（回退为 TEXT）；
 * - sendMessage 在底层异常时返回 [Result.failure] 的错误处理路径；
 * - 会话实时观察流。
 *
 * **验证需求: Requirements 2.8**
 */
class ChatRepositoryImplTest {

    private lateinit var dao: FakeChatDao
    private lateinit var repository: ChatRepositoryImpl

    private fun sampleMessage(
        id: String = "msg-1",
        conversationId: String = "user-a_user-b",
        senderId: String = "user-a",
        receiverId: String = "user-b",
        type: MessageType = MessageType.TEXT,
        imageUrl: String? = null,
        timestamp: Long = 100L,
        isRead: Boolean = false
    ) = ChatMessage(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        receiverId = receiverId,
        content = "你好",
        messageType = type,
        imageUrl = imageUrl,
        timestamp = timestamp,
        isRead = isRead
    )

    @BeforeEach
    fun setup() {
        dao = FakeChatDao()
        repository = ChatRepositoryImpl(dao)
    }

    @Test
    fun `send then get conversation returns equal model - round trip conversion`() = runTest {
        val message = sampleMessage(type = MessageType.IMAGE, imageUrl = "pic.png")

        val result = repository.sendMessage(message)
        assertTrue(result.isSuccess)

        val conversation = repository.getConversation("user-a", "user-b")

        assertEquals(listOf(message), conversation)
    }

    @Test
    fun `getConversation is symmetric in user order`() = runTest {
        repository.sendMessage(sampleMessage())

        val ab = repository.getConversation("user-a", "user-b")
        val ba = repository.getConversation("user-b", "user-a")

        assertEquals(ab, ba)
        assertEquals(1, ab.size)
    }

    @Test
    fun `messages are returned ordered by timestamp ascending`() = runTest {
        repository.sendMessage(sampleMessage(id = "late", timestamp = 300L))
        repository.sendMessage(sampleMessage(id = "early", timestamp = 100L))

        val conversation = repository.getConversation("user-a", "user-b")

        assertEquals(listOf("early", "late"), conversation.map { it.id })
    }

    @Test
    fun `unknown message type falls back to TEXT`() = runTest {
        dao.seed(
            ChatMessageEntity(
                id = "weird",
                conversationId = "user-a_user-b",
                senderId = "user-a",
                receiverId = "user-b",
                content = "x",
                messageType = "VOICE_NOT_SUPPORTED",
                imageUrl = null,
                timestamp = 1L,
                isRead = false
            )
        )

        val message = repository.getConversation("user-a", "user-b").first()

        assertEquals(MessageType.TEXT, message.messageType)
    }

    @Test
    fun `markAsRead marks all messages in conversation as read`() = runTest {
        repository.sendMessage(sampleMessage(id = "m1", isRead = false))
        repository.sendMessage(sampleMessage(id = "m2", isRead = false))

        repository.markAsRead("user-a_user-b")

        val conversation = repository.getConversation("user-a", "user-b")
        assertTrue(conversation.all { it.isRead })
    }

    @Test
    fun `sendMessage returns failure when dao throws - error handling path`() = runTest {
        dao.failInserts = true

        val result = repository.sendMessage(sampleMessage())

        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `observeConversation emits updated messages`() = runTest {
        repository.observeConversation("user-a_user-b").test {
            assertEquals(emptyList(), awaitItem())

            repository.sendMessage(sampleMessage(id = "m1"))
            assertEquals(listOf("m1"), awaitItem().map { it.id })

            cancelAndIgnoreRemainingEvents()
        }
    }
}
