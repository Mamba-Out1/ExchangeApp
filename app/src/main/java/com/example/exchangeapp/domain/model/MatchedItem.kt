package com.example.exchangeapp.domain.model

/**
 * 匹配物品及其对应的匹配分数。
 *
 * 用于匹配系统返回与源物品相似的物品，并按 [matchingScore] 降序排列。
 */
data class MatchedItem(
    val item: Item,
    val matchingScore: Double
)
