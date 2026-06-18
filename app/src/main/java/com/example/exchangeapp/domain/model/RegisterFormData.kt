package com.example.exchangeapp.domain.model

import kotlinx.serialization.Serializable

/**
 * 注册表单数据模型
 *
 * 用于用户注册表单的数据传输对象，包含用户输入的注册信息。
 * 设计说明：
 * - 此模型专门用于注册表单验证场景，不直接对应数据库实体
 * - 字段定义遵循 Requirement 11.2: "THE App SHALL支持手机号码和密码登录/注册"
 *   - phone: 手机号码 (必填，需为有效的中国大陆手机号)
 *   - password: 密码 (必填，需满足最低强度要求)
 *   - confirmPassword: 确认密码 (必填，需与 password 一致)
 *   - nickname: 昵称 (必填，不能为空)
 * - 验证逻辑由 [com.example.exchangeapp.domain.validation.RegisterFormValidator] 处理
 *
 * **验证需求: Requirements 11.2**
 */
@Serializable
data class RegisterFormData(
    val phone: String,
    val password: String,
    val confirmPassword: String,
    val nickname: String
)
