package com.example.exchangeapp.di

import com.example.exchangeapp.data.location.LocationServiceImpl
import com.example.exchangeapp.domain.matching.MatchingSystem
import com.example.exchangeapp.domain.matching.MatchingSystemImpl
import com.example.exchangeapp.domain.recommendation.RecommendationEngine
import com.example.exchangeapp.domain.recommendation.RecommendationEngineImpl
import com.example.exchangeapp.domain.service.LocationService
import com.example.exchangeapp.domain.usecase.CalculateDistanceUseCase
import com.example.exchangeapp.domain.usecase.CreateExchangeOrderUseCase
import com.example.exchangeapp.domain.usecase.DeleteItemUseCase
import com.example.exchangeapp.domain.usecase.GetItemDetailsUseCase
import com.example.exchangeapp.domain.usecase.GetMatchedItemsUseCase
import com.example.exchangeapp.domain.usecase.GetRecommendedItemsUseCase
import com.example.exchangeapp.domain.usecase.MarkMessagesAsReadUseCase
import com.example.exchangeapp.domain.usecase.RecognizeItemImageUseCase
import com.example.exchangeapp.domain.usecase.RegisterUserUseCase
import com.example.exchangeapp.domain.usecase.SaveItemUseCase
import com.example.exchangeapp.domain.usecase.SendMessageUseCase
import com.example.exchangeapp.domain.usecase.ToggleFavoriteUseCase
import com.example.exchangeapp.domain.validation.ItemFormValidator
import com.example.exchangeapp.domain.validation.RegisterFormValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Use Case模块
 * 提供所有Use Case的依赖注入
 * 
 * **验证需求: 所有Use Cases**
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    
    /**
     * 提供RecommendationEngine实现
     * 
     * 推荐引擎实现，负责计算物品推荐分数
     * 
     * @param itemRepository 物品仓库
     * @param userInteractionRepository 用户交互仓库
     * @param locationService 位置服务
     * @return RecommendationEngineImpl实例
     */
    @Provides
    @Singleton
    fun provideRecommendationEngine(
        itemRepository: com.example.exchangeapp.domain.repository.ItemRepository,
        userInteractionRepository: com.example.exchangeapp.domain.repository.UserInteractionRepository,
        locationService: LocationService
    ): RecommendationEngine {
        return RecommendationEngineImpl(itemRepository, userInteractionRepository, locationService)
    }
    
    /**
     * 提供MatchingSystem实现
     * 
     * 匹配系统实现，负责计算物品匹配分数
     * 
     * @param itemRepository 物品仓库
     * @return MatchingSystemImpl实例
     */
    @Provides
    @Singleton
    fun provideMatchingSystem(
        itemRepository: com.example.exchangeapp.domain.repository.ItemRepository
    ): MatchingSystem {
        return MatchingSystemImpl(itemRepository)
    }
    
    /**
     * 提供ItemFormValidator实例
     * 
     * 物品表单验证器，用于验证发布物品时的表单数据
     * 
     * @return ItemFormValidator实例
     */
    @Provides
    @Singleton
    fun provideItemFormValidator(): ItemFormValidator {
        return ItemFormValidator()
    }
    
    /**
     * 提供RegisterFormValidator实例
     * 
     * 注册表单验证器，用于验证注册时的表单数据(手机号、密码、确认密码、昵称)
     * 
     * @return RegisterFormValidator实例
     */
    @Provides
    @Singleton
    fun provideRegisterFormValidator(): RegisterFormValidator {
        return RegisterFormValidator()
    }
    
    /**
     * 提供LocationService实现
     * 
     * 注意：LocationServiceImpl已经有@Singleton注解和@Inject构造函数，
     * 但为了确保DI正常工作，我们仍然通过@Provides方法提供它
     */
    @Provides
    @Singleton
    fun provideLocationService(locationServiceImpl: LocationServiceImpl): LocationService {
        return locationServiceImpl
    }
    
    /**
     * 提供RecognizeItemImageUseCase实例
     * 
     * AI图像识别Use Case
     * 
     * @param aiRepository AI仓库
     * @return RecognizeItemImageUseCase实例
     */
    @Provides
    @Singleton
    fun provideRecognizeItemImageUseCase(
        aiRepository: com.example.exchangeapp.data.repository.AIRepository
    ): RecognizeItemImageUseCase {
        return RecognizeItemImageUseCase(aiRepository)
    }
    
    /**
     * 提供GetRecommendedItemsUseCase实例
     * 
     * 获取推荐物品Use Case
     * 
     * @param recommendationEngine 推荐引擎
     * @return GetRecommendedItemsUseCase实例
     */
    @Provides
    @Singleton
    fun provideGetRecommendedItemsUseCase(
        recommendationEngine: RecommendationEngine
    ): GetRecommendedItemsUseCase {
        return GetRecommendedItemsUseCase(recommendationEngine)
    }
    
    /**
     * 提供GetMatchedItemsUseCase实例
     * 
     * 获取匹配物品Use Case
     * 
     * @param matchingSystem 匹配系统
     * @return GetMatchedItemsUseCase实例
     */
    @Provides
    @Singleton
    fun provideGetMatchedItemsUseCase(
        matchingSystem: MatchingSystem
    ): GetMatchedItemsUseCase {
        return GetMatchedItemsUseCase(matchingSystem)
    }
    
    /**
     * 提供CalculateDistanceUseCase实例
     * 
     * 计算距离Use Case
     * 
     * @param locationService 位置服务
     * @return CalculateDistanceUseCase实例
     */
    @Provides
    @Singleton
    fun provideCalculateDistanceUseCase(
        locationService: LocationService
    ): CalculateDistanceUseCase {
        return CalculateDistanceUseCase(locationService)
    }
    
    /**
     * 提供SaveItemUseCase实例
     * 
     * 保存物品Use Case
     * 
     * @param itemRepository 物品仓库
     * @return SaveItemUseCase实例
     */
    @Provides
    @Singleton
    fun provideSaveItemUseCase(
        itemRepository: com.example.exchangeapp.domain.repository.ItemRepository
    ): SaveItemUseCase {
        return SaveItemUseCase(itemRepository)
    }
    
    /**
     * 提供CreateExchangeOrderUseCase实例
     *
     * 发起交换（创建交换订单）Use Case
     *
     * @param orderRepository 订单仓库
     * @return CreateExchangeOrderUseCase实例
     */
    @Provides
    @Singleton
    fun provideCreateExchangeOrderUseCase(
        orderRepository: com.example.exchangeapp.domain.repository.OrderRepository
    ): CreateExchangeOrderUseCase {
        return CreateExchangeOrderUseCase(orderRepository)
    }

    /**
     * 提供DeleteItemUseCase实例
     * 
     * 删除物品Use Case
     * 
     * @param itemRepository 物品仓库
     * @return DeleteItemUseCase实例
     */
    @Provides
    @Singleton
    fun provideDeleteItemUseCase(
        itemRepository: com.example.exchangeapp.domain.repository.ItemRepository
    ): DeleteItemUseCase {
        return DeleteItemUseCase(itemRepository)
    }
    
    /**
     * 提供GetItemDetailsUseCase实例
     * 
     * 获取物品详情Use Case
     * 
     * @param itemRepository 物品仓库
     * @return GetItemDetailsUseCase实例
     */
    @Provides
    @Singleton
    fun provideGetItemDetailsUseCase(
        itemRepository: com.example.exchangeapp.domain.repository.ItemRepository
    ): GetItemDetailsUseCase {
        return GetItemDetailsUseCase(itemRepository)
    }
    
    /**
     * 提供SendMessageUseCase实例
     * 
     * 发送消息Use Case
     * 
     * @param chatRepository 聊天仓库
     * @return SendMessageUseCase实例
     */
    @Provides
    @Singleton
    fun provideSendMessageUseCase(
        chatRepository: com.example.exchangeapp.domain.repository.ChatRepository
    ): SendMessageUseCase {
        return SendMessageUseCase(chatRepository)
    }
    
    /**
     * 提供MarkMessagesAsReadUseCase实例
     * 
     * 标记消息已读Use Case
     * 
     * @param chatRepository 聊天仓库
     * @return MarkMessagesAsReadUseCase实例
     */
    @Provides
    @Singleton
    fun provideMarkMessagesAsReadUseCase(
        chatRepository: com.example.exchangeapp.domain.repository.ChatRepository
    ): MarkMessagesAsReadUseCase {
        return MarkMessagesAsReadUseCase(chatRepository)
    }
    
    /**
     * 提供ToggleFavoriteUseCase实例
     * 
     * 切换收藏状态Use Case
     * 
     * @param userInteractionRepository 用户交互仓库
     * @param currentUserProvider 当前用户提供者
     * @return ToggleFavoriteUseCase实例
     */
    @Provides
    @Singleton
    fun provideToggleFavoriteUseCase(
        userInteractionRepository: com.example.exchangeapp.domain.repository.UserInteractionRepository,
        currentUserProvider: com.example.exchangeapp.domain.service.CurrentUserProvider,
        recommendationEngine: RecommendationEngine
    ): ToggleFavoriteUseCase {
        return ToggleFavoriteUseCase(userInteractionRepository, currentUserProvider, recommendationEngine)
    }

    /**
     * 提供RegisterUserUseCase实例
     *
     * 用户注册Use Case，封装新用户账户创建业务逻辑
     *
     * @param userRepository 用户仓库
     * @return RegisterUserUseCase实例
     */
    @Provides
    @Singleton
    fun provideRegisterUserUseCase(
        userRepository: com.example.exchangeapp.domain.repository.UserRepository
    ): RegisterUserUseCase {
        return RegisterUserUseCase(userRepository)
    }
}