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

        const val MIN_MATCHING_THRESHOLD = 0.05
        const val CACHE_VALIDITY_MS = 60L * 1000L

        private const val PARALLEL_THRESHOLD = 32
    }

    private data class CacheKey(
        val sourceItemId: String,
        val limit: Int
    )

    private data class CacheEntry(
        val items: List<MatchedItem>,
        val timestamp: Long
    )

    private val matchingCache = ConcurrentHashMap<CacheKey, CacheEntry>()

    override suspend fun getMatchedItems(
        sourceItemId: String,
        limit: Int
    ): List<MatchedItem> {
        val cacheKey = CacheKey(sourceItemId, limit)
        matchingCache[cacheKey]?.let { entry ->
            if (isCacheValid(entry)) return entry.items
        }

        val sourceItem = itemRepository.getItemById(sourceItemId) ?: return emptyList()
        val candidateItems = itemRepository.getAllItems()
            .filter { it.id != sourceItemId }
            .filter { it.userId != sourceItem.userId }

        val result = withContext(computationDispatcher) {
            scoreCandidates(sourceItem, candidateItems)
                .filter { it.matchingScore >= MIN_MATCHING_THRESHOLD }
                .sortedByDescending { it.matchingScore }
                .take(limit)
        }

        matchingCache[cacheKey] = CacheEntry(result, timeProvider())
        return result
    }

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

    private fun isCacheValid(entry: CacheEntry): Boolean {
        return timeProvider() - entry.timestamp < CACHE_VALIDITY_MS
    }

    internal fun calculateMatchingScore(source: Item, target: Item): Double {
        val sourceWants = source.wantedSignalTags()
        val targetWants = target.wantedSignalTags()
        val sourceIdentity = source.identitySignalTags()
        val targetIdentity = target.identitySignalTags()

        val sourceWantsTargetScore = calculateTagSimilarity(sourceWants, targetIdentity)
        val targetWantsSourceScore = calculateTagSimilarity(targetWants, sourceIdentity)
        val itemTagScore = calculateTagSimilarity(sourceIdentity, targetIdentity)
        val keywordScore = calculateKeywordSimilarity(source.description, target.description)

        return (
            sourceWantsTargetScore * SOURCE_WANTS_TARGET_WEIGHT +
                targetWantsSourceScore * TARGET_WANTS_SOURCE_WEIGHT +
                itemTagScore * ITEM_TAG_WEIGHT +
                keywordScore * KEYWORD_WEIGHT
            ).coerceIn(0.0, 1.0)
    }

    internal fun calculateTagSimilarity(tags1: Collection<String>, tags2: Collection<String>): Double {
        val left = expandTags(tags1)
        val right = expandTags(tags2)
        if (left.isEmpty() || right.isEmpty()) return 0.0

        val intersection = left.intersect(right).size
        val union = left.union(right).size
        return intersection.toDouble() / union.toDouble()
    }

    internal fun calculateKeywordSimilarity(desc1: String, desc2: String): Double {
        val words1 = expandTags(tokenize(desc1))
        val words2 = expandTags(tokenize(desc2))
        if (words1.isEmpty() || words2.isEmpty()) return 0.0

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        return intersection.toDouble() / union.toDouble()
    }

    internal fun tokenize(text: String): Set<String> {
        val normalized = text.lowercase()
        val asciiTokens = normalized
            .split(Regex("[^a-z0-9]+"))
            .mapNotNull { it.toSearchTokenOrNull() }
        return (asciiTokens + keywordTagsFromText(normalized)).toSet()
    }

    private fun Item.identitySignalTags(): Set<String> {
        return expandTags(tags + tokenize(name) + tokenize(description))
    }

    private fun Item.wantedSignalTags(): Set<String> {
        return expandTags(wantedTags + tokenize(wantedItemName))
    }

    private fun expandTags(tags: Collection<String>): Set<String> {
        val result = linkedSetOf<String>()
        tags.forEach { raw ->
            val tag = raw.trim().lowercase()
            if (tag.isBlank()) return@forEach
            result += tag
            result += tag.phraseTokens()
            result += keywordTagsFromText(tag)
            result += TAG_ALIASES[tag].orEmpty()
            tag.phraseTokens().forEach { token ->
                result += TAG_ALIASES[token].orEmpty()
            }
        }
        return result
    }

    private fun keywordTagsFromText(text: String): Set<String> {
        val lower = text.lowercase()
        val result = linkedSetOf<String>()
        KEYWORD_TAGS.forEach { (keyword, tags) ->
            if (lower.contains(keyword)) {
                result += tags
            }
        }
        return result
    }

    private fun String.phraseTokens(): Set<String> {
        return split(Regex("[^a-z0-9]+"))
            .mapNotNull { it.toSearchTokenOrNull() }
            .toSet()
    }

    private fun String.toSearchTokenOrNull(): String? {
        val token = trim().lowercase()
        if (token.length <= 1) return null
        return when (token) {
            "mice" -> "mouse"
            "calculators" -> "calculator"
            "keyboards" -> "keyboard"
            "earbuds", "earphones" -> "earphone"
            "headphones" -> "headphone"
            "electronics", "electronic" -> "electronics"
            "peripherals" -> "peripheral"
            "computers" -> "computer"
            "books" -> "book"
            "textbooks" -> "textbook"
            "bicycles", "bikes" -> "bike"
            else -> token
        }
    }
}

private val TAG_ALIASES: Map<String, Set<String>> = mapOf(
    "鼠标" to setOf("mouse", "computer", "electronics", "peripheral"),
    "mouse" to setOf("鼠标", "computer", "electronics", "peripheral"),
    "mice" to setOf("mouse", "鼠标", "computer", "electronics", "peripheral"),
    "计算器" to setOf("calculator", "electronics", "study", "math", "stationery"),
    "calculator" to setOf("计算器", "electronics", "study", "math", "stationery"),
    "calculators" to setOf("calculator", "计算器", "electronics", "study", "math", "stationery"),
    "键盘" to setOf("keyboard", "computer", "electronics", "peripheral"),
    "keyboard" to setOf("键盘", "computer", "electronics", "peripheral"),
    "耳机" to setOf("earphones", "headphones", "bluetooth", "audio"),
    "蓝牙耳机" to setOf("earphones", "headphones", "bluetooth", "audio"),
    "earphone" to setOf("earphones", "headphones", "bluetooth", "audio"),
    "earphones" to setOf("earphone", "headphones", "bluetooth", "audio"),
    "headphone" to setOf("headphones", "earphones", "bluetooth", "audio"),
    "headphones" to setOf("headphone", "earphones", "bluetooth", "audio"),
    "书" to setOf("book", "textbook", "study", "education"),
    "教材" to setOf("book", "textbook", "study", "education"),
    "book" to setOf("书", "textbook", "study", "education"),
    "textbook" to setOf("教材", "book", "study", "education"),
    "自行车" to setOf("bicycle", "bike", "transport", "sports"),
    "bike" to setOf("bicycle", "自行车", "transport", "sports"),
    "bicycle" to setOf("bike", "自行车", "transport", "sports")
)

private val KEYWORD_TAGS: Map<String, Set<String>> = mapOf(
    "鼠标" to TAG_ALIASES.getValue("鼠标"),
    "mouse" to TAG_ALIASES.getValue("mouse"),
    "计算器" to TAG_ALIASES.getValue("计算器"),
    "calculator" to TAG_ALIASES.getValue("calculator"),
    "键盘" to TAG_ALIASES.getValue("键盘"),
    "keyboard" to TAG_ALIASES.getValue("keyboard"),
    "耳机" to TAG_ALIASES.getValue("耳机"),
    "headphone" to TAG_ALIASES.getValue("headphones"),
    "earphone" to TAG_ALIASES.getValue("earphones"),
    "教材" to TAG_ALIASES.getValue("教材"),
    "book" to TAG_ALIASES.getValue("book"),
    "自行车" to TAG_ALIASES.getValue("自行车"),
    "bike" to TAG_ALIASES.getValue("bike"),
    "bicycle" to TAG_ALIASES.getValue("bicycle")
)
