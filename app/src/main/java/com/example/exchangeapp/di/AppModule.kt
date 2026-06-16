package com.example.exchangeapp.di

import com.example.exchangeapp.BuildConfig
import com.example.exchangeapp.data.remote.api.OpenAIApiService
import com.example.exchangeapp.data.remote.interceptor.OpenAIRetryInterceptor
import com.example.exchangeapp.data.repository.AIRepository
import com.example.exchangeapp.data.repository.AIRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies.
 * This module provides app-wide singleton instances.
 * 
 * **验证需求: Requirements 13.1, 13.2, 13.5, 13.6**
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
    
    /**
     * Provides OpenAI retry interceptor with exponential backoff.
     * 
     * 重试策略:
     * - 最多重试3次
     * - 对429 (Rate Limit)和5xx错误重试
     * - 指数退避: 1秒 -> 2秒 -> 4秒
     * 
     * **验证需求: Requirements 13.5, 13.6**
     */
    @Provides
    @Singleton
    fun provideRetryInterceptor(): OpenAIRetryInterceptor {
        return OpenAIRetryInterceptor()
    }
    
    /**
     * Provides OkHttpClient with configured interceptors and timeouts.
     * 
     * 配置:
     * - 连接超时: 10秒
     * - 读取超时: 10秒
     * - 写入超时: 10秒
     * - API密钥注入: 自动添加Authorization header
     * - 重试机制: OpenAIRetryInterceptor
     * - 日志记录: HttpLoggingInterceptor
     * 
     * **验证需求: Requirements 13.1, 13.2**
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiKey: String,
        retryInterceptor: OpenAIRetryInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // API密钥注入拦截器
                val originalRequest = chain.request()
                val requestWithAuth = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(requestWithAuth)
            }
            .addInterceptor(retryInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    /**
     * Provides Retrofit instance configured for OpenAI API.
     * 
     * 配置:
     * - Base URL: 从BuildConfig读取
     * - JSON转换器: kotlinx.serialization
     * - HTTP客户端: 带拦截器和超时配置的OkHttpClient
     * 
     * **验证需求: Requirements 13.1, 13.2**
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        apiEndpoint: String
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        
        return Retrofit.Builder()
            .baseUrl(apiEndpoint)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    
    /**
     * Provides OpenAI API service interface.
     * 
     * 提供OpenAI GPT-4V API的网络接口实现
     */
    @Provides
    @Singleton
    fun provideOpenAIApiService(retrofit: Retrofit): OpenAIApiService {
        return retrofit.create(OpenAIApiService::class.java)
    }
    
    /**
     * Provides AIRepository implementation.
     * 
     * 提供AI识别Repository的实现，用于调用OpenAI API进行物品识别
     * 
     * **验证需求: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6**
     */
    @Provides
    @Singleton
    fun provideAIRepository(
        apiService: OpenAIApiService,
        apiKey: String
    ): AIRepository {
        return AIRepositoryImpl(apiService, apiKey)
    }
}
