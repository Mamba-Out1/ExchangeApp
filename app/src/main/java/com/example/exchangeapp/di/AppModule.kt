package com.example.exchangeapp.di

import android.content.Context
import com.example.exchangeapp.BuildConfig
import com.example.exchangeapp.data.local.dao.ChatDao
import com.example.exchangeapp.data.local.dao.ItemDao
import com.example.exchangeapp.data.local.dao.OrderDao
import com.example.exchangeapp.data.local.dao.UserDao
import com.example.exchangeapp.data.local.dao.UserInteractionDao
import com.example.exchangeapp.data.local.database.AppDatabase
import com.example.exchangeapp.data.remote.api.OpenAIApiService
import com.example.exchangeapp.data.remote.interceptor.OpenAIRetryInterceptor
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
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 应用级依赖模块
 * 提供应用范围的配置依赖，如JSON序列化器、API配置等
 * 
 * **验证需求: Requirements 13.1**
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provides the OpenAI API key from BuildConfig.
     * 
     * 密钥从local.properties文件读取：
     * OPENAI_API_KEY=sk-your-api-key-here
     */
    @Provides
    @Singleton
    fun provideApiKey(): String {
        return BuildConfig.OPENAI_API_KEY
    }
    
    /**
     * Provides the OpenAI API endpoint from BuildConfig.
     * 
     * 默认endpoint: https://api.openai.com
     * 可通过local.properties自定义：
     * OPENAI_API_ENDPOINT=https://your-custom-endpoint.com
     */
    @Provides
    @Singleton
    fun provideApiEndpoint(): String {
        return BuildConfig.OPENAI_API_ENDPOINT
    }
    
    /**
     * Provides JSON serializer with lenient parsing configuration.
     * 
     * 配置:
     * - ignoreUnknownKeys: 忽略未知字段，提高API兼容性
     * - isLenient: 宽松的JSON解析
     */
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }
    
    /**
     * Provides HTTP logging interceptor for debugging.
     * 
     * 日志级别: BODY (详细日志包含请求和响应体)
     * 仅在调试构建中启用详细日志
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
}
