package com.example.exchangeapp.domain.matching

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.domain.repository.ItemRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [MatchingSystemImpl] 缓存与异步优化单元测试。
 *
 * 验证任务 21.3 的性能优化目标：
 * - 缓存机制：相同源物品请求命中缓存，不重复访问仓库；
 * - 缓存失效后重新计算；
 * - 并行评分不改变匹配排序结果（**验证需求: Requirements 4.7**）。
 */
class MatchingSystemImplTest {

    private class CountingItemRepository(private val items: List<Item>) : ItemRepository {
        var getAllItemsCallCount = 0
        override suspend fun getAllItems(): List<Item> {
            getAllItemsCallCount++
            return items
        }
        override suspend fun getItemById(itemId: String): Item? = items.find { it.id == itemId }
        override suspend fun getItemsByUserId(userId: String): List<Item> = emptyList()
        override suspend fun insertItem(item: Item) {}
        override suspend fun updateItem(item: Item) {}
        override suspend fun deleteItem(itemId: String) {}
        override suspend fun getItemsByTag(tag: String): List<Item> = emptyList()
    }

    private fun item(id: String, tags: List<String>, description: String = "") = Item(
        id = id,
        userId = "owner",
        name = id,
        description = description,
        estimatedPrice = 10.0,
        images = emptyList(),
        tags = tags,
        location = null,
        status = ItemStatus.AVAILABLE,
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun system(repo: ItemRepository, now: () -> Long): MatchingSystemImpl {
        return MatchingSystemImpl(
            itemRepository = repo,
            computationDispatcher = StandardTestDispatcher(),
            timeProvider = now
        )
    }

    @Test
    fun `second call within cache window does not recompute`() = runTest {
        val items = listOf(
            item("source", listOf("book", "study", "english")),
            item("match", listOf("book", "study", "english")),
            item("other", listOf("phone"))
        )
        val repo = CountingItemRepository(items)
        var clock = 1_000L
        val sut = system(repo, now = { clock })

        val first = sut.getMatchedItems("source", 5)
        clock += 1_000L
        val second = sut.getMatchedItems("source", 5)

        assertEquals(first, second)
        // First computation calls getAllItems once; cached call should not call it again.
        assertEquals(1, repo.getAllItemsCallCount)
    }

    @Test
    fun `cache expires after validity window and recomputes`() = runTest {
        val items = listOf(
            item("source", listOf("book", "study")),
            item("match", listOf("book", "study"))
        )
        val repo = CountingItemRepository(items)
        var clock = 1_000L
        val sut = system(repo, now = { clock })

        sut.getMatchedItems("source", 5)
        clock += MatchingSystemImpl.CACHE_VALIDITY_MS + 1L
        sut.getMatchedItems("source", 5)

        assertEquals(2, repo.getAllItemsCallCount)
    }

    @Test
    fun `results sorted descending by matching score and above threshold`() = runTest {
        val items = listOf(
            item("source", listOf("a", "b", "c")),
            item("strong", listOf("a", "b", "c")), // identical tags -> high score
            item("weak", listOf("a", "x", "y", "z")), // low overlap
            item("none", listOf("p", "q")) // no overlap -> filtered out
        )
        val repo = CountingItemRepository(items)
        val sut = system(repo, now = { 1_000L })

        val result = sut.getMatchedItems("source", 5)

        assertTrue(result.none { it.item.id == "none" }, "Below-threshold items must be filtered")
        val scores = result.map { it.matchingScore }
        assertEquals(scores.sortedDescending(), scores, "Results must be sorted descending by score")
        assertEquals("strong", result.first().item.id)
    }

    @Test
    fun `parallel scoring over many candidates preserves correctness`() = runTest {
        val source = item("source", listOf("a", "b", "c"))
        // 40 candidates > PARALLEL_THRESHOLD, all identical tags -> all match strongly
        val candidates = (0 until 40).map { item("cand-$it", listOf("a", "b", "c")) }
        val repo = CountingItemRepository(listOf(source) + candidates)
        val sut = system(repo, now = { 1_000L })

        val result = sut.getMatchedItems("source", 100)

        assertEquals(40, result.size)
        val scores = result.map { it.matchingScore }
        assertEquals(scores.sortedDescending(), scores)
    }

    @Test
    fun `missing source item returns empty list`() = runTest {
        val repo = CountingItemRepository(emptyList())
        val sut = system(repo, now = { 1_000L })

        val result = sut.getMatchedItems("does-not-exist", 5)

        assertTrue(result.isEmpty())
    }
}
