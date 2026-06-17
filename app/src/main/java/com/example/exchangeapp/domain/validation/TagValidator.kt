package com.example.exchangeapp.domain.validation

import com.example.exchangeapp.domain.exception.ValidationException
import com.example.exchangeapp.domain.model.ItemCategory

/**
 * 物品标签验证器
 *
 * 负责验证物品标签是否符合预定义分类体系以及数量限制。
 * 设计说明：
 * - 遵循 Requirement 14.3: "WHEN Item被分配Item_Tag, THE App SHALL验证Item_Tag属于预定义分类"
 * - 遵循 Requirement 14.7: "THE App SHALL允许一个Item拥有最多5个Item_Tag"
 * - 验证规则：
 *   1. 每个标签都必须属于 [ItemCategory] 预定义分类（电子产品、书籍、服装、运动器材、生活用品、其他）
 *   2. 标签总数不能超过 [ItemCategory.MAX_TAGS_PER_ITEM]（5个）
 * - 验证失败时返回包含错误字段标识的 [ValidationException]：
 *   - "tags" 表示标签数量超过上限
 *   - "tag:<标签名>" 表示该标签不属于预定义分类
 *
 * **验证需求: Requirements 14.3, 14.7**
 */
class TagValidator {

    /**
     * 验证物品标签列表。
     *
     * @param tags 要验证的标签列表
     * @return 验证结果，成功时返回 [Result.success]，失败时返回包装 [ValidationException] 的 [Result.failure]
     */
    fun validate(tags: List<String>): Result<Unit> {
        val invalidFields = mutableListOf<String>()

        // 验证标签数量上限 (Requirement 14.7)
        if (tags.size > ItemCategory.MAX_TAGS_PER_ITEM) {
            invalidFields.add("tags")
        }

        // 验证每个标签都属于预定义分类 (Requirement 14.3)
        tags.forEach { tag ->
            if (!ItemCategory.isValid(tag)) {
                invalidFields.add("tag:$tag")
            }
        }

        return if (invalidFields.isNotEmpty()) {
            Result.failure(ValidationException(invalidFields))
        } else {
            Result.success(Unit)
        }
    }

    /**
     * 校验单个标签是否属于预定义分类。
     *
     * @param tag 标签名称
     * @return true 如果标签属于预定义分类，否则 false
     */
    fun isValidTag(tag: String): Boolean {
        return ItemCategory.isValid(tag)
    }

    /**
     * 检查标签列表是否有效（不抛出异常）。
     *
     * @param tags 要检查的标签列表
     * @return true 如果所有标签合法且数量未超限，否则 false
     */
    fun isValid(tags: List<String>): Boolean {
        return validate(tags).isSuccess
    }

    /**
     * 便捷方法：验证并在失败时抛出异常（适用于 ViewModel 层）。
     *
     * @param tags 要验证的标签列表
     * @throws ValidationException 如果验证失败
     */
    @Throws(ValidationException::class)
    fun validateOrThrow(tags: List<String>) {
        validate(tags).getOrThrow()
    }
}
