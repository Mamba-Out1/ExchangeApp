package com.example.exchangeapp.di

import javax.inject.Qualifier

/**
 * 限定符：OpenAI API 密钥（String）。
 *
 * 用于区分多个 [String] 类型的绑定，避免 Dagger 重复绑定冲突。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiKey

/**
 * 限定符：OpenAI API 端点地址（String）。
 *
 * 用于区分多个 [String] 类型的绑定，避免 Dagger 重复绑定冲突。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiEndpoint

/**
 * 限定符：DashScope/Qwen model name.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiModel
