package com.example.exchangeapp.data.repository

import com.example.exchangeapp.BuildConfig
import com.example.exchangeapp.data.remote.api.OpenAIApiService
import com.example.exchangeapp.data.remote.dto.Content
import com.example.exchangeapp.data.remote.dto.ImageAnalysisRequest
import com.example.exchangeapp.data.remote.dto.ImageUrl
import com.example.exchangeapp.data.remote.dto.Message
import com.example.exchangeapp.domain.model.ItemRecognitionResult
import com.example.exchangeapp.domain.model.WantedItemTagsResult
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

interface AIRepository {
    suspend fun recognizeItem(imageBase64: String): Result<ItemRecognitionResult>
    suspend fun parseWantedItemTags(itemName: String): Result<List<String>>
}

@Singleton
class AIRepositoryImpl @Inject constructor(
    private val apiService: OpenAIApiService,
    private val apiKey: String = BuildConfig.DASHSCOPE_API_KEY,
    private val model: String = BuildConfig.DASHSCOPE_MODEL
) : AIRepository {

    companion object {
        private val RECOGNITION_PROMPT = """
请识别图片中的校园二手物品，并且只返回一个合法 JSON object，不要返回 Markdown 或解释性文字。
JSON 字段必须为：
- name: 中文物品名称，字符串
- description: 中文物品描述，字符串，包含成色、型号、可见配件等，控制在 200 字以内
- estimatedPrice: 二手估价，数字，单位人民币元
- tags: 商品标签数组，必须为英文小写标签。一个商品可以有多个标签，例如 AirPods 可返回 ["airpods","earphones","bluetooth","apple"]

如果图片信息不完整，请基于可见信息给出合理估计，但仍必须返回上述 JSON 字段。
        """.trimIndent()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun recognizeItem(imageBase64: String): Result<ItemRecognitionResult> {
        if (apiKey.isBlank()) {
            return Result.failure(
                IllegalStateException("DashScope API key 未配置，请在 local.properties 中配置 DASHSCOPE_API_KEY")
            )
        }

        return try {
            val request = ImageAnalysisRequest(
                model = model,
                messages = listOf(
                    Message(
                        role = "system",
                        content = listOf(
                            Content(
                                type = "text",
                                text = "You are an item recognition assistant. Return JSON only."
                            )
                        )
                    ),
                    Message(
                        role = "user",
                        content = listOf(
                            Content(type = "text", text = RECOGNITION_PROMPT),
                            Content(
                                type = "image_url",
                                imageUrl = ImageUrl(url = "data:image/jpeg;base64,$imageBase64")
                            )
                        )
                    )
                ),
                maxTokens = 500
            )

            val response = apiService.analyzeImage(
                authorization = "Bearer $apiKey",
                request = request
            )

            when {
                response.isSuccessful -> {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    if (content != null) {
                        parseRecognitionResult(content)
                    } else {
                        Result.failure(IllegalStateException("API 返回空响应内容"))
                    }
                }
                response.code() == 401 -> {
                    Result.failure(
                        IllegalStateException("API key 无效，请检查 local.properties 中的 DASHSCOPE_API_KEY 配置")
                    )
                }
                response.code() == 429 -> {
                    Result.failure(IllegalStateException("API 请求频率超限，请稍后重试"))
                }
                response.code() in 500..599 -> {
                    Result.failure(
                        IllegalStateException("DashScope 服务暂时不可用（错误码：${response.code()}），请稍后重试")
                    )
                }
                else -> {
                    Result.failure(
                        IllegalStateException("API 请求失败（错误码：${response.code()}）：${response.message()}")
                    )
                }
            }
        } catch (e: SerializationException) {
            Result.failure(IllegalStateException("请求数据序列化失败：${e.message}", e))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(IllegalStateException("网络请求超时，请检查网络连接后重试", e))
        } catch (e: java.net.UnknownHostException) {
            Result.failure(IllegalStateException("网络不可用，请检查网络连接", e))
        } catch (e: java.io.IOException) {
            Result.failure(IllegalStateException("网络请求失败：${e.message}", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("AI 识别失败：${e.message}", e))
        }
    }

    override suspend fun parseWantedItemTags(itemName: String): Result<List<String>> {
        val normalizedName = itemName.trim()
        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException("想要的物品名不能为空"))
        }
        if (apiKey.isBlank()) {
            val fallback = buildWantedTagFallback(normalizedName)
            return if (fallback.isNotEmpty()) {
                Result.success(fallback)
            } else {
                Result.failure(
                    IllegalStateException("DashScope API key 未配置，请在 local.properties 中配置 DASHSCOPE_API_KEY")
                )
            }
        }

        return try {
            val request = buildJsonObject {
                put("model", model)
                put(
                    "messages",
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put("role", "system")
                                put("content", "Return JSON only. Extract searchable English lowercase product tags.")
                            },
                            buildJsonObject {
                                put("role", "user")
                                put(
                                    "content",
                                    """
请把用户想要的物品名解析成用于校园以物易物匹配的英文标签，并只返回 JSON object。
JSON 格式：{"tags":["tag1","tag2"]}。
返回 3 到 6 个标签。标签必须英文小写，包含品牌、型号、品类、关键属性。用户输入：$normalizedName
                                    """.trimIndent()
                                )
                            }
                        )
                    )
                )
                put("max_tokens", 120)
                put("temperature", 0)
                put(
                    "response_format",
                    buildJsonObject {
                        put("type", "json_object")
                    }
                )
            }

            val response = apiService.analyzeText(
                authorization = "Bearer $apiKey",
                request = request
            )
            if (!response.isSuccessful) {
                return Result.failure(
                    IllegalStateException("标签解析失败（错误码：${response.code()}）：${response.message()}")
                )
            }

            val content = response.body()?.choices?.firstOrNull()?.message?.content
                ?: return Result.failure(IllegalStateException("API 返回空响应内容"))

            val parsed = parseWantedTagsResult(content)
            if (parsed.isSuccess) {
                parsed
            } else {
                val fallback = buildWantedTagFallback(normalizedName)
                if (fallback.isNotEmpty()) Result.success(fallback) else parsed
            }
        } catch (e: java.net.SocketTimeoutException) {
            val fallback = buildWantedTagFallback(normalizedName)
            if (fallback.isNotEmpty()) {
                Result.success(fallback)
            } else {
                Result.failure(IllegalStateException("想要的商品标签解析超时，请稍后重试", e))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("想要的商品标签解析失败：${e.message}", e))
        }
    }

    internal fun parseRecognitionResult(jsonString: String): Result<ItemRecognitionResult> {
        return try {
            val cleanedJson = cleanJson(jsonString)
            val result = json.decodeFromString<ItemRecognitionResult>(cleanedJson)

            when {
                result.name.isBlank() -> Result.failure(IllegalStateException("解析结果缺少物品名称"))
                result.description.isBlank() -> Result.failure(IllegalStateException("解析结果缺少物品描述"))
                result.estimatedPrice <= 0 -> Result.failure(IllegalStateException("解析结果的估价无效，必须大于 0"))
                result.tags.isEmpty() -> Result.failure(IllegalStateException("解析结果缺少标签"))
                else -> Result.success(result)
            }
        } catch (e: SerializationException) {
            Result.failure(IllegalStateException("无法解析 AI 识别结果，返回的 JSON 格式不正确：${e.message}", e))
        } catch (e: IllegalArgumentException) {
            Result.failure(IllegalStateException("AI 识别结果格式错误：${e.message}", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("解析 AI 识别结果时发生错误：${e.message}", e))
        }
    }

    internal fun parseWantedTagsResult(jsonString: String): Result<List<String>> {
        return try {
            val result = json.decodeFromString<WantedItemTagsResult>(cleanJson(jsonString))
            val tags = normalizeTags(result.tags)
            if (tags.isEmpty()) {
                Result.failure(IllegalStateException("解析结果缺少想要的商品标签"))
            } else {
                Result.success(tags)
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("无法解析想要的商品标签 JSON：${e.message}", e))
        }
    }

    private fun cleanJson(value: String): String {
        return value
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun normalizeTags(tags: List<String>): List<String> {
        return tags
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun buildWantedTagFallback(itemName: String): List<String> {
        val lower = itemName.lowercase()
        val tags = linkedSetOf<String>()

        fun add(vararg values: String) {
            values.forEach { tags.add(it) }
        }

        when {
            lower.contains("计算器") || lower.contains("calculator") -> add("calculator", "electronics", "study", "math", "stationery")
            lower.contains("耳机") || lower.contains("airpods") || lower.contains("headphone") -> add("earphones", "headphones", "bluetooth", "audio")
            lower.contains("书") || lower.contains("教材") || lower.contains("book") -> add("book", "textbook", "study", "education")
            lower.contains("自行车") || lower.contains("bike") -> add("bicycle", "transport", "sports")
            lower.contains("键盘") || lower.contains("keyboard") -> add("keyboard", "computer", "electronics", "peripheral")
            lower.contains("鼠标") || lower.contains("mouse") -> add("mouse", "computer", "electronics", "peripheral")
            lower.contains("手机") || lower.contains("iphone") || lower.contains("phone") -> add("phone", "smartphone", "electronics")
            lower.contains("电脑") || lower.contains("笔记本") || lower.contains("laptop") -> add("laptop", "computer", "electronics")
            lower.contains("杯") || lower.contains("水杯") || lower.contains("bottle") -> add("bottle", "cup", "daily", "drinkware")
            lower.contains("台灯") || lower.contains("lamp") -> add("lamp", "lighting", "desk", "study")
        }

        val asciiWords = lower
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 1 }
        add(*asciiWords.toTypedArray())

        if (tags.isEmpty() && itemName.isNotBlank()) {
            add("wanted", "campus", "secondhand")
        }

        return tags.take(6)
    }
}
