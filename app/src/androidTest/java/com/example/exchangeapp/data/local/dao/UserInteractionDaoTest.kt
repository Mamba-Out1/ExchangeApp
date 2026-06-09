package com.example.exchangeapp.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.exchangeapp.data.local.database.AppDatabase
import com.example.exchangeapp.data.local.entity.UserInteractionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented test for UserInteractionDao
 * Tests user interaction tracking operations
 */
@RunWith(AndroidJUnit4::class)
class UserInteractionDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var userInteractionDao: UserInteractionDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        userInteractionDao = database.userInteractionDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertOrUpdateInteraction_insertsNewInteraction() = runTest {
        // Given
        val interaction = UserInteractionEntity(
            userId = "user1",
            itemId = "item1",
            clickCount = 1,
            isFavorite = false,
            lastInteractionTime = System.currentTimeMillis()
        )
        
        // When
        userInteractionDao.insertOrUpdateInteraction(interaction)
        val retrieved = userInteractionDao.getInteraction("user1", "item1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(interaction.userId, retrieved?.userId)
        assertEquals(interaction.itemId, retrieved?.itemId)
        assertEquals(interaction.clickCount, retrieved?.clickCount)
    }
    
    @Test
    fun getUserInteractions_returnsAllUserInteractions() = runTest {
        // Given
        val interaction1 = createTestInteraction("user1", "item1")
        val interaction2 = createTestInteraction("user1", "item2")
        val interaction3 = createTestInteraction("user2", "item1")
        
        // When
        userInteractionDao.insertOrUpdateInteraction(interaction1)
        userInteractionDao.insertOrUpdateInteraction(interaction2)
        userInteractionDao.insertOrUpdateInteraction(interaction3)
        val user1Interactions = userInteractionDao.getUserInteractions("user1")
        
        // Then
        assertEquals(2, user1Interactions.size)
        assertTrue(user1Interactions.all { it.userId == "user1" })
    }
    
    @Test
    fun incrementClickCount_increasesCount() = runTest {
        // Given
        val interaction = createTestInteraction("user1", "item1", clickCount = 1)
        userInteractionDao.insertOrUpdateInteraction(interaction)
        
        // When
        val newTimestamp = System.currentTimeMillis()
        userInteractionDao.incrementClickCount("user1", "item1", newTimestamp)
        val retrieved = userInteractionDao.getInteraction("user1", "item1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(2, retrieved?.clickCount)
        assertEquals(newTimestamp, retrieved?.lastInteractionTime)
    }
    
    @Test
    fun insertOrUpdateInteraction_withConflict_replacesExisting() = runTest {
        // Given
        val interaction1 = createTestInteraction("user1", "item1", clickCount = 1, isFavorite = false)
        val interaction2 = createTestInteraction("user1", "item1", clickCount = 5, isFavorite = true)
        
        // When
        userInteractionDao.insertOrUpdateInteraction(interaction1)
        userInteractionDao.insertOrUpdateInteraction(interaction2)
        val retrieved = userInteractionDao.getInteraction("user1", "item1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(5, retrieved?.clickCount)
        assertTrue(retrieved?.isFavorite ?: false)
    }
    
    @Test
    fun getInteraction_withNonexistent_returnsNull() = runTest {
        // When
        val retrieved = userInteractionDao.getInteraction("nonexistent", "nonexistent")
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun incrementClickCount_forNonexistentInteraction_doesNothing() = runTest {
        // When
        userInteractionDao.incrementClickCount("user1", "item1", System.currentTimeMillis())
        val retrieved = userInteractionDao.getInteraction("user1", "item1")
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun insertOrUpdateInteraction_toggleFavorite() = runTest {
        // Given
        val interaction1 = createTestInteraction("user1", "item1", isFavorite = false)
        userInteractionDao.insertOrUpdateInteraction(interaction1)
        
        // When - Toggle favorite to true
        val interaction2 = createTestInteraction("user1", "item1", isFavorite = true)
        userInteractionDao.insertOrUpdateInteraction(interaction2)
        val retrieved1 = userInteractionDao.getInteraction("user1", "item1")
        
        // Then
        assertNotNull(retrieved1)
        assertTrue(retrieved1?.isFavorite ?: false)
        
        // When - Toggle favorite back to false
        val interaction3 = createTestInteraction("user1", "item1", isFavorite = false)
        userInteractionDao.insertOrUpdateInteraction(interaction3)
        val retrieved2 = userInteractionDao.getInteraction("user1", "item1")
        
        // Then
        assertNotNull(retrieved2)
        assertFalse(retrieved2?.isFavorite ?: true)
    }
    
    @Test
    fun multipleIncrements_accumulateClickCount() = runTest {
        // Given
        val interaction = createTestInteraction("user1", "item1", clickCount = 0)
        userInteractionDao.insertOrUpdateInteraction(interaction)
        
        // When
        userInteractionDao.incrementClickCount("user1", "item1", System.currentTimeMillis())
        userInteractionDao.incrementClickCount("user1", "item1", System.currentTimeMillis())
        userInteractionDao.incrementClickCount("user1", "item1", System.currentTimeMillis())
        val retrieved = userInteractionDao.getInteraction("user1", "item1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(3, retrieved?.clickCount)
    }
    
    private fun createTestInteraction(
        userId: String,
        itemId: String,
        clickCount: Int = 0,
        isFavorite: Boolean = false,
        lastInteractionTime: Long = System.currentTimeMillis()
    ): UserInteractionEntity {
        return UserInteractionEntity(
            userId = userId,
            itemId = itemId,
            clickCount = clickCount,
            isFavorite = isFavorite,
            lastInteractionTime = lastInteractionTime
        )
    }
}
