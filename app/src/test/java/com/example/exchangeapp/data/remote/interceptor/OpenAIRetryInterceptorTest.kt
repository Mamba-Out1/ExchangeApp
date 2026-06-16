package com.example.exchangeapp.data.remote.interceptor

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * 测试OpenAIRetryInterceptor的重试逻辑
 * 
 * 测试场景:
 * - 成功响应不重试
 * - 429错误重试3次
 * - 5xx错误重试3次
 * - 4xx错误(除429)不重试
 * - 重试使用指数退避策略
 * 
 * **验证需求: Requirements 13.5, 13.6**
 */
class OpenAIRetryInterceptorTest {
    
    private lateinit var interceptor: OpenAIRetryInterceptor
    private lateinit var chain: Interceptor.Chain
    private lateinit var request: Request
    
    @BeforeEach
    fun setup() {
        interceptor = OpenAIRetryInterceptor()
        chain = mockk(relaxed = true)
        request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .build()
        
        every { chain.request() } returns request
    }
    
    @Test
    fun `成功响应不应该重试`() {
        // Given: 成功的响应
        val successResponse = createResponse(200)
        every { chain.proceed(any()) } returns successResponse
        
        // When: 执行拦截器
        val result = interceptor.intercept(chain)
        
        // Then: 只调用一次proceed，不重试
        verify(exactly = 1) { chain.proceed(any()) }
        assertEquals(200, result.code)
    }
    
    @Test
    fun `429错误应该重试3次`() {
        // Given: 429响应
        val rateLimitResponse = createResponse(429)
        every { chain.proceed(any()) } returns rateLimitResponse
        
        // When: 执行拦截器
        val result = interceptor.intercept(chain)
        
        // Then: 总共调用4次(首次 + 3次重试)
        verify(exactly = 4) { chain.proceed(any()) }
        assertEquals(429, result.code)
    }
    
    @Test
    fun `500错误应该重试3次`() {
        // Given: 500响应
        val serverErrorResponse = createResponse(500)
        every { chain.proceed(any()) } returns serverErrorResponse
        
        // When: 执行拦截器
        val result = interceptor.intercept(chain)
        
        // Then: 总共调用4次(首次 + 3次重试)
        verify(exactly = 4) { chain.proceed(any()) }
        assertEquals(500, result.code)
    }
    
    @Test
    fun `503错误应该重试3次`() {
        // Given: 503响应
        val serviceUnavailableResponse = createResponse(503)
        every { chain.proceed(any()) } returns serviceUnavailableResponse
        
        // When: 执行拦截器
        val result = interceptor.intercept(chain)
        
        // Then: 总共调用4次(首次 + 3次重试)
        verify(exactly = 4) { chain.proceed(any()) }
        assertEquals(503, result.code)
    }
    
    @Test
    fun `400错误不应该重试`() {
        // Given: 400响应
        val badRequestResponse = createResponse(400)
        every { chain.proceed(any()) } returns badRequestResponse
        
        // When: 执行拦截器
        val result = interceptor.intercept(chain)
        
        // Then: 只调用一次proceed，不重试
        verify(exactly = 1) { chain.proceed(any()) }
        assertEquals(400, result.code)
    }
    
    @Test
    fun `401错误不应该重试`() {
        // Given: 401响应
        val unauthorizedResponse = createResponse(401)
        every { chain.proceed(any()) } returns unauthorizedResponse
        
        // When: 执行拦截器
        val result = interceptor.intercept(chain)
        
        // Then: 只调用一次proceed，不重试
        verify(exactly = 1) { chain.proceed(any()) }
        assertEquals(401, result.code)
    }
    
    @Test
    fun `404错误不应该重试`() {
        // Given: 404响应
        val notFoundResponse = createResponse(404)
        every { chain.proceed(any()) } returns notFoundResponse
        
        // When: 执行拦截器
        val result = interceptor.intercept(chain)
        
        // Then: 只调用一次proceed，不重试
        verify(exactly = 1) { chain.proceed(any()) }
        assertEquals(404, result.code)
    }
    
    @Test
    fun `第二次重试成功应该停止重试`() {
        // Given: 首次429，第二次成功
        val rateLimitResponse = createResponse(429)
        val successResponse = createResponse(200)
        every { chain.proceed(any()) } returnsMany listOf(
            rateLimitResponse,
            successResponse
        )
        
        // When: 执行拦截器
        val result = interceptor.intercept(chain)
        
        // Then: 总共调用2次(首次失败 + 1次重试成功)
        verify(exactly = 2) { chain.proceed(any()) }
        assertEquals(200, result.code)
    }
    
    @Test
    fun `指数退避策略验证`() {
        // Given: 429响应
        val rateLimitResponse = createResponse(429)
        every { chain.proceed(any()) } returns rateLimitResponse
        
        // When: 执行拦截器并测量时间
        val startTime = System.currentTimeMillis()
        interceptor.intercept(chain)
        val elapsedTime = System.currentTimeMillis() - startTime
        
        // Then: 应该至少等待 1000 + 2000 + 4000 = 7000ms
        // 允许一些误差范围
        assertTrue(elapsedTime >= 6500, "Expected at least 6500ms, but got $elapsedTime ms")
    }
    
    /**
     * 创建模拟的HTTP响应
     */
    private fun createResponse(code: Int): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Test Response")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
    }
}
