package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.recommendation.RecommendationEngine
import javax.inject.Inject

/**
 * 获取"猜你喜欢"推荐物品的 Use Case。
 *
 * 封装单一业务逻辑：调用 [RecommendationEngine] 计算每个物品的 Recommendation_Score，
 * 并返回按分数降序排列的推荐物品列表。推荐引擎内部已经通过 ItemRepository 读取候选物品
 * 及用户交互记录，因此本 Use Case 主要负责对引擎调用进行封装，并以 [Result] 形式
 * 向上层（如 ViewModel）传播成功结果或失败信息。
 *
 * 设计说明：
 * - 调用方只需提供用户ID与可选的位置信息，无需关心评分细节。
 * - 当推荐计算失败时（如数据读取异常），失败信息通过 [Result.failure] 向上传播，
 *   调用方可据此进行降级处理（如展示空状态或提示）。
 *
 * @property recommendationEngine 推荐引擎，负责计算推荐分数并按分数降序返回物品。
 *
 * **验证需求: Requirements 3.1, 3.4**
 */
class GetRecommendedItemsUseCase @Inject constructor(
    private val recommendationEngine: RecommendationEngine
) {

    /**
     * 获取推荐物品列表。
     *
     * @param userId 当前用户ID，用于读取其交互记录。
     * @param userLocation 当前用户位置，可为 null（无定位信息时使用默认距离分数）。
     * @param limit 返回的最大物品数量，默认 10。
     * @return 成功时返回 [Result.success] 包装的、按 Recommendation_Score 降序排列的物品列表；
     *         计算失败时返回 [Result.failure]，由调用方决定降级处理。
     */
    suspend operator fun invoke(
        userId: String,
        userLocation: Location?,
        limit: Int = 10
    ): Result<List<Item>> = runCatching {
        recommendationEngine.getRecommendedItems(
            userId = userId,
            userLocation = userLocation,
            limit = limit
        )
    }
}
