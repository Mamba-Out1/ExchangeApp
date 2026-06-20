package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.remote.api.OpenAIApiService
import com.example.exchangeapp.data.remote.dto.Choice
import com.example.exchangeapp.data.remote.dto.ImageAnalysisResponse
import com.example.exchangeapp.data.remote.dto.MessageResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * AIRepository单元测试
 * 
 * 测试AI识别功能的各种场景，包括：
 * - 成功识别场景
 * - API错误处理（401, 429, 5xx）
 * - JSON解析错误
 * - 网络错误处理
 * - 数据验证
 * 
 * **验证需求: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 13.2, 13.3, 13.7**
 */
class AIRepositoryTest {
    
    private lateinit var apiService: OpenAIApiService
    private lateinit var repository: AIRepository
    private val testApiKey = "sk-test-key-12345"
    
    @BeforeEach
    fun setup() {
        apiService = mockk()
        repository = AIRepositoryImpl(apiService, testApiKey)
    }
    
    @Test
    fun `recognizeItem should return success when API returns valid response`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        val validJsonResponse = """
            {
                "name": "iPhone 13",
                "description": "一部功能完好的iPhone 13手机，128GB存储空间，黑色外观，使用约一年时间。",
                "estimatedPrice": 4500.0,
                "tags": ["电子产品", "手机"]
            }
        """.trimIndent()
        
        val mockResponse = ImageAnalysisResponse(
            id = "chatcmpl-123",
            choices = listOf(
                Choice(
                    message = MessageResponse(content = validJsonResponse)
                )
            )
        )
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.success(mockResponse)
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isSuccess)
        val item = result.getOrNull()!!
        assertEquals("iPhone 13", item.name)
        assertEquals("一部功能完好的iPhone 13手机，128GB存储空间，黑色外观，使用约一年时间。", item.description)
        assertEquals(4500.0, item.estimatedPrice)
        assertEquals(listOf("电子产品", "手机"), item.tags)
        
        coVerify(exactly = 1) { apiService.analyzeImage(any(), any()) }
    }
    
    @Test
    fun `recognizeItem should handle JSON with markdown code blocks`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        val jsonWithMarkdown = """
            ```json
            {
                "name": "计算机组成原理",
                "description": "经典计算机教材，适合计算机专业学生使用。",
                "estimatedPrice": 35.0,
                "tags": ["书籍", "教材"]
            }
            ```
        """.trimIndent()
        
        val mockResponse = ImageAnalysisResponse(
            id = "chatcmpl-456",
            choices = listOf(
                Choice(
                    message = MessageResponse(content = jsonWithMarkdown)
                )
            )
        )
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.success(mockResponse)
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isSuccess)
        val item = result.getOrNull()!!
        assertEquals("计算机组成原理", item.name)
        assertEquals(listOf("书籍", "教材"), item.tags)
    }
    
    @Test
    fun `recognizeItem should return failure when API key is blank`() = runTest {
        // Given
        val repositoryWithNoKey = AIRepositoryImpl(apiService, "")
        val imageBase64 = "base64encodedimage"
        
        // When
        val result = repositoryWithNoKey.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("API密钥未配置"))
    }
    
    @Test
    fun `recognizeItem should return failure when API returns 401`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.error(401, mockk(relaxed = true))
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("API密钥无效"))
    }
    
    @Test
    fun `recognizeItem should return failure when API returns 429`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.error(429, mockk(relaxed = true))
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("请求频率超限"))
    }
    
    @Test
    fun `recognizeItem should return failure when API returns 500`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.error(500, mockk(relaxed = true))
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("服务暂时不可用"))
    }
    
    @Test
    fun `recognizeItem should return failure when API returns empty response`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        
        val mockResponse = ImageAnalysisResponse(
            id = "chatcmpl-789",
            choices = emptyList()
        )
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.success(mockResponse)
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("空响应"))
    }
    
    @Test
    fun `recognizeItem should return failure when JSON is malformed`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        val malformedJson = """
            {
                "name": "物品名称",
                "description": "描述",
                // 这是无效的JSON注释
                "estimatedPrice": "invalid_number",
                "tags": ["标签1"]
            }
        """.trimIndent()
        
        val mockResponse = ImageAnalysisResponse(
            id = "chatcmpl-999",
            choices = listOf(
                Choice(
                    message = MessageResponse(content = malformedJson)
                )
            )
        )
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.success(mockResponse)
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("JSON格式不正确") || 
                   result.exceptionOrNull()!!.message!!.contains("格式错误"))
    }
    
    @Test
    fun `recognizeItem should return failure when name is blank`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        val jsonWithBlankName = """
            {
                "name": "",
                "description": "物品描述",
                "estimatedPrice": 100.0,
                "tags": ["标签1"]
            }
        """.trimIndent()
        
        val mockResponse = ImageAnalysisResponse(
            id = "chatcmpl-111",
            choices = listOf(
                Choice(
                    message = MessageResponse(content = jsonWithBlankName)
                )
            )
        )
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.success(mockResponse)
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("物品名称"))
    }
    
    @Test
    fun `recognizeItem should return failure when description is blank`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        val jsonWithBlankDescription = """
            {
                "name": "物品名称",
                "description": "",
                "estimatedPrice": 100.0,
                "tags": ["标签1"]
            }
        """.trimIndent()
        
        val mockResponse = ImageAnalysisResponse(
            id = "chatcmpl-222",
            choices = listOf(
                Choice(
                    message = MessageResponse(content = jsonWithBlankDescription)
                )
            )
        )
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.success(mockResponse)
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("物品描述"))
    }
    
    @Test
    fun `recognizeItem should return failure when estimatedPrice is zero or negative`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        val jsonWithInvalidPrice = """
            {
                "name": "物品名称",
                "description": "物品描述",
                "estimatedPrice": 0,
                "tags": ["标签1"]
            }
        """.trimIndent()
        
        val mockResponse = ImageAnalysisResponse(
            id = "chatcmpl-333",
            choices = listOf(
                Choice(
                    message = MessageResponse(content = jsonWithInvalidPrice)
                )
            )
        )
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.success(mockResponse)
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("估价"))
    }
    
    @Test
    fun `recognizeItem should return failure when tags are empty`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        val jsonWithEmptyTags = """
            {
                "name": "物品名称",
                "description": "物品描述",
                "estimatedPrice": 100.0,
                "tags": []
            }
        """.trimIndent()
        
        val mockResponse = ImageAnalysisResponse(
            id = "chatcmpl-444",
            choices = listOf(
                Choice(
                    message = MessageResponse(content = jsonWithEmptyTags)
                )
            )
        )
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } returns Response.success(mockResponse)
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("标签"))
    }
    
    @Test
    fun `recognizeItem should return failure when network times out`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } throws java.net.SocketTimeoutException("Connect timed out")
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("超时"))
    }
    
    @Test
    fun `recognizeItem should return failure when network is unavailable`() = runTest {
        // Given
        val imageBase64 = "base64encodedimage"
        
        coEvery { 
            apiService.analyzeImage(any(), any()) 
        } throws java.net.UnknownHostException("Unable to resolve host")
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("网络不可用"))
    }
    
    @Test
    fun `recognizeItem should send correct request format`() = runTest {
        // Given
        val imageBase64 = "testbase64"
        val validJsonResponse = """
            {
                "name": "测试物品",
                "description": "测试描述内容",
                "estimatedPrice": 50.0,
                "tags": ["其他"]
            }
        """.trimIndent()
        
        val mockResponse = ImageAnalysisResponse(
            id = "chatcmpl-555",
            choices = listOf(
                Choice(
                    message = MessageResponse(content = validJsonResponse)
                )
            )
        )
        
        coEvery { 
            apiService.analyzeImage(
                authorization = "Bearer $testApiKey",
                request = match { 
                    it.model == "gpt-4-vision-preview" &&
                    it.maxTokens == 500 &&
                    it.messages.size == 1 &&
                    it.messages[0].role == "user" &&
                    it.messages[0].content.size == 2 &&
                    it.messages[0].content[0].type == "text" &&
                    it.messages[0].content[1].type == "image_url" &&
                    it.messages[0].content[1].imageUrl?.url == "data:image/jpeg;base64,$imageBase64"
                }
            ) 
        } returns Response.success(mockResponse)
        
        // When
        val result = repository.recognizeItem(imageBase64)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { 
            apiService.analyzeImage(
                authorization = "Bearer $testApiKey",
                request = any()
            ) 
        }
    }
}
