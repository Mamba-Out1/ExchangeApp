package com.example.exchangeapp.domain.recommendation

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.model.ScoredItem
import com.example.exchangeapp.domain.model.UserInteractions
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.repository.UserInteractionRepository
import com.example.exchangeapp.domain.service.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * [RecommendationEngine] 的基于规则算法实现。
 *
 * Recommendation_Score 计算公式：
 * ```
 * score = (distanceScore  * DISTANCE_WEIGHT)
 *       + (clickCount * CLICK_SCORE_FACTOR * CLICK_WEIGHT)
 *       + (isFavorite ? 1.0 : 0.0) * FAVORITE_WEIGHT
 * ```
 * 其中 distanceScore = 1.0 - (distanceKm / MAX_DISTANCE_KM) 并裁剪到 [0.0, 1.0]，
 * distanceKm 由 [LocationService.calculateDistance] 返回的米数换算得到（米 / 1000）。
 *
 * @property itemRepository 物品仓库，用于获取候选物品。
 * @property userInteractionRepository 交互记录仓库，用于读取与更新点击 / 收藏权重。
 * @property locationService 位置服务，用于计算用户与物品之间的距离。
 * @property externalScope 用于异步执行点击 / 收藏权重持久化的协程作用域。
 *
 * **验证需求: Requirements 3.1, 3.2, 3.3, 3.5, 3.6**
 */
class RecommendationEngineImpl(
    private val itemRepository: ItemRepository,
    private val userInteractionRepository: UserInteractionRepository,
    private val locationService: LocationService,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : RecommendationEngine {

    companion object {
        /** 距离分数权重。 */
        const val DISTANCE_WEIGHT = 0.4

        /** 点击分数权重。 */
        const val CLICK_WEIGHT = 0.3

        /** 收藏分数权重。 */
        const val FAVORITE_WEIGHT = 0.3

        /** 单次点击转换为点击分数的系数。 */
        const val CLICK_SCORE_FACTOR = 0.1

        /** 推荐考虑的最大距离（公里），超过该距离的物品距离分数为 0。 */
        const val MAX_DISTANCE_KM = 10.0

        /** 缺少位置信息时使用的默认距离分数。 */
        const val DEFAULT_DISTANCE_SCORE = 0.5

        /** 米与公里之间的换算系数。 */
        const val METERS_PER_KM = 1000.0
    }

    override suspend fun getRecommendedItems(
        userId: String,
        userLocation: Location?,
        limit: Int
    ): List<Item> {
        val allItems = itemRepository.getAllItems()
        val interactions = userInteractionRepository.getUserInteractions(userId)

        return allItems
            .map { item ->
                ScoredItem(item, calculateRecommendationScore(item, userLocation, interactions))
            }
            .sortedByDescending { it.score }
            .take(limit)
            .map { it.item }
    }

    /**
     * 计算单个物品的 Recommendation_Score。
     *
     * **验证需求: Requirements 3.2, 3.3**
     */
    internal fun calculateRecommendationScore(
        item: Item,
        userLocation: Location?,
        interactions: UserInteractions
    ): Double {
        val distanceScore = calculateDistanceScore(item.location, userLocation)
        val clickScore = interactions.getClickCount(item.id) * CLICK_SCORE_FACTOR
        val favoriteScore = if (interactions.isFavorite(item.id)) 1.0 else 0.0

        return (distanceScore * DISTANCE_WEIGHT) +
            (clickScore * CLICK_WEIGHT) +
            (favoriteScore * FAVORITE_WEIGHT)
    }

    /**
     * 计算距离分数。
     *
     * - 当物品位置或用户位置缺失时返回 [DEFAULT_DISTANCE_SCORE]。
     * - 否则将 [LocationService.calculateDistance] 返回的米数换算为公里，
     *   按 `1.0 - (distanceKm / MAX_DISTANCE_KM)` 计算并裁剪到 [0.0, 1.0]。
     */
    internal fun calculateDistanceScore(
        itemLocation: Location?,
        userLocation: Location?
    ): Double {
        if (itemLocation == null || userLocation == null) return DEFAULT_DISTANCE_SCORE

        val distanceKm = locationService.calculateDistance(userLocation, itemLocation) / METERS_PER_KM
        return 1.0 - (distanceKm / MAX_DISTANCE_KM).coerceIn(0.0, 1.0)
    }

    override fun updateClickWeight(itemId: String) {
        externalScope.launch {
            userInteractionRepository.incrementClickCount(itemId)
        }
    }

    override fun updateFavoriteWeight(itemId: String) {
        externalScope.launch {
            userInteractionRepository.setFavorite(itemId, true)
        }
    }

    override suspend fun recalculateScores() {
        // 推荐分数在 getRecommendedItems 中按需即时计算，无需缓存的重算逻辑。
    }
}
