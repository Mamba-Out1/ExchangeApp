package com.example.exchangeapp.domain.matching

import com.example.exchangeapp.domain.model.MatchedItem

/**
 * 匹配系统接口（"以物换物"匹配）。
 *
 * 基于标签相似度与描述关键词相似度计算每个候选物品与源物品的 Matching_Score，
 * 并返回按分数降序排列、超过最低阈值的匹配结果。
 *
 * Matching_Score = (标签相似度 * TAG_WEIGHT) + (关键词相似度 * KEYWORD_WEIGHT)
 *
 * **验证需求: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**
 */
interface MatchingSystem {
    /**
     * 获取与源物品匹配的物品列表。
     *
     * @param sourceItemId 源物品ID，作为匹配基准。
     * @param limit 返回的最大匹配物品数量，默认 5。
     * @return 按匹配分数降序排列、最多 [limit] 个匹配物品；源物品不存在时返回空列表。
     */
    suspend fun getMatchedItems(
        sourceItemId: String,
        limit: Int = 5
    ): List<MatchedItem>
}
