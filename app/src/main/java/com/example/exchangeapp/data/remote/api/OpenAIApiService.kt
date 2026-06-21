package com.example.exchangeapp.data.remote.api

import com.example.exchangeapp.data.remote.dto.ImageAnalysisRequest
import com.example.exchangeapp.data.remote.dto.ImageAnalysisResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI API服务接口
 * 用于调用OpenAI GPT-4V API进行图像识别
 */
interface OpenAIApiService {
    /**
     * 分析图像并返回识别结果
     * 
     * @param authorization 授权令牌，格式为 "Bearer {API_KEY}"
     * @param request 图像分析请求对象
     * @return 包含识别结果的响应
     */
    @POST("v1/chat/completions")
    suspend fun analyzeImage(
        @Header("Authorization") authorization: String,
        @Body request: ImageAnalysisRequest
    ): Response<ImageAnalysisResponse>

    @POST("v1/chat/completions")
    suspend fun analyzeText(
        @Header("Authorization") authorization: String,
        @Body request: JsonObject
    ): Response<ImageAnalysisResponse>
}
