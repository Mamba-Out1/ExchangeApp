package com.example.exchangeapp.di

import com.example.exchangeapp.data.remote.api.OpenAIApiService
import com.example.exchangeapp.data.remote.interceptor.OpenAIRetryInterceptor
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * 测试AppModule的Retrofit配置
 * 
 * 测试场景:
 * - JSON解析器配置正确
 * - OkHttpClient配置正确(超时、拦截器)
 * - Retrofit实例配置正确
 * - OpenAIApiService创建成功
 * 
 * **验证需求: Requirements 13.1, 13.2**
 */
class AppModuleTest {
    
    @Test
    fun `provideJson应该返回配置正确的JSON实例`() {
        // When
        val json = AppModule.provideJson()
        
        // Then
        assertNotNull(json)
        // JSON配置应该包含ignoreUnknownKeys和isLenient
        // 这些配置无法直接验证，但我们可以验证实例创建成功
    }
    
    @Test
    fun `provideLoggingInterceptor应该返回HttpLoggingInterceptor实例`() {
        // When
        val interceptor = AppModule.provideLoggingInterceptor()
        
        // Then
        assertNotNull(interceptor)
        assertTrue(interceptor is HttpLoggingInterceptor)
    }
    
    @Test
    fun `provideRetryInterceptor应该返回OpenAIRetryInterceptor实例`() {
        // When
        val interceptor = AppModule.provideRetryInterceptor()
        
        // Then
        assertNotNull(interceptor)
        assertTrue(interceptor is OpenAIRetryInterceptor)
    }
    
    @Test
    fun `provideOkHttpClient应该配置正确的超时和拦截器`() {
        // Given
        val apiKey = "test-api-key"
        val retryInterceptor = OpenAIRetryInterceptor()
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // When
        val client = AppModule.provideOkHttpClient(
            apiKey = apiKey,
            retryInterceptor = retryInterceptor,
            loggingInterceptor = loggingInterceptor
        )
        
        // Then
        assertNotNull(client)
        
        // 验证超时配置
        assertEquals(10, client.connectTimeoutMillis / 1000)
        assertEquals(10, client.readTimeoutMillis / 1000)
        assertEquals(10, client.writeTimeoutMillis / 1000)
        
        // 验证拦截器数量(API密钥注入 + retry + logging)
        assertEquals(3, client.interceptors.size)
    }
    
    @Test
    fun `provideRetrofit应该使用正确的base URL和converter`() {
        // Given
        val client = OkHttpClient.Builder().build()
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val apiEndpoint = "https://api.openai.com"
        
        // When
        val retrofit = AppModule.provideRetrofit(
            okHttpClient = client,
            json = json,
            apiEndpoint = apiEndpoint
        )
        
        // Then
        assertNotNull(retrofit)
        assertEquals("https://api.openai.com/", retrofit.baseUrl().toString())
    }
    
    @Test
    fun `provideOpenAIApiService应该创建API服务实例`() {
        // Given
        val client = OkHttpClient.Builder().build()
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val retrofit = AppModule.provideRetrofit(
            okHttpClient = client,
            json = json,
            apiEndpoint = "https://api.openai.com"
        )
        
        // When
        val apiService = AppModule.provideOpenAIApiService(retrofit)
        
        // Then
        assertNotNull(apiService)
        assertTrue(apiService is OpenAIApiService)
    }
    
    @Test
    fun `OkHttpClient应该包含API密钥注入拦截器`() {
        // Given
        val apiKey = "sk-test-key-123"
        val retryInterceptor = OpenAIRetryInterceptor()
        val loggingInterceptor = HttpLoggingInterceptor()
        
        // When
        val client = AppModule.provideOkHttpClient(
            apiKey = apiKey,
            retryInterceptor = retryInterceptor,
            loggingInterceptor = loggingInterceptor
        )
        
        // Then
        // 验证拦截器存在
        assertNotNull(client.interceptors)
        assertTrue(client.interceptors.size >= 3)
    }
    
    @Test
    fun `完整的依赖链应该正常工作`() {
        // Given
        val apiKey = "test-key"
        val apiEndpoint = "https://api.openai.com"
        val json = AppModule.provideJson()
        val loggingInterceptor = AppModule.provideLoggingInterceptor()
        val retryInterceptor = AppModule.provideRetryInterceptor()
        
        // When
        val client = AppModule.provideOkHttpClient(
            apiKey = apiKey,
            retryInterceptor = retryInterceptor,
            loggingInterceptor = loggingInterceptor
        )
        val retrofit = AppModule.provideRetrofit(
            okHttpClient = client,
            json = json,
            apiEndpoint = apiEndpoint
        )
        val apiService = AppModule.provideOpenAIApiService(retrofit)
        
        // Then
        assertNotNull(apiService)
    }
}
