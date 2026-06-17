package com.example.exchangeapp.di

import android.content.Context
import com.example.exchangeapp.data.repository.AIRepository
import com.example.exchangeapp.data.repository.AIRepositoryImpl
import com.example.exchangeapp.data.repository.ChatRepositoryImpl
import com.example.exchangeapp.data.repository.ItemRepositoryImpl
import com.example.exchangeapp.data.repository.OrderRepositoryImpl
import com.example.exchangeapp.data.repository.UserInteractionRepositoryImpl
import com.example.exchangeapp.data.repository.UserRepositoryImpl
import com.example.exchangeapp.domain.repository.ChatRepository
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.repository.OrderRepository
import com.example.exchangeapp.domain.repository.UserInteractionRepository
import com.example.exchangeapp.domain.repository.UserRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.service.CurrentUserProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 仓库模块
 * 提供所有Repository实现的依赖注入
 * 
 * **验证需求: Requirements 5.1, 5.2, 5.3, 7.2, 8.2, 8.4, 9.3, 9.4, 9.5, 9.7, 10.4, 13.1**
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    /**
     * 提供CurrentUserProvider实现
     * 
     * 提供当前登录用户的ID，用于需要用户上下文的Repository操作
     */
    @Provides
    @Singleton
    fun provideCurrentUserProvider(currentUserProviderImpl: CurrentUserProviderImpl): CurrentUserProvider {
        return currentUserProviderImpl
    }
    
    /**
     * 提供CurrentUserProviderImpl实例
     * 
     * @param context Android Context
     * @return CurrentUserProviderImpl实例
     */
    @Provides
    @Singleton
    fun provideCurrentUserProviderImpl(
        @ApplicationContext context: Context
    ): CurrentUserProviderImpl {
        return CurrentUserProviderImpl(context)
    }
    
    /**
     * 提供ItemRepository实现
     * 
     * 基于Room的ItemDao完成物品数据的本地持久化
     * 
     * **验证需求: Requirements 5.1, 5.2, 5.3, 14.4, 14.5**
     */
    @Provides
    @Singleton
    fun provideItemRepository(itemRepositoryImpl: ItemRepositoryImpl): ItemRepository {
        return itemRepositoryImpl
    }
    
    /**
     * 提供ItemRepositoryImpl实现
     * 
     * @param itemDao ItemDao实例
     * @return ItemRepositoryImpl实例
     */
    @Provides
    @Singleton
    fun provideItemRepositoryImpl(
        itemDao: com.example.exchangeapp.data.local.dao.ItemDao
    ): ItemRepositoryImpl {
        return ItemRepositoryImpl(itemDao)
    }
    
    /**
     * 提供UserRepository实现
     * 
     * 基于Room的UserDao完成用户数据的本地持久化
     * 
     * **验证需求: Requirements 7.2, 11.3, 11.4**
     */
    @Provides
    @Singleton
    fun provideUserRepository(userRepositoryImpl: UserRepositoryImpl): UserRepository {
        return userRepositoryImpl
    }
    
    /**
     * 提供UserRepositoryImpl实现
     * 
     * @param userDao UserDao实例
     * @return UserRepositoryImpl实例
     */
    @Provides
    @Singleton
    fun provideUserRepositoryImpl(
        userDao: com.example.exchangeapp.data.local.dao.UserDao
    ): UserRepositoryImpl {
        return UserRepositoryImpl(userDao)
    }
    
    /**
     * 提供OrderRepository实现
     * 
     * 基于Room的OrderDao完成交换订单的本地持久化
     * 
     * **验证需求: Requirements 8.2, 8.4, 10.1**
     */
    @Provides
    @Singleton
    fun provideOrderRepository(orderRepositoryImpl: OrderRepositoryImpl): OrderRepository {
        return orderRepositoryImpl
    }
    
    /**
     * 提供OrderRepositoryImpl实现
     * 
     * @param orderDao OrderDao实例
     * @return OrderRepositoryImpl实例
     */
    @Provides
    @Singleton
    fun provideOrderRepositoryImpl(
        orderDao: com.example.exchangeapp.data.local.dao.OrderDao
    ): OrderRepositoryImpl {
        return OrderRepositoryImpl(orderDao)
    }
    
    /**
     * 提供ChatRepository实现
     * 
     * 基于Room的ChatDao完成聊天消息的本地持久化
     * 
     * **验证需求: Requirements 9.3, 9.4, 9.5, 9.7**
     */
    @Provides
    @Singleton
    fun provideChatRepository(chatRepositoryImpl: ChatRepositoryImpl): ChatRepository {
        return chatRepositoryImpl
    }
    
    /**
     * 提供ChatRepositoryImpl实现
     * 
     * @param chatDao ChatDao实例
     * @return ChatRepositoryImpl实例
     */
    @Provides
    @Singleton
    fun provideChatRepositoryImpl(
        chatDao: com.example.exchangeapp.data.local.dao.ChatDao
    ): ChatRepositoryImpl {
        return ChatRepositoryImpl(chatDao)
    }
    
    /**
     * 提供UserInteractionRepository实现
     * 
     * 基于Room的UserInteractionDao完成用户交互记录的本地持久化
     * 
     * **验证需求: Requirements 3.5, 3.6, 10.4**
     */
    @Provides
    @Singleton
    fun provideUserInteractionRepository(
        userInteractionRepositoryImpl: UserInteractionRepositoryImpl
    ): UserInteractionRepository {
        return userInteractionRepositoryImpl
    }
    
    /**
     * 提供UserInteractionRepositoryImpl实现
     * 
     * @param userInteractionDao UserInteractionDao实例
     * @param currentUserProvider CurrentUserProvider实例
     * @return UserInteractionRepositoryImpl实例
     */
    @Provides
    @Singleton
    fun provideUserInteractionRepositoryImpl(
        userInteractionDao: com.example.exchangeapp.data.local.dao.UserInteractionDao,
        currentUserProvider: com.example.exchangeapp.domain.service.CurrentUserProvider
    ): UserInteractionRepositoryImpl {
        return UserInteractionRepositoryImpl(userInteractionDao, currentUserProvider)
    }
    
    /**
     * 提供AIRepository实现
     * 
     * 用于调用OpenAI API进行物品图像识别
     * 
     * **验证需求: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6**
     */
    @Provides
    @Singleton
    fun provideAIRepository(aiRepositoryImpl: AIRepositoryImpl): AIRepository {
        return aiRepositoryImpl
    }
    
    /**
     * 提供AIRepositoryImpl实现
     * 
     * @param apiService OpenAIApiService实例
     * @param apiKey OpenAI API密钥
     * @return AIRepositoryImpl实例
     */
    @Provides
    @Singleton
    fun provideAIRepositoryImpl(
        apiService: com.example.exchangeapp.data.remote.api.OpenAIApiService,
        @ApiKey apiKey: String
    ): AIRepositoryImpl {
        return AIRepositoryImpl(apiService, apiKey)
    }
}