package com.example.exchangeapp.domain.matching

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.MatchedItem
import com.example.exchangeapp.domain.repository.ItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * [MatchingSystem] 的基于规则算法实现。
 *
 * Matching_Score 计算公式：
 * ```
 * score = (tagScore * TAG_WEIGHT) + (keywordScore * KEYWORD_WEIGHT)
 * ```
 * 其中：
 * - tagScore 为源物品与目标物品标签集合的 Jaccard 相似度。
 * - keywordScore 为两者描述分词后 token 集合的 Jaccard 相似度。
 *
 * ## 性能优化
 * - **缓存机制**：匹配结果按 (sourceItemId, limit) 缓存，缓存在 [CACHE_VALIDITY_MS]
 *   内有效，命中时直接返回，避免对相同源物品重复计算相似度。
 * - **异步计算**：相似度计算在 [computationDispatcher]（默认 [Dispatchers.Default]）
 *   线程池上执行，避免阻塞主线程；候选物品评分通过协程并行计算，
 *   以确保在 2 秒内完成匹配计算（**验证需求: Requirements 4.7**）。
 *
 * @property itemRepository 物品仓库，用于获取源物品与候选物品。
 * @property computationDispatcher 用于执行相似度计算的调度器，默认 [Dispatchers.Default]。
 * @property timeProvider 当前时间提供者（毫秒），用于缓存失效判断，便于测试注入。
 *
 * **验证需求: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.7**
 */
class MatchingSystemImpl(
    private val itemRepository: ItemRepository,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) : MatchingSystem {

    companion object {
        const val SOURCE_WANTS_TARGET_WEIGHT = 0.45

        const val TARGET_WANTS_SOURCE_WEIGHT = 0.35

        const val ITEM_TAG_WEIGHT = 0.12

        const val KEYWORD_WEIGHT = 0.08

        /** 最低匹配阈值，分数低于或等于该值的物品将被过滤。 */
        const val MIN_MATCHING_THRESHOLD = 0.2

        /** 缓存有效期（毫秒）：5 分钟。匹配结果对实时性要求较低，可短时缓存。 */
        const val CACHE_VALIDITY_MS = 5L * 60L * 1000L

        /** 并行评分的最小候选物品数量阈值，低于该值时串行计算以避免协程开销。 */
        private const val PARALLEL_THRESHOLD = 32
    }

    /** 缓存键：源物品与数量限制共同决定一份匹配结果。 */
    private data class CacheKey(
        val sourceItemId: String,
        val limit: Int
    )

    /** 缓存条目：保存匹配结果及其生成时间戳。 */
    private data class CacheEntry(
        val items: List<MatchedItem>,
        val timestamp: Long
    )

    /** 匹配结果缓存，线程安全。 */
    private val matchingCache = ConcurrentHashMap<CacheKey, CacheEntry>()

    override suspend fun getMatchedItems(
        sourceItemId: String,
        limit: Int
    ): List<MatchedItem> {
        val cacheKey = CacheKey(sourceItemId, limit)
        matchingCache[cacheKey]?.let { entry ->
            if (isCacheValid(entry)) {
                return entry.items
            }
        }

        val sourceItem = itemRepository.getItemById(sourceItemId) ?: return emptyList()
        val candidateItems = itemRepository.getAllItems()
            .filter { it.id != sourceItemId }

        val result = withContext(computationDispatcher) {
            scoreCandidates(sourceItem, candidateItems)
                .filter { it.matchingScore > MIN_MATCHING_THRESHOLD }
                .sortedByDescending { it.matchingScore }
                .take(limit)
        }

        matchingCache[cacheKey] = CacheEntry(result, timeProvider())
        return result
    }

    /**
     * 对候选物品并行计算匹配分数。
     *
     * 当候选物品数量超过 [PARALLEL_THRESHOLD] 时，将物品分块并通过协程并行计算，
     * 以加速大规模候选集的处理；否则串行计算以避免协程调度开销。
     */
    private suspend fun scoreCandidates(
        source: Item,
        candidates: List<Item>
    ): List<MatchedItem> {
        if (candidates.size < PARALLEL_THRESHOLD) {
            return candidates.map { item -> MatchedItem(item, calculateMatchingScore(source, item)) }
        }

        val workers = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val chunkSize = (candidates.size + workers - 1) / workers
        return coroutineScope {
            candidates.chunked(chunkSize)
                .map { chunk ->
                    async {
                        chunk.map { item -> MatchedItem(item, calculateMatchingScore(source, item)) }
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
     * 计算源物品与目标物品的 Matching_Score。
     *
     * **验证需求: Requirements 4.2, 4.3, 4.4**
     */
    internal fun calculateMatchingScore(source: Item, target: Item): Double {
        val sourceWantsTargetScore = calculateTagSimilarity(source.wantedTags, target.tags)
        val targetWantsSourceScore = calculateTagSimilarity(target.wantedTags, source.tags)
        val itemTagScore = calculateTagSimilarity(source.tags, target.tags)
        val keywordScore = calculateKeywordSimilarity(source.description, target.description)

        return (
            sourceWantsTargetScore * SOURCE_WANTS_TARGET_WEIGHT +
                targetWantsSourceScore * TARGET_WANTS_SOURCE_WEIGHT +
                itemTagScore * ITEM_TAG_WEIGHT +
                keywordScore * KEYWORD_WEIGHT
            ).coerceIn(0.0, 1.0)
    }

    /**
     * 计算两个标签集合的 Jaccard 相似度（交集大小 / 并集大小）。
     *
     * 任一标签列表为空时返回 0.0。
     *
     * **验证需求: Requirements 4.3**
     */
    internal fun calculateTagSimilarity(tags1: List<String>, tags2: List<String>): Double {
        if (tags1.isEmpty() || tags2.isEmpty()) return 0.0

        val intersection = tags1.intersect(tags2.toSet()).size
        val union = tags1.union(tags2).size

        return intersection.toDouble() / union.toDouble()
    }

    /**
     * 计算两段描述文本关键词的 Jaccard 相似度。
     *
     * 先对两段文本分词得到 token 集合，再计算交集大小 / 并集大小。
     * 任一文本分词结果为空时返回 0.0。
     *
     * **验证需求: Requirements 4.4**
     */
    internal fun calculateKeywordSimilarity(desc1: String, desc2: String): Double {
        val words1 = tokenize(desc1)
        val words2 = tokenize(desc2)

        if (words1.isEmpty() || words2.isEmpty()) return 0.0

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        return intersection.toDouble() / union.toDouble()
    }

    /**
     * 将文本分词为 token 集合。
     *
     * 转为小写后按非单词字符切分，并过滤掉长度小于等于 1 的 token。
     */
    internal fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 1 }
            .toSet()
    }
}
