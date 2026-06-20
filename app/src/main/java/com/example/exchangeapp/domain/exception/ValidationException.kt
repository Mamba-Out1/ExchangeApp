package com.example.exchangeapp.domain.exception

/**
 * 表单验证异常
 *
 * 当物品表单验证失败时抛出，包含所有缺失或无效的字段信息。
 * 设计说明：
 * - 遵循 Requirement 6.8: "IF 必填信息未填写, THEN THE App SHALL高亮显示缺失字段并提示User补充"
 * - 包含所有缺失字段的列表，以便UI可以针对性地高亮显示和提示用户
 * - 异常信息提供用户友好的提示
 *
 * **验证需求: Requirements 6.6, 6.8**
 */
class ValidationException(
    val missingFields: List<String>,
    message: String = "表单验证失败，请检查以下字段：${missingFields.joinToString(", ")}"
) : Exception(message) {
    
    companion object {
        /**
         * 创建包含单个缺失字段的验证异常
         */
        fun single(field: String): ValidationException {
            return ValidationException(listOf(field), "请填写 $field")
        }
        
        /**
         * 创建包含多个缺失字段的验证异常
         */
        fun multiple(fields: List<String>): ValidationException {
            return ValidationException(fields)
        }
    }
}