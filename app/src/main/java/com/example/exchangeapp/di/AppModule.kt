package com.example.exchangeapp.di

import com.example.exchangeapp.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.logging.HttpLoggingInterceptor
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
    @ApiKey
    fun provideApiKey(): String {
        return BuildConfig.DASHSCOPE_API_KEY
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
    @ApiEndpoint
    fun provideApiEndpoint(): String {
        return BuildConfig.DASHSCOPE_API_ENDPOINT
    }

    @Provides
    @Singleton
    @AiModel
    fun provideAiModel(): String {
        return BuildConfig.DASHSCOPE_MODEL
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
