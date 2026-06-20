package com.example.exchangeapp.di

import com.example.exchangeapp.data.remote.api.OpenAIApiService
import com.example.exchangeapp.data.remote.interceptor.OpenAIRetryInterceptor
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
 * 网络模块
 * 提供网络相关依赖，包括Retrofit、OkHttpClient、OpenAIApiService等
 * 
 * **验证需求: Requirements 13.1, 13.2, 13.5, 13.6**
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * 提供OpenAI重试拦截器，具有指数退避重试策略
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
     * 提供OkHttpClient，配置了拦截器和超时设置
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
        @ApiKey apiKey: String,
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
     * 提供Retrofit实例，配置了OpenAI API
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
        @ApiEndpoint apiEndpoint: String
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        
        return Retrofit.Builder()
            .baseUrl(apiEndpoint.ensureTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    
    /**
     * 提供OpenAI API服务接口
     * 
     * 提供OpenAI GPT-4V API的网络接口实现
     */
    @Provides
    @Singleton
    fun provideOpenAIApiService(retrofit: Retrofit): OpenAIApiService {
        return retrofit.create(OpenAIApiService::class.java)
    }

    private fun String.ensureTrailingSlash(): String {
        return if (endsWith("/")) this else "$this/"
    }
}
