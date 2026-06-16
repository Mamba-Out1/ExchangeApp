package com.example.exchangeapp.data.remote

import com.example.exchangeapp.data.remote.api.OpenAIApiService
import com.example.exchangeapp.data.remote.interceptor.OpenAIRetryInterceptor
import com.example.exchangeapp.data.repository.AIRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * OpenAI API集成测试
 *
 * 使用MockWebServer模拟OpenAI API的成功与错误响应，验证从网络层(OpenAIApiService)
 * 经由重试拦截器(OpenAIRetryInterceptor)到Repository层(AIRepositoryImpl)的端到端行为。
 *
 * **验证需求: Requirements 1.7, 13.3, 13.7**
 * - 1.7: 图像识别API调用并解析返回结果
 * - 13.3: 网络不可用时返回"网络不可用，请检查网络连接"提示
 * - 13.7: 重试3次后仍失败则停止重试并提示用户
 */
class OpenAIApiIntegrationTest {

    private lateinit var server: MockWebServer
    private val testApiKey = "sk-test-key"
    private val testImageBase64 = "dGVzdC1pbWFnZS1iYXNlNjQ=" // "test-image-base64"

    /** 与生产环境一致的宽松JSON解析配置 */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 构建指向MockWebServer的Retrofit + OpenAIApiService。
     * OkHttpClient包含OpenAIRetryInterceptor，以便真实验证重试行为。
     * 超时时间设置得较短以加快连接失败类测试。
     */
    private fun buildApiService(baseUrl: String): OpenAIApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .addInterceptor(OpenAIRetryInterceptor())
            .build()

        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OpenAIApiService::class.java)
    }

    private fun buildRepository(baseUrl: String): AIRepositoryImpl =
        AIRepositoryImpl(buildApiService(baseUrl), testApiKey)

    private fun successResponseBody(): String = """
        {
            "id": "chatcmpl-test-123",
            "choices": [
                {
                    "message": {
                        "content": "{\"name\":\"iPhone 13\",\"description\":\"一台二手智能手机，成色良好，功能正常。\",\"estimatedPrice\":3000.0,\"tags\":[\"电子产品\"]}"
                    }
                }
            ]
        }
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    // -------------------- 成功场景 --------------------

    @Test
    fun `recognizeItem 成功解析有效的识别结果`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(successResponseBody())
        )
        val repository = buildRepository(server.url("/").toString())

        val result = repository.recognizeItem(testImageBase64)

        assertTrue(result.isSuccess, "成功响应应返回Result.success")
        val recognition = result.getOrNull()
        assertNotNull(recognition)
        assertEquals("iPhone 13", recognition!!.name)
        assertEquals(3000.0, recognition.estimatedPrice)
        assertEquals(listOf("电子产品"), recognition.tags)

        // 验证调用了正确的OpenAI端点 (Req 1.7)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals(1, server.requestCount)
    }

    // -------------------- 错误场景: 服务器错误 + 重试 --------------------

    @Test
    fun `recognizeItem 在持续500错误时重试3次后失败`() = runTest {
        // 1次初始请求 + 3次重试 = 4次请求 (Req 13.7)
        repeat(4) {
            server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        }
        val repository = buildRepository(server.url("/").toString())

        val result = repository.recognizeItem(testImageBase64)

        assertTrue(result.isFailure, "持续5xx应返回失败")
        assertEquals(4, server.requestCount, "应在初始请求后最多重试3次")
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("OpenAI服务暂时不可用"), "实际消息: $message")
    }

    @Test
    fun `recognizeItem 在持续429限流时重试3次后失败`() = runTest {
        // 1次初始请求 + 3次重试 = 4次请求 (Req 13.7)
        repeat(4) {
            server.enqueue(MockResponse().setResponseCode(429).setBody("{}"))
        }
        val repository = buildRepository(server.url("/").toString())

        val result = repository.recognizeItem(testImageBase64)

        assertTrue(result.isFailure, "持续429应返回失败")
        assertEquals(4, server.requestCount, "应在初始请求后最多重试3次")
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("频率超限"), "实际消息: $message")
    }

    @Test
    fun `recognizeItem 在瞬时500后成功时自动恢复`() = runTest {
        // 前两次失败，第三次成功 -> 验证自动重试恢复能力
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody(successResponseBody()))
        val repository = buildRepository(server.url("/").toString())

        val result = repository.recognizeItem(testImageBase64)

        assertTrue(result.isSuccess, "重试后成功应返回Result.success")
        assertEquals("iPhone 13", result.getOrNull()?.name)
        assertEquals(3, server.requestCount, "应在2次失败后第3次成功")
    }

    @Test
    fun `recognizeItem 对4xx客户端错误不重试`() = runTest {
        // 400不属于429/5xx，拦截器不应重试 (Req 13.6)
        server.enqueue(MockResponse().setResponseCode(400).setBody("{}"))
        val repository = buildRepository(server.url("/").toString())

        val result = repository.recognizeItem(testImageBase64)

        assertTrue(result.isFailure, "4xx应返回失败")
        assertEquals(1, server.requestCount, "4xx错误不应触发重试")
    }

    // -------------------- 网络不可用场景 --------------------

    @Test
    fun `recognizeItem 在主机无法解析时返回网络不可用提示`() = runTest {
        // 使用RFC6761保留的 .invalid 顶级域，保证DNS解析失败 -> UnknownHostException (Req 13.3)
        val repository = buildRepository("https://nonexistent-host.invalid/")

        val result = repository.recognizeItem(testImageBase64)

        assertTrue(result.isFailure, "网络不可用应返回失败")
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(
            message.contains("网络不可用，请检查网络连接"),
            "实际消息: $message"
        )
    }

    @Test
    fun `recognizeItem 在API密钥为空时直接失败且不发起请求`() = runTest {
        val repository = AIRepositoryImpl(buildApiService(server.url("/").toString()), apiKey = "")

        val result = repository.recognizeItem(testImageBase64)

        assertTrue(result.isFailure)
        assertEquals(0, server.requestCount, "密钥缺失时不应发起网络请求")
        assertFalse(result.isSuccess)
    }
}
