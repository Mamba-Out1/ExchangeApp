package com.example.exchangeapp.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.exchangeapp.data.local.database.AppDatabase
import com.example.exchangeapp.data.local.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented test for UserDao
 * Tests user CRUD operations
 */
@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        userDao = database.userDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertUser_andGetById_returnsCorrectUser() = runTest {
        // Given
        val user = UserEntity(
            id = "user1",
            phone = "13800138000",
            nickname = "Test User",
            avatar = "avatar.jpg",
            campusLocation = "北京大学",
            createdAt = System.currentTimeMillis()
        )
        
        // When
        userDao.insertUser(user)
        val retrieved = userDao.getUserById("user1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(user.id, retrieved?.id)
        assertEquals(user.phone, retrieved?.phone)
        assertEquals(user.nickname, retrieved?.nickname)
    }
    
    @Test
    fun getUserByPhone_returnsCorrectUser() = runTest {
        // Given
        val user = UserEntity(
            id = "user1",
            phone = "13800138000",
            nickname = "Test User",
            avatar = null,
            campusLocation = "清华大学",
            createdAt = System.currentTimeMillis()
        )
        
        // When
        userDao.insertUser(user)
        val retrieved = userDao.getUserByPhone("13800138000")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(user.id, retrieved?.id)
        assertEquals(user.phone, retrieved?.phone)
    }
    
    @Test
    fun updateUser_modifiesExistingUser() = runTest {
        // Given
        val user = UserEntity(
            id = "user1",
            phone = "13800138000",
            nickname = "Original Name",
            avatar = null,
            campusLocation = "北京大学",
            createdAt = System.currentTimeMillis()
        )
        userDao.insertUser(user)
        
        // When
        val updatedUser = user.copy(nickname = "Updated Name", avatar = "new_avatar.jpg")
        userDao.updateUser(updatedUser)
        val retrieved = userDao.getUserById("user1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved?.nickname)
        assertEquals("new_avatar.jpg", retrieved?.avatar)
    }
    
    @Test
    fun insertUser_withConflict_replacesExistingUser() = runTest {
        // Given
        val user1 = UserEntity(
            id = "user1",
            phone = "13800138000",
            nickname = "Original Name",
            avatar = null,
            campusLocation = "北京大学",
            createdAt = System.currentTimeMillis()
        )
        val user2 = UserEntity(
            id = "user1",
            phone = "13800138000",
            nickname = "New Name",
            avatar = "avatar.jpg",
            campusLocation = "清华大学",
            createdAt = System.currentTimeMillis()
        )
        
        // When
        userDao.insertUser(user1)
        userDao.insertUser(user2)
        val retrieved = userDao.getUserById("user1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals("New Name", retrieved?.nickname)
        assertEquals("清华大学", retrieved?.campusLocation)
    }
    
    @Test
    fun getUserById_withNonexistentId_returnsNull() = runTest {
        // When
        val retrieved = userDao.getUserById("nonexistent")
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun getUserByPhone_withNonexistentPhone_returnsNull() = runTest {
        // When
        val retrieved = userDao.getUserByPhone("99999999999")
        
        // Then
        assertNull(retrieved)
    }
}
