package com.example.exchangeapp.di

import android.content.Context
import androidx.room.Room
import com.example.exchangeapp.data.local.dao.ChatDao
import com.example.exchangeapp.data.local.dao.ItemDao
import com.example.exchangeapp.data.local.dao.OrderDao
import com.example.exchangeapp.data.local.dao.UserDao
import com.example.exchangeapp.data.local.dao.UserInteractionDao
import com.example.exchangeapp.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库模块
 * 提供Room数据库和所有DAO的依赖注入
 * 
 * **验证需求: Requirements 2.1, 2.2, 2.3, 2.4, 2.7, 13.1**
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * 提供AppDatabase单例实例
     * 
     * 配置:
     * - 数据库名称: exchange_app.db
     * - 版本: 1
     * - 不导出Schema以提高性能
     * 
     * @param context 应用上下文
     * @return AppDatabase实例
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
            name = "exchange_app.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    /**
     * 提供ItemDao实例
     * 
     * 用于物品数据的CRUD操作
     * 
     * @param database AppDatabase实例
     * @return ItemDao实例
     */
    @Provides
    @Singleton
    fun provideItemDao(database: AppDatabase): ItemDao {
        return database.itemDao()
    }
    
    /**
     * 提供UserDao实例
     * 
     * 用于用户数据的CRUD操作
     * 
     * @param database AppDatabase实例
     * @return UserDao实例
     */
    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
    
    /**
     * 提供OrderDao实例
     * 
     * 用于订单数据的CRUD操作
     * 
     * @param database AppDatabase实例
     * @return OrderDao实例
     */
    @Provides
    @Singleton
    fun provideOrderDao(database: AppDatabase): OrderDao {
        return database.orderDao()
    }
    
    /**
     * 提供ChatDao实例
     * 
     * 用于聊天消息数据的CRUD操作
     * 
     * @param database AppDatabase实例
     * @return ChatDao实例
     */
    @Provides
    @Singleton
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }
    
    /**
     * 提供UserInteractionDao实例
     * 
     * 用于用户交互记录数据的CRUD操作
     * 
     * @param database AppDatabase实例
     * @return UserInteractionDao实例
     */
    @Provides
    @Singleton
    fun provideUserInteractionDao(database: AppDatabase): UserInteractionDao {
        return database.userInteractionDao()
    }
}