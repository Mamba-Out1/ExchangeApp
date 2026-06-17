package com.example.exchangeapp.domain.recommendation

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.model.ScoredItem
import com.example.exchangeapp.domain.model.UserInteractions
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.repository.UserInteractionRepository
import com.example.exchangeapp.domain.service.LocationService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

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
 * ## 性能优化
 * - **缓存机制**：推荐结果按 (userId, userLocation, limit) 缓存，缓存在
 *   [CACHE_VALIDITY_MS]（24 小时）内有效。命中缓存时直接返回，避免重复计算。
 *   每隔 24 小时缓存自动失效并重新计算（**验证需求: Requirements 3.7**）。
 *   当用户点击 / 收藏行为改变交互权重时，会清除该用户的缓存以保证推荐及时更新。
 * - **异步计算**：所有评分计算在 [computationDispatcher]（默认 [Dispatchers.Default]）
 *   线程池上执行，避免阻塞主线程；候选物品评分通过协程并行计算。
 *
 * @property itemRepository 物品仓库，用于获取候选物品。
 * @property userInteractionRepository 交互记录仓库，用于读取与更新点击 / 收藏权重。
 * @property locationService 位置服务，用于计算用户与物品之间的距离。
 * @property externalScope 用于异步执行点击 / 收藏权重持久化的协程作用域。
 * @property computationDispatcher 用于执行评分计算的调度器，默认 [Dispatchers.Default]。
 * @property timeProvider 当前时间提供者（毫秒），用于缓存失效判断，便于测试注入。
 *
 * **验证需求: Requirements 3.1, 3.2, 3.3, 3.5, 3.6, 3.7**
 */
class RecommendationEngineImpl(
    private val itemRepository: ItemRepository,
    private val userInteractionRepository: UserInteractionRepository,
    private val locationService: LocationService,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
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

        /** 缓存有效期（毫秒）：24 小时。 */
        const val CACHE_VALIDITY_MS = 24L * 60L * 60L * 1000L

        /** 并行评分的最小物品数量阈值，低于该值时串行计算以避免协程开销。 */
        private const val PARALLEL_THRESHOLD = 32
    }

    /** 缓存键：用户、位置与数量限制共同决定一份推荐结果。 */
    private data class CacheKey(
        val userId: String,
        val userLocation: Location?,
        val limit: Int
    )

    /** 缓存条目：保存计算结果及其生成时间戳。 */
    private data class CacheEntry(
        val items: List<Item>,
        val timestamp: Long
    )

    /** 推荐结果缓存，线程安全。 */
    private val recommendationCache = ConcurrentHashMap<CacheKey, CacheEntry>()

    override suspend fun getRecommendedItems(
        userId: String,
        userLocation: Location?,
        limit: Int
    ): List<Item> {
        val cacheKey = CacheKey(userId, userLocation, limit)
        recommendationCache[cacheKey]?.let { entry ->
            if (isCacheValid(entry)) {
                return entry.items
            }
        }

        val allItems = itemRepository.getAllItems()
        val interactions = userInteractionRepository.getUserInteractions(userId)

        val result = withContext(computationDispatcher) {
            scoreItems(allItems, userLocation, interactions)
                .sortedByDescending { it.score }
                .take(limit)
                .map { it.item }
        }

        recommendationCache[cacheKey] = CacheEntry(result, timeProvider())
        return result
    }

    /**
     * 对候选物品并行评分。
     *
     * 当物品数量超过 [PARALLEL_THRESHOLD] 时，将物品分块并通过协程并行计算评分，
     * 以加速大规模候选集的处理；否则串行计算以避免协程调度开销。
     */
    private suspend fun scoreItems(
        items: List<Item>,
        userLocation: Location?,
        interactions: UserInteractions
    ): List<ScoredItem> {
        if (items.size < PARALLEL_THRESHOLD) {
            return items.map { item ->
                ScoredItem(item, calculateRecommendationScore(item, userLocation, interactions))
            }
        }

        val workers = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val chunkSize = (items.size + workers - 1) / workers
        return coroutineScope {
            items.chunked(chunkSize)
                .map { chunk ->
                    async {
                        chunk.map { item ->
                            ScoredItem(
                                item,
                                calculateRecommendationScore(item, userLocation, interactions)
                            )
                        }
                    }
                }
                .awaitAll()
                .flatten()
        }
    }

    /**
     * 判断缓存条目是否仍然有效（生成至今未超过 [CACHE_VALIDITY_MS]）。
     */
    private fun isCacheValid(entry: CacheEntry): Boolean {
        return timeProvider() - entry.timestamp < CACHE_VALIDITY_MS
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
            // 交互权重变化后清除缓存，确保下次推荐反映最新行为。
            invalidateCache()
        }
    }

    override fun updateFavoriteWeight(itemId: String) {
        externalScope.launch {
            userInteractionRepository.setFavorite(itemId, true)
            // 交互权重变化后清除缓存，确保下次推荐反映最新行为。
            invalidateCache()
        }
    }

    override suspend fun recalculateScores() {
        // 清空缓存，使下一次 getRecommendedItems 重新计算所有物品的推荐分数。
        // 配合 24 小时定时调用即可满足"每隔 24 小时重新计算"的需求。
        // **验证需求: Requirements 3.7**
        invalidateCache()
    }

    /** 清空推荐结果缓存。 */
    private fun invalidateCache() {
        recommendationCache.clear()
    }
}
