package com.example.exchangeapp.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 测试OpenAI API DTO的序列化和反序列化
 */
class OpenAIApiDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    @Test
    fun `test ImageAnalysisRequest serialization`() {
        // Given
        val request = ImageAnalysisRequest(
            model = "gpt-4-vision-preview",
            messages = listOf(
                Message(
                    role = "user",
                    content = listOf(
                        Content(
                            type = "text",
                            text = "请识别这个物品"
                        ),
                        Content(
                            type = "image_url",
                            imageUrl = ImageUrl(url = "data:image/jpeg;base64,test123")
                        )
                    )
                )
            ),
            maxTokens = 500
        )

        // When
        val jsonString = json.encodeToString(request)

        // Then
        assert(jsonString.contains("\"model\":\"gpt-4-vision-preview\""))
        assert(jsonString.contains("\"max_tokens\":500"))
        assert(jsonString.contains("\"role\":\"user\""))
        assert(jsonString.contains("\"type\":\"text\""))
        assert(jsonString.contains("\"type\":\"image_url\""))
    }

    @Test
    fun `test ImageAnalysisRequest deserialization`() {
        // Given
        val jsonString = """
            {
                "model": "gpt-4-vision-preview",
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "请识别这个物品"
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": "data:image/jpeg;base64,test123"
                                }
                            }
                        ]
                    }
                ],
                "max_tokens": 500
            }
        """.trimIndent()

        // When
        val request = json.decodeFromString<ImageAnalysisRequest>(jsonString)

        // Then
        assertEquals("gpt-4-vision-preview", request.model)
        assertEquals(500, request.maxTokens)
        assertEquals(1, request.messages.size)
        assertEquals("user", request.messages[0].role)
        assertEquals(2, request.messages[0].content.size)
        assertEquals("text", request.messages[0].content[0].type)
        assertEquals("请识别这个物品", request.messages[0].content[0].text)
        assertEquals("image_url", request.messages[0].content[1].type)
        assertNotNull(request.messages[0].content[1].imageUrl)
        assertEquals("data:image/jpeg;base64,test123", request.messages[0].content[1].imageUrl?.url)
    }

    @Test
    fun `test ImageAnalysisResponse serialization`() {
        // Given
        val response = ImageAnalysisResponse(
            id = "chatcmpl-123",
            choices = listOf(
                Choice(
                    message = MessageResponse(
                        content = """{"name":"笔记本电脑","description":"一台笔记本电脑","estimatedPrice":3000.0,"tags":["电子产品"]}"""
                    )
                )
            )
        )

        // When
        val jsonString = json.encodeToString(response)

        // Then
        assert(jsonString.contains("\"id\":\"chatcmpl-123\""))
        assert(jsonString.contains("\"choices\""))
        assert(jsonString.contains("\"message\""))
        assert(jsonString.contains("\"content\""))
    }

    @Test
    fun `test ImageAnalysisResponse deserialization`() {
        // Given
        val jsonString = """
            {
                "id": "chatcmpl-123",
                "choices": [
                    {
                        "message": {
                            "content": "{\"name\":\"笔记本电脑\",\"description\":\"一台笔记本电脑\",\"estimatedPrice\":3000.0,\"tags\":[\"电子产品\"]}"
                        }
                    }
                ]
            }
        """.trimIndent()

        // When
        val response = json.decodeFromString<ImageAnalysisResponse>(jsonString)

        // Then
        assertEquals("chatcmpl-123", response.id)
        assertEquals(1, response.choices.size)
        assertNotNull(response.choices[0].message.content)
        assert(response.choices[0].message.content.contains("笔记本电脑"))
        assert(response.choices[0].message.content.contains("estimatedPrice"))
    }

    @Test
    fun `test Content with text only`() {
        // Given
        val content = Content(
            type = "text",
            text = "请识别这个物品"
        )

        // When
        val jsonString = json.encodeToString(content)

        // Then
        assert(jsonString.contains("\"type\":\"text\""))
        assert(jsonString.contains("\"text\":\"请识别这个物品\""))
    }

    @Test
    fun `test Content with image_url only`() {
        // Given
        val content = Content(
            type = "image_url",
            imageUrl = ImageUrl(url = "https://example.com/image.jpg")
        )

        // When
        val jsonString = json.encodeToString(content)

        // Then
        assert(jsonString.contains("\"type\":\"image_url\""))
        assert(jsonString.contains("\"image_url\""))
        assert(jsonString.contains("\"url\":\"https://example.com/image.jpg\""))
    }

    @Test
    fun `test ImageAnalysisRequest with default values`() {
        // Given
        val request = ImageAnalysisRequest(
            messages = listOf(
                Message(
                    role = "user",
                    content = listOf(
                        Content(type = "text", text = "test")
                    )
                )
            )
        )

        // Then
        assertEquals("gpt-4-vision-preview", request.model)
        assertEquals(500, request.maxTokens)
    }

    @Test
    fun `test round-trip serialization for ImageAnalysisRequest`() {
        // Given
        val original = ImageAnalysisRequest(
            model = "gpt-4-vision-preview",
            messages = listOf(
                Message(
                    role = "user",
                    content = listOf(
                        Content(type = "text", text = "识别物品"),
                        Content(type = "image_url", imageUrl = ImageUrl(url = "data:image/jpeg;base64,abc"))
                    )
                )
            ),
            maxTokens = 300
        )

        // When
        val jsonString = json.encodeToString(original)
        val deserialized = json.decodeFromString<ImageAnalysisRequest>(jsonString)

        // Then
        assertEquals(original.model, deserialized.model)
        assertEquals(original.maxTokens, deserialized.maxTokens)
        assertEquals(original.messages.size, deserialized.messages.size)
        assertEquals(original.messages[0].role, deserialized.messages[0].role)
        assertEquals(original.messages[0].content.size, deserialized.messages[0].content.size)
    }

    @Test
    fun `test round-trip serialization for ImageAnalysisResponse`() {
        // Given
        val original = ImageAnalysisResponse(
            id = "test-id-123",
            choices = listOf(
                Choice(
                    message = MessageResponse(
                        content = """{"name":"测试物品","description":"描述","estimatedPrice":100.0,"tags":["tag1"]}"""
                    )
                )
            )
        )

        // When
        val jsonString = json.encodeToString(original)
        val deserialized = json.decodeFromString<ImageAnalysisResponse>(jsonString)

        // Then
        assertEquals(original.id, deserialized.id)
        assertEquals(original.choices.size, deserialized.choices.size)
        assertEquals(original.choices[0].message.content, deserialized.choices[0].message.content)
    }
}
