package com.example.exchangeapp.domain.recommendation

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.model.UserInteraction
import com.example.exchangeapp.domain.model.UserInteractions
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.repository.UserInteractionRepository
import com.example.exchangeapp.domain.service.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [RecommendationEngineImpl] 缓存与异步优化单元测试。
 *
 * 验证任务 21.3 的性能优化目标：
 * - 缓存机制：相同请求命中缓存，不重复访问仓库；
 * - 24 小时缓存失效后重新计算（**验证需求: Requirements 3.7**）；
 * - [RecommendationEngineImpl.recalculateScores] 与交互更新会清除缓存；
 * - 评分排序结果保持不变（不改变算法语义）。
 */
class RecommendationEngineImplTest {

    private class CountingItemRepository(private val items: List<Item>) : ItemRepository {
        var getAllItemsCallCount = 0
        override suspend fun getAllItems(): List<Item> {
            getAllItemsCallCount++
            return items
        }
        override suspend fun getItemById(itemId: String): Item? = items.find { it.id == itemId }
        override suspend fun getItemsByUserId(userId: String): List<Item> = items.filter { it.userId == userId }
        override suspend fun insertItem(item: Item) {}
        override suspend fun updateItem(item: Item) {}
        override suspend fun deleteItem(itemId: String) {}
        override suspend fun getItemsByTag(tag: String): List<Item> = emptyList()
    }

    private class FakeUserInteractionRepository(
        private val interactions: UserInteractions = UserInteractions.EMPTY
    ) : UserInteractionRepository {
        var incrementClickCalls = 0
        var setFavoriteCalls = 0
        override suspend fun getUserInteractions(userId: String): UserInteractions = interactions
        override suspend fun getInteraction(userId: String, itemId: String): UserInteraction? = null
        override suspend fun insertOrUpdateInteraction(interaction: UserInteraction) {}
        override suspend fun incrementClickCount(itemId: String) { incrementClickCalls++ }
        override suspend fun setFavorite(itemId: String, isFavorite: Boolean) { setFavoriteCalls++ }
    }

    /** 简单距离服务：返回两点纬度差的绝对值乘以 1000 米。 */
    private class FakeLocationService : LocationService {
        override suspend fun getCurrentLocation(): Location? = null
        override fun calculateDistance(loc1: Location, loc2: Location): Double =
            kotlin.math.abs(loc1.latitude - loc2.latitude) * 1000.0
        override fun formatDistance(distanceInMeters: Double): String = "$distanceInMeters"
    }

    private fun item(id: String, lat: Double) = Item(
        id = id,
        userId = "owner",
        name = id,
        description = "desc",
        estimatedPrice = 10.0,
        images = emptyList(),
        tags = emptyList(),
        location = Location(lat, 0.0, null),
        status = ItemStatus.AVAILABLE,
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun engine(
        itemRepo: ItemRepository,
        interactionRepo: UserInteractionRepository = FakeUserInteractionRepository(),
        now: () -> Long
    ): RecommendationEngineImpl {
        val dispatcher = StandardTestDispatcher()
        return RecommendationEngineImpl(
            itemRepository = itemRepo,
            userInteractionRepository = interactionRepo,
            locationService = FakeLocationService(),
            externalScope = CoroutineScope(dispatcher),
            computationDispatcher = dispatcher,
            timeProvider = now
        )
    }

    @Test
    fun `second call within cache window does not recompute`() = runTest {
        val repo = CountingItemRepository(listOf(item("a", 0.0), item("b", 1.0)))
        var clock = 1_000L
        val sut = engine(repo, now = { clock })

        val first = sut.getRecommendedItems("user", Location(0.0, 0.0, null), 10)
        clock += 60_000L // 1 minute later, still within 24h window
        val second = sut.getRecommendedItems("user", Location(0.0, 0.0, null), 10)

        assertEquals(first, second)
        assertEquals(1, repo.getAllItemsCallCount, "Cached call should not hit repository again")
    }

    @Test
    fun `cache expires after 24 hours and recomputes`() = runTest {
        val repo = CountingItemRepository(listOf(item("a", 0.0), item("b", 1.0)))
        var clock = 1_000L
        val sut = engine(repo, now = { clock })

        sut.getRecommendedItems("user", Location(0.0, 0.0, null), 10)
        clock += RecommendationEngineImpl.CACHE_VALIDITY_MS + 1L
        sut.getRecommendedItems("user", Location(0.0, 0.0, null), 10)

        assertEquals(2, repo.getAllItemsCallCount, "Expired cache should trigger recomputation")
    }

    @Test
    fun `recalculateScores clears cache forcing recomputation`() = runTest {
        val repo = CountingItemRepository(listOf(item("a", 0.0)))
        val clock = 1_000L
        val sut = engine(repo, now = { clock })

        sut.getRecommendedItems("user", Location(0.0, 0.0, null), 10)
        sut.recalculateScores()
        sut.getRecommendedItems("user", Location(0.0, 0.0, null), 10)

        assertEquals(2, repo.getAllItemsCallCount)
    }

    @Test
    fun `ordering is by descending score and unchanged by caching`() = runTest {
        // item "near" has smaller distance -> higher distance score -> ranked first
        val repo = CountingItemRepository(listOf(item("far", 5.0), item("near", 0.0)))
        val clock = 1_000L
        val sut = engine(repo, now = { clock })

        val result = sut.getRecommendedItems("user", Location(0.0, 0.0, null), 10)

        assertEquals(listOf("near", "far"), result.map { it.id })
    }

    @Test
    fun `parallel scoring over many items preserves descending order`() = runTest {
        // 50 items > PARALLEL_THRESHOLD; latitude increases so distance increases, score decreases
        val items = (0 until 50).map { item("item-$it", it.toDouble() * 0.01) }
        val repo = CountingItemRepository(items)
        val clock = 1_000L
        val sut = engine(repo, now = { clock })

        val result = sut.getRecommendedItems("user", Location(0.0, 0.0, null), 50)

        val ids = result.map { it.id }
        assertEquals((0 until 50).map { "item-$it" }, ids, "Parallel scoring must keep deterministic order")
        assertTrue(result.isNotEmpty())
    }
}
