package com.example.exchangeapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.exchangeapp.data.local.dao.ChatDao
import com.example.exchangeapp.data.local.dao.ItemDao
import com.example.exchangeapp.data.local.dao.OrderDao
import com.example.exchangeapp.data.local.dao.UserDao
import com.example.exchangeapp.data.local.dao.UserInteractionDao
import com.example.exchangeapp.data.local.entity.ChatMessageEntity
import com.example.exchangeapp.data.local.entity.ItemEntity
import com.example.exchangeapp.data.local.entity.OrderEntity
import com.example.exchangeapp.data.local.entity.UserEntity
import com.example.exchangeapp.data.local.entity.UserInteractionEntity

@Database(
    entities = [
        ItemEntity::class,
        UserEntity::class,
        OrderEntity::class,
        ChatMessageEntity::class,
        UserInteractionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun userDao(): UserDao
    abstract fun orderDao(): OrderDao
    abstract fun chatDao(): ChatDao
    abstract fun userInteractionDao(): UserInteractionDao
}
