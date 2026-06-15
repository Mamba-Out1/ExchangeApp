package com.example.exchangeapp.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.exchangeapp.data.local.database.AppDatabase
import com.example.exchangeapp.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented test for ChatDao
 * Tests chat message operations and Flow observations
 */
@RunWith(AndroidJUnit4::class)
class ChatDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var chatDao: ChatDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        chatDao = database.chatDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertMessage_andGetByConversationId_returnsMessages() = runTest {
        // Given
        val conversationId = "user1_user2"
        val message = ChatMessageEntity(
            id = "msg1",
            conversationId = conversationId,
            senderId = "user1",
            receiverId = "user2",
            content = "Hello!",
            messageType = "TEXT",
            imageUrl = null,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        
        // When
        chatDao.insertMessage(message)
        val messages = chatDao.getMessagesByConversationId(conversationId)
        
        // Then
        assertEquals(1, messages.size)
        assertEquals(message.id, messages[0].id)
        assertEquals(message.content, messages[0].content)
    }
    
    @Test
    fun getMessagesByConversationId_orderedByTimestampAsc() = runTest {
        // Given
        val conversationId = "user1_user2"
        val now = System.currentTimeMillis()
        val msg1 = createTestMessage("msg1", conversationId, timestamp = now - 3000)
        val msg2 = createTestMessage("msg2", conversationId, timestamp = now - 2000)
        val msg3 = createTestMessage("msg3", conversationId, timestamp = now - 1000)
        
        // When
        chatDao.insertMessage(msg3)
        chatDao.insertMessage(msg1)
        chatDao.insertMessage(msg2)
        val messages = chatDao.getMessagesByConversationId(conversationId)
        
        // Then
        assertEquals(3, messages.size)
        // Should be in ascending order (oldest first)
        assertEquals("msg1", messages[0].id)
        assertEquals("msg2", messages[1].id)
        assertEquals("msg3", messages[2].id)
    }
    
    @Test
    fun observeMessages_returnsFlow() = runTest {
        // Given
        val conversationId = "user1_user2"
        val message = createTestMessage("msg1", conversationId)
        
        // When
        chatDao.insertMessage(message)
        val messagesFlow = chatDao.observeMessages(conversationId)
        val messages = messagesFlow.first()
        
        // Then
        assertEquals(1, messages.size)
        assertEquals(message.id, messages[0].id)
    }
    
    @Test
    fun markMessagesAsRead_updatesIsReadFlag() = runTest {
        // Given
        val conversationId = "user1_user2"
        val msg1 = createTestMessage("msg1", conversationId, isRead = false)
        val msg2 = createTestMessage("msg2", conversationId, isRead = false)
        
        // When
        chatDao.insertMessage(msg1)
        chatDao.insertMessage(msg2)
        chatDao.markMessagesAsRead(conversationId)
        val messages = chatDao.getMessagesByConversationId(conversationId)
        
        // Then
        assertEquals(2, messages.size)
        assertTrue(messages.all { it.isRead })
    }
    
    @Test
    fun getUnreadCount_returnsCorrectCount() = runTest {
        // Given
        val userId = "user1"
        val msg1 = createTestMessage("msg1", "conv1", receiverId = userId, isRead = false)
        val msg2 = createTestMessage("msg2", "conv2", receiverId = userId, isRead = false)
        val msg3 = createTestMessage("msg3", "conv3", receiverId = userId, isRead = true)
        val msg4 = createTestMessage("msg4", "conv4", receiverId = "user2", isRead = false)
        
        // When
        chatDao.insertMessage(msg1)
        chatDao.insertMessage(msg2)
        chatDao.insertMessage(msg3)
        chatDao.insertMessage(msg4)
        val unreadCount = chatDao.getUnreadCount(userId).first()
        
        // Then
        assertEquals(2, unreadCount)
    }
    
    @Test
    fun insertMessage_withImageType_storesCorrectly() = runTest {
        // Given
        val conversationId = "user1_user2"
        val imageMessage = ChatMessageEntity(
            id = "msg1",
            conversationId = conversationId,
            senderId = "user1",
            receiverId = "user2",
            content = "Image message",
            messageType = "IMAGE",
            imageUrl = "https://example.com/image.jpg",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        
        // When
        chatDao.insertMessage(imageMessage)
        val messages = chatDao.getMessagesByConversationId(conversationId)
        
        // Then
        assertEquals(1, messages.size)
        assertEquals("IMAGE", messages[0].messageType)
        assertNotNull(messages[0].imageUrl)
        assertEquals("https://example.com/image.jpg", messages[0].imageUrl)
    }
    
    @Test
    fun insertMessage_withConflict_replacesExistingMessage() = runTest {
        // Given
        val conversationId = "user1_user2"
        val msg1 = createTestMessage("msg1", conversationId, content = "Original")
        val msg2 = createTestMessage("msg1", conversationId, content = "Updated")
        
        // When
        chatDao.insertMessage(msg1)
        chatDao.insertMessage(msg2)
        val messages = chatDao.getMessagesByConversationId(conversationId)
        
        // Then
        assertEquals(1, messages.size)
        assertEquals("Updated", messages[0].content)
    }
    
    private fun createTestMessage(
        id: String,
        conversationId: String,
        senderId: String = "user1",
        receiverId: String = "user2",
        content: String = "Test message",
        messageType: String = "TEXT",
        timestamp: Long = System.currentTimeMillis(),
        isRead: Boolean = false
    ): ChatMessageEntity {
        return ChatMessageEntity(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            messageType = messageType,
            imageUrl = null,
            timestamp = timestamp,
            isRead = isRead
        )
    }
}
