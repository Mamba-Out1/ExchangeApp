package com.example.exchangeapp.domain.model

/**
 * 物品及其对应的推荐分数。
 *
 * 用于推荐引擎内部对物品按 [score] 进行降序排序。
 */
data class ScoredItem(
    val item: Item,
    val score: Double
)
