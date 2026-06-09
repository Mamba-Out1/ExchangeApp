package com.example.exchangeapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI图像分析响应DTO
 * 
 * @property id 响应ID
 * @property choices 响应选项列表
 */
@Serializable
data class ImageAnalysisResponse(
    @SerialName("id")
    val id: String,
    
    @SerialName("choices")
    val choices: List<Choice>
)

/**
 * 响应选项DTO
 * 
 * @property message 消息响应对象
 */
@Serializable
data class Choice(
    @SerialName("message")
    val message: MessageResponse
)

/**
 * 消息响应DTO
 * 
 * @property content 消息内容，通常为JSON格式的识别结果字符串
 *                  格式示例: {"name":"物品名称","description":"物品描述","estimatedPrice":100.0,"tags":["标签1","标签2"]}
 */
@Serializable
data class MessageResponse(
    @SerialName("content")
    val content: String
)
