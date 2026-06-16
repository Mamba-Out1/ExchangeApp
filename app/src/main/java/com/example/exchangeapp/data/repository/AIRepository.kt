package com.example.exchangeapp.data.repository

import com.example.exchangeapp.BuildConfig
import com.example.exchangeapp.data.remote.api.OpenAIApiService
import com.example.exchangeapp.data.remote.dto.Content
import com.example.exchangeapp.data.remote.dto.ImageAnalysisRequest
import com.example.exchangeapp.data.remote.dto.ImageUrl
import com.example.exchangeapp.data.remote.dto.Message
import com.example.exchangeapp.domain.model.ItemRecognitionResult
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Repository接口
 * 负责调用OpenAI GPT-4V API进行物品图像识别
 */
interface AIRepository {
    /**
     * 识别物品图像并返回识别结果
     * 
     * @param imageBase64 Base64编码的图像字符串
     * @return Result包含ItemRecognitionResult或错误信息
     */
    suspend fun recognizeItem(imageBase64: String): Result<ItemRecognitionResult>
}

/**
 * AI Repository实现类
 * 
 * @property apiService OpenAI API服务接口
 * @property apiKey OpenAI API密钥
 */
@Singleton
class AIRepositoryImpl @Inject constructor(
    private val apiService: OpenAIApiService,
    private val apiKey: String = BuildConfig.OPENAI_API_KEY
) : AIRepository {
    
    companion object {
        private val RECOGNITION_PROMPT = """
请识别这个物品并返回JSON格式的信息，包含以下字段：
- name: 物品名称（字符串）
- description: 物品描述（字符串，50-200字）
- estimatedPrice: 估价（数字，单位为元）
- tags: 标签列表（字符串数组，从以下类别中选择：电子产品、书籍、服装、运动器材、生活用品、其他）

请确保返回的是有效的JSON格式，不要包含其他解释性文字。
        """.trimIndent()
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    override suspend fun recognizeItem(imageBase64: String): Result<ItemRecognitionResult> {
        // 验证API密钥是否配置
        if (apiKey.isBlank()) {
            return Result.failure(
                IllegalStateException("OpenAI API密钥未配置，请在local.properties中配置OPENAI_API_KEY")
            )
        }
        
        return try {
            // 构建请求对象
            val request = ImageAnalysisRequest(
                model = "gpt-4-vision-preview",
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            Content(
                                type = "text",
                                text = RECOGNITION_PROMPT
                            ),
                            Content(
                                type = "image_url",
                                imageUrl = ImageUrl(
                                    url = "data:image/jpeg;base64,$imageBase64"
                                )
                            )
                        )
                    )
                ),
                maxTokens = 500
            )
            
            // 调用API
            val response = apiService.analyzeImage(
                authorization = "Bearer $apiKey",
                request = request
            )
            
            // 处理响应
            when {
                response.isSuccessful -> {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    if (content != null) {
                        parseRecognitionResult(content)
                    } else {
                        Result.failure(
                            IllegalStateException("API返回空响应内容")
                        )
                    }
                }
                response.code() == 401 -> {
                    Result.failure(
                        IllegalStateException("API密钥无效，请检查local.properties中的OPENAI_API_KEY配置")
                    )
                }
                response.code() == 429 -> {
                    Result.failure(
                        IllegalStateException("API请求频率超限，请稍后重试")
                    )
                }
                response.code() in 500..599 -> {
                    Result.failure(
                        IllegalStateException("OpenAI服务暂时不可用（错误码：${response.code()}），请稍后重试")
                    )
                }
                else -> {
                    Result.failure(
                        IllegalStateException("API请求失败（错误码：${response.code()}）：${response.message()}")
                    )
                }
            }
        } catch (e: SerializationException) {
            // JSON序列化错误
            Result.failure(
                IllegalStateException("请求数据序列化失败：${e.message}", e)
            )
        } catch (e: java.net.SocketTimeoutException) {
            // 网络超时
            Result.failure(
                IllegalStateException("网络请求超时，请检查网络连接后重试", e)
            )
        } catch (e: java.net.UnknownHostException) {
            // 网络不可用
            Result.failure(
                IllegalStateException("网络不可用，请检查网络连接", e)
            )
        } catch (e: java.io.IOException) {
            // 其他IO错误
            Result.failure(
                IllegalStateException("网络请求失败：${e.message}", e)
            )
        } catch (e: Exception) {
            // 其他未知错误
            Result.failure(
                IllegalStateException("AI识别失败：${e.message}", e)
            )
        }
    }
    
    /**
     * 解析OpenAI API返回的JSON响应
     * 
     * @param jsonString OpenAI API返回的JSON字符串
     * @return Result包含解析后的ItemRecognitionResult或错误信息
     *
     * 注: 标记为internal以便在单元测试中验证JSON解析的错误鲁棒性(Property 9)
     */
    internal fun parseRecognitionResult(jsonString: String): Result<ItemRecognitionResult> {
        return try {
            // 清理可能存在的markdown代码块标记
            val cleanedJson = jsonString
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            // 解析JSON
            val result = json.decodeFromString<ItemRecognitionResult>(cleanedJson)
            
            // 验证解析结果的有效性
            when {
                result.name.isBlank() -> {
                    Result.failure(
                        IllegalStateException("解析结果缺少物品名称")
                    )
                }
                result.description.isBlank() -> {
                    Result.failure(
                        IllegalStateException("解析结果缺少物品描述")
                    )
                }
                result.estimatedPrice <= 0 -> {
                    Result.failure(
                        IllegalStateException("解析结果的估价无效（必须大于0）")
                    )
                }
                result.tags.isEmpty() -> {
                    Result.failure(
                        IllegalStateException("解析结果缺少标签")
                    )
                }
                else -> Result.success(result)
            }
        } catch (e: SerializationException) {
            // JSON解析失败
            Result.failure(
                IllegalStateException("无法解析AI识别结果，返回的JSON格式不正确：${e.message}", e)
            )
        } catch (e: IllegalArgumentException) {
            // JSON格式错误
            Result.failure(
                IllegalStateException("AI识别结果格式错误：${e.message}", e)
            )
        } catch (e: Exception) {
            // 其他解析错误
            Result.failure(
                IllegalStateException("解析AI识别结果时发生错误：${e.message}", e)
            )
        }
    }
}
