package com.example.exchangeapp.domain.validation

import com.example.exchangeapp.domain.exception.ValidationException
import com.example.exchangeapp.domain.model.ItemFormData

/**
 * 物品表单验证器
 *
 * 负责验证物品发布表单数据的完整性和有效性。
 * 设计说明：
 * - 遵循 Requirement 6.6: "WHEN User点击发布按钮, THE App SHALL验证所有必填信息已填写"
 * - 遵循 Requirement 6.8: "IF 必填信息未填写, THEN THE App SHALL高亮显示缺失字段并提示User补充"
 * - 验证以下必填字段：
 *   1. name: 物品名称（不能为空或仅空白字符）
 *   2. description: 物品描述（不能为空或仅空白字符）
 *   3. price: 物品价格（必须大于0）
 *   4. images: 物品图片列表（不能为空列表）
 * - 返回包含所有缺失字段的 [ValidationException]，便于UI针对性地高亮显示
 * - 如果验证通过，返回 Result.success(Unit)
 *
 * **验证需求: Requirements 6.6, 6.8**
 */
class ItemFormValidator {
    
    /**
     * 验证物品表单数据
     *
     * @param formData 要验证的表单数据
     * @return 验证结果，成功时返回 [Result.success]，失败时返回 [Result.failure] 包装的 [ValidationException]
     */
    fun validate(formData: ItemFormData): Result<Unit> {
        val missingFields = mutableListOf<String>()
        
        // 验证物品名称
        if (formData.name.isBlank()) {
            missingFields.add("name")
        }
        
        // 验证物品描述
        if (formData.description.isBlank()) {
            missingFields.add("description")
        }
        
        // 验证物品价格
        if (formData.price <= 0) {
            missingFields.add("price")
        }
        
        // 验证物品图片
        if (formData.images.isEmpty()) {
            missingFields.add("images")
        }
        
        // 如果有缺失字段，返回验证异常
        return if (missingFields.isNotEmpty()) {
            Result.failure(ValidationException(missingFields))
        } else {
            Result.success(Unit)
        }
    }
    
    /**
     * 便捷方法：验证并抛出异常（适用于ViewModel层）
     *
     * @param formData 要验证的表单数据
     * @throws ValidationException 如果验证失败
     */
    @Throws(ValidationException::class)
    fun validateOrThrow(formData: ItemFormData) {
        validate(formData).getOrThrow()
    }
    
    /**
     * 便捷方法：检查表单是否有效（不抛出异常）
     *
     * @param formData 要检查的表单数据
     * @return true 如果表单有效，false 如果有任何缺失字段
     */
    fun isValid(formData: ItemFormData): Boolean {
        return validate(formData).isSuccess
    }
}