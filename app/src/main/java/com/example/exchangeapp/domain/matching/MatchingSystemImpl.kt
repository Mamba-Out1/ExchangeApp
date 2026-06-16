package com.example.exchangeapp.domain.matching

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.MatchedItem
import com.example.exchangeapp.domain.repository.ItemRepository

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
 * @property itemRepository 物品仓库，用于获取源物品与候选物品。
 *
 * **验证需求: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**
 */
class MatchingSystemImpl(
    private val itemRepository: ItemRepository
) : MatchingSystem {

    companion object {
        /** 标签相似度权重。 */
        const val TAG_WEIGHT = 0.6

        /** 关键词相似度权重。 */
        const val KEYWORD_WEIGHT = 0.4

        /** 最低匹配阈值，分数低于或等于该值的物品将被过滤。 */
        const val MIN_MATCHING_THRESHOLD = 0.3
    }

    override suspend fun getMatchedItems(
        sourceItemId: String,
        limit: Int
    ): List<MatchedItem> {
        val sourceItem = itemRepository.getItemById(sourceItemId) ?: return emptyList()
        val candidateItems = itemRepository.getAllItems()
            .filter { it.id != sourceItemId }

        return candidateItems
            .map { item -> MatchedItem(item, calculateMatchingScore(sourceItem, item)) }
            .filter { it.matchingScore > MIN_MATCHING_THRESHOLD }
            .sortedByDescending { it.matchingScore }
            .take(limit)
    }

    /**
     * 计算源物品与目标物品的 Matching_Score。
     *
     * **验证需求: Requirements 4.2, 4.3, 4.4**
     */
    internal fun calculateMatchingScore(source: Item, target: Item): Double {
        val tagScore = calculateTagSimilarity(source.tags, target.tags)
        val keywordScore = calculateKeywordSimilarity(source.description, target.description)

        return (tagScore * TAG_WEIGHT) + (keywordScore * KEYWORD_WEIGHT)
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
