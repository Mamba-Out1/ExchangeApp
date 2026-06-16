package com.example.exchangeapp.domain.recommendation

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Location

/**
 * 推荐引擎接口（"猜你喜欢"）。
 *
 * 基于规则算法计算每个物品的 Recommendation_Score，并返回按分数降序排列的推荐结果。
 *
 * Recommendation_Score = 距离权重 + 点击权重 + 收藏权重
 *
 * **验证需求: Requirements 3.1, 3.2, 3.3, 3.5, 3.6**
 */
interface RecommendationEngine {
    /**
     * 获取推荐物品列表。
     *
     * @param userId 当前用户ID，用于读取其交互记录。
     * @param userLocation 当前用户位置，可为 null（无定位信息时使用默认距离分数）。
     * @param limit 返回的最大物品数量，默认 10。
     * @return 按推荐分数降序排列、最多 [limit] 个物品。
     */
    suspend fun getRecommendedItems(
        userId: String,
        userLocation: Location?,
        limit: Int = 10
    ): List<Item>

    /**
     * 当用户点击某个物品时调用，增加该物品的点击权重。
     *
     * **验证需求: Requirements 3.5**
     */
    fun updateClickWeight(itemId: String)

    /**
     * 当用户收藏某个物品时调用，增加该物品的收藏权重。
     *
     * **验证需求: Requirements 3.6**
     */
    fun updateFavoriteWeight(itemId: String)

    /**
     * 重新计算推荐分数。
     *
     * 当前实现按需在 [getRecommendedItems] 中即时计算分数，因此无需缓存的重算逻辑。
     */
    suspend fun recalculateScores()
}
