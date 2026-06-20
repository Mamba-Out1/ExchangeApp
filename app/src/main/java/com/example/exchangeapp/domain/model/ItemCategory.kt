package com.example.exchangeapp.domain.model

import kotlinx.serialization.Serializable

/**
 * 物品预定义分类体系
 *
 * 维护 App 支持的预定义 Item_Tag 分类，物品标签必须属于这些分类之一。
 * 设计说明：
 * - 遵循 Requirement 14.1: "THE App SHALL维护预定义的Item_Tag分类体系"
 * - 遵循 Requirement 14.2: "THE App SHALL支持以下分类：电子产品、书籍、服装、运动器材、生活用品、其他"
 * - 标签在数据层以中文字符串（[displayName]）形式持久化，因此提供
 *   [fromDisplayName] / [isValid] 等便捷方法用于字符串与枚举之间的转换与校验。
 *
 * **验证需求: Requirements 14.1, 14.2**
 */
@Serializable
enum class ItemCategory(val displayName: String) {
    ELECTRONICS("电子产品"),
    BOOKS("书籍"),
    CLOTHING("服装"),
    SPORTS_EQUIPMENT("运动器材"),
    DAILY_NECESSITIES("生活用品"),
    OTHER("其他");

    companion object {
        /**
         * 一个物品最多允许拥有的标签数量。
         *
         * **验证需求: Requirement 14.7** - "THE App SHALL允许一个Item拥有最多5个Item_Tag"
         */
        const val MAX_TAGS_PER_ITEM = 5

        /**
         * 所有预定义分类的中文名称列表，顺序与枚举声明一致。
         * 用于 UI 展示（如标签筛选器）和标签校验。
         */
        val displayNames: List<String> = entries.map { it.displayName }

        /**
         * 根据中文名称查找对应的分类。
         *
         * @param displayName 分类的中文名称
         * @return 匹配的 [ItemCategory]，如果不存在则返回 null
         */
        fun fromDisplayName(displayName: String): ItemCategory? {
            return entries.find { it.displayName == displayName }
        }

        /**
         * 校验给定的标签名称是否属于预定义分类。
         *
         * @param tag 标签名称
         * @return true 如果标签属于预定义分类，否则 false
         */
        fun isValid(tag: String): Boolean {
            return fromDisplayName(tag) != null
        }
    }
}
