package com.example.exchangeapp.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.exchangeapp.data.local.database.AppDatabase
import com.example.exchangeapp.data.local.entity.ItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented test for ItemDao
 * Tests CRUD operations and queries for items
 */
@RunWith(AndroidJUnit4::class)
class ItemDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var itemDao: ItemDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        itemDao = database.itemDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertItem_andGetById_returnsCorrectItem() = runTest {
        // Given
        val item = ItemEntity(
            id = "item1",
            userId = "user1",
            name = "Test Item",
            description = "Test Description",
            estimatedPrice = 100.0,
            images = "[\"image1.jpg\"]",
            tags = "[\"电子产品\"]",
            latitude = 39.9042,
            longitude = 116.4074,
            address = "Beijing",
            status = "AVAILABLE",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // When
        itemDao.insertItem(item)
        val retrieved = itemDao.getItemById("item1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(item.id, retrieved?.id)
        assertEquals(item.name, retrieved?.name)
        assertEquals(item.userId, retrieved?.userId)
    }
    
    @Test
    fun getAllAvailableItems_returnsOnlyAvailableItems() = runTest {
        // Given
        val availableItem = ItemEntity(
            id = "item1",
            userId = "user1",
            name = "Available Item",
            description = "Available",
            estimatedPrice = 100.0,
            images = "[]",
            tags = "[]",
            latitude = null,
            longitude = null,
            address = null,
            status = "AVAILABLE",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val exchangedItem = ItemEntity(
            id = "item2",
            userId = "user1",
            name = "Exchanged Item",
            description = "Exchanged",
            estimatedPrice = 200.0,
            images = "[]",
            tags = "[]",
            latitude = null,
            longitude = null,
            address = null,
            status = "EXCHANGED",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // When
        itemDao.insertItem(availableItem)
        itemDao.insertItem(exchangedItem)
        val availableItems = itemDao.getAllAvailableItems()
        
        // Then
        assertEquals(1, availableItems.size)
        assertEquals("item1", availableItems[0].id)
        assertEquals("AVAILABLE", availableItems[0].status)
    }
    
    @Test
    fun getItemsByUserId_returnsUserItems() = runTest {
        // Given
        val user1Item1 = createTestItem("item1", "user1")
        val user1Item2 = createTestItem("item2", "user1")
        val user2Item = createTestItem("item3", "user2")
        
        // When
        itemDao.insertItem(user1Item1)
        itemDao.insertItem(user1Item2)
        itemDao.insertItem(user2Item)
        val user1Items = itemDao.getItemsByUserId("user1")
        
        // Then
        assertEquals(2, user1Items.size)
        assertTrue(user1Items.all { it.userId == "user1" })
    }
    
    @Test
    fun updateItem_modifiesExistingItem() = runTest {
        // Given
        val item = createTestItem("item1", "user1")
        itemDao.insertItem(item)
        
        // When
        val updatedItem = item.copy(name = "Updated Name", estimatedPrice = 200.0)
        itemDao.updateItem(updatedItem)
        val retrieved = itemDao.getItemById("item1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved?.name)
        assertEquals(200.0, retrieved?.estimatedPrice, 0.01)
    }
    
    @Test
    fun deleteItem_removesItem() = runTest {
        // Given
        val item = createTestItem("item1", "user1")
        itemDao.insertItem(item)
        
        // When
        itemDao.deleteItem("item1")
        val retrieved = itemDao.getItemById("item1")
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun getItemsByTag_returnsItemsWithTag() = runTest {
        // Given
        val item1 = createTestItem("item1", "user1", tags = "[\"电子产品\",\"手机\"]")
        val item2 = createTestItem("item2", "user1", tags = "[\"书籍\"]")
        val item3 = createTestItem("item3", "user1", tags = "[\"电子产品\"]")
        
        // When
        itemDao.insertItem(item1)
        itemDao.insertItem(item2)
        itemDao.insertItem(item3)
        val electronicsItems = itemDao.getItemsByTag("电子产品")
        
        // Then
        assertEquals(2, electronicsItems.size)
        assertTrue(electronicsItems.all { it.tags.contains("电子产品") })
    }
    
    @Test
    fun insertItem_withConflict_replacesExistingItem() = runTest {
        // Given
        val item1 = createTestItem("item1", "user1", name = "Original Name")
        val item2 = createTestItem("item1", "user1", name = "New Name")
        
        // When
        itemDao.insertItem(item1)
        itemDao.insertItem(item2)
        val allItems = itemDao.getAllAvailableItems()
        val retrieved = itemDao.getItemById("item1")
        
        // Then
        assertEquals(1, allItems.size)
        assertEquals("New Name", retrieved?.name)
    }
    
    private fun createTestItem(
        id: String,
        userId: String,
        name: String = "Test Item",
        tags: String = "[]",
        status: String = "AVAILABLE"
    ): ItemEntity {
        return ItemEntity(
            id = id,
            userId = userId,
            name = name,
            description = "Test Description",
            estimatedPrice = 100.0,
            images = "[]",
            tags = tags,
            latitude = null,
            longitude = null,
            address = null,
            status = status,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
