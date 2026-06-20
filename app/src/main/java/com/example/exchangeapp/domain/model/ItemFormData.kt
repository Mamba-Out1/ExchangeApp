package com.example.exchangeapp.domain.model

import kotlinx.serialization.Serializable

/**
 * 物品表单数据模型
 *
 * 用于物品发布表单的数据传输对象，包含用户输入的所有物品信息。
 * 设计说明：
 * - 此模型专门用于表单验证场景，不直接对应数据库实体
 * - 字段定义遵循 Requirement 6 的物品发布需求：
 *   - name: 物品名称 (必填)
 *   - description: 物品描述 (必填)
 *   - price: 物品价格 (必填，需为正数)
 *   - images: 物品图片列表 (必填，最少1张，最多9张)
 *   - tags: 物品标签列表 (可选)
 * - 验证逻辑由 [ItemFormValidator] 处理
 *
 * **验证需求: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 14.7**
 */
@Serializable
data class ItemFormData(
    val name: String,
    val description: String,
    val price: Double,
    val images: List<String>,
    val tags: List<String> = emptyList()
) {
    /**
     * 检查表单数据是否有效（所有必填字段都非空/非空列表）
     * 这是一个便捷方法，实际验证由 [ItemFormValidator] 完成
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
               description.isNotBlank() &&
               price > 0 &&
               images.isNotEmpty()
    }
}