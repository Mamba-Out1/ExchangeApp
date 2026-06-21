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

class RecommendationEngineImpl(
    private val itemRepository: ItemRepository,
    private val userInteractionRepository: UserInteractionRepository,
    private val locationService: LocationService,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) : RecommendationEngine {

    companion object {
        const val WANTED_TAG_WEIGHT = 0.35
        const val FAVORITE_TAG_WEIGHT = 0.22
        const val CLICK_TAG_WEIGHT = 0.18
        const val DISTANCE_WEIGHT = 0.10
        const val FRESHNESS_WEIGHT = 0.10
        const val DIRECT_INTERACTION_WEIGHT = 0.05

        const val CLICK_SCORE_FACTOR = 0.12
        const val FAVORITE_INTEREST_WEIGHT = 3
        const val MAX_DISTANCE_KM = 10.0
        const val DEFAULT_DISTANCE_SCORE = 0.5
        const val METERS_PER_KM = 1000.0
        const val FRESHNESS_WINDOW_MS = 14L * 24L * 60L * 60L * 1000L
        const val CACHE_VALIDITY_MS = 5L * 60L * 1000L

        private const val PARALLEL_THRESHOLD = 32
    }

    private data class CacheKey(
        val userId: String,
        val userLocation: Location?,
        val limit: Int
    )

    private data class CacheEntry(
        val items: List<Item>,
        val timestamp: Long
    )

    private data class UserInterestProfile(
        val wantedTags: Set<String>,
        val favoriteTags: Set<String>,
        val clickedTags: Set<String>
    )

    private val recommendationCache = ConcurrentHashMap<CacheKey, CacheEntry>()

    override suspend fun getRecommendedItems(
        userId: String,
        userLocation: Location?,
        limit: Int
    ): List<Item> {
        val cacheKey = CacheKey(userId, userLocation, limit)
        recommendationCache[cacheKey]?.let { entry ->
            if (isCacheValid(entry)) return entry.items
        }

        val allItems = itemRepository.getAllItems()
        val candidateItems = allItems.filter { it.userId != userId }
        val interactions = userInteractionRepository.getUserInteractions(userId)
        val userItems = itemRepository.getItemsByUserId(userId)
        val interestProfile = buildInterestProfile(userItems, allItems, interactions)

        val result = withContext(computationDispatcher) {
            scoreItems(candidateItems, userLocation, interactions, interestProfile)
                .sortedWith(compareByDescending<ScoredItem> { it.score }.thenByDescending { it.item.createdAt })
                .take(limit)
                .map { it.item }
        }

        recommendationCache[cacheKey] = CacheEntry(result, timeProvider())
        return result
    }

    private suspend fun scoreItems(
        items: List<Item>,
        userLocation: Location?,
        interactions: UserInteractions,
        interestProfile: UserInterestProfile
    ): List<ScoredItem> {
        if (items.size < PARALLEL_THRESHOLD) {
            return items.map { item ->
                ScoredItem(
                    item,
                    calculateRecommendationScore(item, userLocation, interactions, interestProfile)
                )
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
                                calculateRecommendationScore(item, userLocation, interactions, interestProfile)
                            )
                        }
                    }
                }
                .awaitAll()
                .flatten()
        }
    }

    internal fun calculateRecommendationScore(
        item: Item,
        userLocation: Location?,
        interactions: UserInteractions
    ): Double {
        return calculateRecommendationScore(
            item = item,
            userLocation = userLocation,
            interactions = interactions,
            interestProfile = UserInterestProfile(emptySet(), emptySet(), emptySet())
        )
    }

    private fun calculateRecommendationScore(
        item: Item,
        userLocation: Location?,
        interactions: UserInteractions,
        interestProfile: UserInterestProfile
    ): Double {
        val itemTags = item.tags.map { it.lowercase() }.toSet()
        val wantedTagScore = calculateTagSimilarity(interestProfile.wantedTags, itemTags)
        val favoriteTagScore = calculateTagSimilarity(interestProfile.favoriteTags, itemTags)
        val clickedTagScore = calculateTagSimilarity(interestProfile.clickedTags, itemTags)
        val distanceScore = calculateDistanceScore(item.location, userLocation)
        val freshnessScore = calculateFreshnessScore(item.createdAt)
        val directInteractionScore = (
            interactions.getClickCount(item.id) * CLICK_SCORE_FACTOR +
                if (interactions.isFavorite(item.id)) 1.0 else 0.0
            ).coerceIn(0.0, 1.0)

        return (
            wantedTagScore * WANTED_TAG_WEIGHT +
                favoriteTagScore * FAVORITE_TAG_WEIGHT +
                clickedTagScore * CLICK_TAG_WEIGHT +
                distanceScore * DISTANCE_WEIGHT +
                freshnessScore * FRESHNESS_WEIGHT +
                directInteractionScore * DIRECT_INTERACTION_WEIGHT
            ).coerceIn(0.0, 1.0)
    }

    private fun buildInterestProfile(
        userItems: List<Item>,
        allItems: List<Item>,
        interactions: UserInteractions
    ): UserInterestProfile {
        val wantedTags = userItems
            .flatMap { it.wantedTags }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        val itemsById = allItems.associateBy { it.id }
        val favoriteTags = mutableListOf<String>()
        val clickedTags = mutableListOf<String>()

        interactions.interactions.forEach { interaction ->
            val item = itemsById[interaction.itemId] ?: return@forEach
            if (interaction.isFavorite) {
                repeat(FAVORITE_INTEREST_WEIGHT) {
                    favoriteTags += item.tags
                }
            }
            if (interaction.clickCount > 0) {
                clickedTags += item.tags
            }
        }

        return UserInterestProfile(
            wantedTags = wantedTags,
            favoriteTags = favoriteTags.map { it.lowercase() }.toSet(),
            clickedTags = clickedTags.map { it.lowercase() }.toSet()
        )
    }

    private fun calculateTagSimilarity(left: Set<String>, right: Set<String>): Double {
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val intersection = left.intersect(right).size
        val union = left.union(right).size
        return intersection.toDouble() / union.toDouble()
    }

    internal fun calculateDistanceScore(
        itemLocation: Location?,
        userLocation: Location?
    ): Double {
        if (itemLocation == null || userLocation == null) return DEFAULT_DISTANCE_SCORE

        val distanceKm = locationService.calculateDistance(userLocation, itemLocation) / METERS_PER_KM
        return 1.0 - (distanceKm / MAX_DISTANCE_KM).coerceIn(0.0, 1.0)
    }

    private fun calculateFreshnessScore(createdAt: Long): Double {
        val age = (timeProvider() - createdAt).coerceAtLeast(0L)
        return 1.0 - (age.toDouble() / FRESHNESS_WINDOW_MS).coerceIn(0.0, 1.0)
    }

    override fun updateClickWeight(itemId: String) {
        externalScope.launch {
            userInteractionRepository.incrementClickCount(itemId)
            invalidateCache()
        }
    }

    override fun updateFavoriteWeight(itemId: String) {
        externalScope.launch {
            userInteractionRepository.setFavorite(itemId, true)
            invalidateCache()
        }
    }

    override suspend fun recalculateScores() {
        invalidateCache()
    }

    private fun isCacheValid(entry: CacheEntry): Boolean {
        return timeProvider() - entry.timestamp < CACHE_VALIDITY_MS
    }

    private fun invalidateCache() {
        recommendationCache.clear()
    }
}
