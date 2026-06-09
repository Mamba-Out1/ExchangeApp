package com.example.exchangeapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI图像分析请求DTO
 * 
 * @property model 使用的模型，默认为gpt-4-vision-preview
 * @property messages 消息列表
 * @property maxTokens 最大token数量，默认为500
 */
@Serializable
data class ImageAnalysisRequest(
    @SerialName("model")
    val model: String = "gpt-4-vision-preview",
    
    @SerialName("messages")
    val messages: List<Message>,
    
    @SerialName("max_tokens")
    val maxTokens: Int = 500
)

/**
 * 消息DTO
 * 
 * @property role 角色（如"user"、"system"）
 * @property content 消息内容列表
 */
@Serializable
data class Message(
    @SerialName("role")
    val role: String,
    
    @SerialName("content")
    val content: List<Content>
)

/**
 * 消息内容DTO
 * 
 * @property type 内容类型（"text"或"image_url"）
 * @property text 文本内容（当type为"text"时使用）
 * @property imageUrl 图片URL对象（当type为"image_url"时使用）
 */
@Serializable
data class Content(
    @SerialName("type")
    val type: String, // "text" or "image_url"
    
    @SerialName("text")
    val text: String? = null,
    
    @SerialName("image_url")
    val imageUrl: ImageUrl? = null
)

/**
 * 图片URL DTO
 * 
 * @property url 图片URL，支持Base64编码的图片或HTTP(S) URL
 *               格式示例: "data:image/jpeg;base64,{base64_string}" 或 "https://example.com/image.jpg"
 */
@Serializable
data class ImageUrl(
    @SerialName("url")
    val url: String
)
