package com.example.exchangeapp.domain.validation

import com.example.exchangeapp.domain.exception.ValidationException
import com.example.exchangeapp.domain.model.RegisterFormData

/**
 * 注册表单验证器
 *
 * 负责验证用户注册表单数据的完整性和有效性。
 * 设计说明：
 * - 遵循 Requirement 11.2: "THE App SHALL支持手机号码和密码登录/注册"
 * - 验证以下必填字段：
 *   1. phone: 手机号码（不能为空，且需匹配中国大陆手机号格式 `^1[3-9]\d{9}$`）
 *   2. password: 密码（不能为空，至少6位，且需同时包含字母和数字）
 *   3. confirmPassword: 确认密码（不能为空，且需与 password 完全一致）
 *   4. nickname: 昵称（不能为空或仅空白字符）
 * - 返回包含所有验证失败字段名的 [ValidationException]，便于UI针对性地高亮显示
 *   失败字段名包括："phone"、"password"、"confirmPassword"、"nickname"
 * - 如果验证通过，返回 Result.success(Unit)
 *
 * **验证需求: Requirements 11.2**
 */
class RegisterFormValidator {

    companion object {
        /** 中国大陆手机号格式：以1开头，第二位为3-9，共11位数字 */
        private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")

        /** 密码最小长度 */
        private const val MIN_PASSWORD_LENGTH = 6
    }

    /**
     * 验证注册表单数据
     *
     * 收集所有验证失败的字段，便于UI一次性提示用户所有问题。
     *
     * @param formData 要验证的注册表单数据
     * @return 验证结果，成功时返回 [Result.success]，失败时返回 [Result.failure] 包装的 [ValidationException]
     */
    fun validate(formData: RegisterFormData): Result<Unit> {
        val invalidFields = mutableListOf<String>()

        // 验证手机号格式
        if (formData.phone.isBlank() || !PHONE_REGEX.matches(formData.phone)) {
            invalidFields.add("phone")
        }

        // 验证密码强度（至少6位，且包含字母和数字）
        if (!isPasswordStrong(formData.password)) {
            invalidFields.add("password")
        }

        // 验证确认密码与密码一致性
        if (formData.confirmPassword.isBlank() || formData.confirmPassword != formData.password) {
            invalidFields.add("confirmPassword")
        }

        // 验证昵称非空
        if (formData.nickname.isBlank()) {
            invalidFields.add("nickname")
        }

        // 如果有验证失败字段，返回验证异常
        return if (invalidFields.isNotEmpty()) {
            Result.failure(ValidationException(invalidFields))
        } else {
            Result.success(Unit)
        }
    }

    /**
     * 便捷方法：验证并抛出异常（适用于ViewModel层）
     *
     * @param formData 要验证的注册表单数据
     * @throws ValidationException 如果验证失败
     */
    @Throws(ValidationException::class)
    fun validateOrThrow(formData: RegisterFormData) {
        validate(formData).getOrThrow()
    }

    /**
     * 便捷方法：检查注册表单是否有效（不抛出异常）
     *
     * @param formData 要检查的注册表单数据
     * @return true 如果表单有效，false 如果有任何验证失败字段
     */
    fun isValid(formData: RegisterFormData): Boolean {
        return validate(formData).isSuccess
    }

    /**
     * 检查密码是否满足最低强度要求：至少6位，且同时包含字母和数字。
     */
    private fun isPasswordStrong(password: String): Boolean {
        if (password.length < MIN_PASSWORD_LENGTH) return false
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }
}
