package com.example.exchangeapp.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * OpenAI API重试拦截器
 * 
 * 实现指数退避重试机制，用于处理网络请求失败的情况
 * - 对于429 (Rate Limit)或5xx服务器错误，最多重试3次
 * - 使用指数退避策略：第1次重试等待1秒，第2次等待2秒，第3次等待4秒
 * - 对于4xx错误（除429外），不进行重试
 * 
 * **验证需求: Requirements 13.5, 13.6**
 */
class OpenAIRetryInterceptor : Interceptor {
    
    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val INITIAL_BACKOFF_MS = 1000L
    }
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0
        
        // 重试逻辑：针对429和5xx错误
        while (!response.isSuccessful && tryCount < MAX_RETRY_COUNT) {
            val shouldRetry = when (response.code) {
                429 -> true  // Rate limit exceeded
                in 500..599 -> true  // Server errors
                else -> false
            }
            
            if (!shouldRetry) {
                break
            }
            
            tryCount++
            response.close()
            
            // 指数退避：1秒 -> 2秒 -> 4秒
            val backoffTime = INITIAL_BACKOFF_MS * (1 shl (tryCount - 1))
            Thread.sleep(backoffTime)
            
            // 重试请求
            response = chain.proceed(request)
        }
        
        return response
    }
}
