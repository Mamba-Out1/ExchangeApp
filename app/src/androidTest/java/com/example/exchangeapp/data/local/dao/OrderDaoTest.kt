package com.example.exchangeapp.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.exchangeapp.data.local.database.AppDatabase
import com.example.exchangeapp.data.local.entity.OrderEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented test for OrderDao
 * Tests order management operations
 */
@RunWith(AndroidJUnit4::class)
class OrderDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var orderDao: OrderDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        orderDao = database.orderDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertOrder_andGetById_returnsCorrectOrder() = runTest {
        // Given
        val order = OrderEntity(
            id = "order1",
            item1Id = "item1",
            item2Id = "item2",
            user1Id = "user1",
            user2Id = "user2",
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            completedAt = null
        )
        
        // When
        orderDao.insertOrder(order)
        val retrieved = orderDao.getOrderById("order1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(order.id, retrieved?.id)
        assertEquals(order.user1Id, retrieved?.user1Id)
        assertEquals(order.user2Id, retrieved?.user2Id)
        assertEquals(order.status, retrieved?.status)
    }
    
    @Test
    fun getOrdersByUserId_returnsAllUserOrders() = runTest {
        // Given
        val order1 = createTestOrder("order1", "user1", "user2")
        val order2 = createTestOrder("order2", "user2", "user1")
        val order3 = createTestOrder("order3", "user3", "user4")
        
        // When
        orderDao.insertOrder(order1)
        orderDao.insertOrder(order2)
        orderDao.insertOrder(order3)
        val user1Orders = orderDao.getOrdersByUserId("user1")
        
        // Then
        assertEquals(2, user1Orders.size)
        assertTrue(user1Orders.any { it.user1Id == "user1" || it.user2Id == "user1" })
        assertFalse(user1Orders.any { it.id == "order3" })
    }
    
    @Test
    fun updateOrder_modifiesExistingOrder() = runTest {
        // Given
        val order = createTestOrder("order1", "user1", "user2", status = "PENDING")
        orderDao.insertOrder(order)
        
        // When
        val completedTime = System.currentTimeMillis()
        val updatedOrder = order.copy(
            status = "COMPLETED",
            completedAt = completedTime,
            updatedAt = completedTime
        )
        orderDao.updateOrder(updatedOrder)
        val retrieved = orderDao.getOrderById("order1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals("COMPLETED", retrieved?.status)
        assertNotNull(retrieved?.completedAt)
        assertEquals(completedTime, retrieved?.completedAt)
    }
    
    @Test
    fun getOrdersByUserId_orderedByCreatedAtDesc() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val order1 = createTestOrder("order1", "user1", "user2", createdAt = now - 3000)
        val order2 = createTestOrder("order2", "user1", "user2", createdAt = now - 2000)
        val order3 = createTestOrder("order3", "user1", "user2", createdAt = now - 1000)
        
        // When
        orderDao.insertOrder(order1)
        orderDao.insertOrder(order2)
        orderDao.insertOrder(order3)
        val orders = orderDao.getOrdersByUserId("user1")
        
        // Then
        assertEquals(3, orders.size)
        // Should be in descending order (newest first)
        assertEquals("order3", orders[0].id)
        assertEquals("order2", orders[1].id)
        assertEquals("order1", orders[2].id)
    }
    
    @Test
    fun insertOrder_withConflict_replacesExistingOrder() = runTest {
        // Given
        val order1 = createTestOrder("order1", "user1", "user2", status = "PENDING")
        val order2 = createTestOrder("order1", "user1", "user2", status = "IN_PROGRESS")
        
        // When
        orderDao.insertOrder(order1)
        orderDao.insertOrder(order2)
        val retrieved = orderDao.getOrderById("order1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals("IN_PROGRESS", retrieved?.status)
    }
    
    @Test
    fun getOrderById_withNonexistentId_returnsNull() = runTest {
        // When
        val retrieved = orderDao.getOrderById("nonexistent")
        
        // Then
        assertNull(retrieved)
    }
    
    private fun createTestOrder(
        id: String,
        user1Id: String,
        user2Id: String,
        status: String = "PENDING",
        createdAt: Long = System.currentTimeMillis()
    ): OrderEntity {
        return OrderEntity(
            id = id,
            item1Id = "item1",
            item2Id = "item2",
            user1Id = user1Id,
            user2Id = user2Id,
            status = status,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis(),
            completedAt = null
        )
    }
}
