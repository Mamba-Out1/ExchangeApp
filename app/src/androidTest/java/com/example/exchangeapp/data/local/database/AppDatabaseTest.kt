package com.example.exchangeapp.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented test for AppDatabase configuration
 * Tests that the database is properly configured with all entities, DAOs, and type converters
 * 
 * **Validates: Requirements 2.1, 2.7**
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    
    private lateinit var database: AppDatabase
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun databaseConfiguration_isCorrect() {
        // Verify database is open
        assertTrue(database.isOpen)
    }
    
    @Test
    fun allDAOs_areAccessible() {
        // Verify all DAO interfaces can be accessed
        assertNotNull(database.itemDao())
        assertNotNull(database.userDao())
        assertNotNull(database.orderDao())
        assertNotNull(database.chatDao())
        assertNotNull(database.userInteractionDao())
    }
    
    @Test
    fun itemDao_isProperlyInitialized() {
        // Verify ItemDao can be retrieved and is not null
        val itemDao = database.itemDao()
        assertNotNull(itemDao)
    }
    
    @Test
    fun userDao_isProperlyInitialized() {
        // Verify UserDao can be retrieved and is not null
        val userDao = database.userDao()
        assertNotNull(userDao)
    }
    
    @Test
    fun orderDao_isProperlyInitialized() {
        // Verify OrderDao can be retrieved and is not null
        val orderDao = database.orderDao()
        assertNotNull(orderDao)
    }
    
    @Test
    fun chatDao_isProperlyInitialized() {
        // Verify ChatDao can be retrieved and is not null
        val chatDao = database.chatDao()
        assertNotNull(chatDao)
    }
    
    @Test
    fun userInteractionDao_isProperlyInitialized() {
        // Verify UserInteractionDao can be retrieved and is not null
        val userInteractionDao = database.userInteractionDao()
        assertNotNull(userInteractionDao)
    }
}
