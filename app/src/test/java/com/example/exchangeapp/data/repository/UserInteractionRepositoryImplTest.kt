package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.repository.fake.FakeCurrentUserProvider
import com.example.exchangeapp.data.repository.fake.FakeUserInteractionDao
import com.example.exchangeapp.domain.model.UserInteraction
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [UserInteractionRepositoryImpl] 单元测试。
 *
 * 使用内存版假 DAO（[FakeUserInteractionDao]）与可配置的 [FakeCurrentUserProvider]
 * 验证：
 * - Entity <-> Model 转换的往返正确性；
 * - getUserInteractions 在无记录时回退为空集合（默认值）；
 * - incrementClickCount / setFavorite 在无当前用户时安全忽略；
 * - 首次点击 / 收藏时自动创建记录的逻辑；
 * - 读取失败的错误处理路径。
 *
 * **验证需求: Requirements 2.8**
 */
class UserInteractionRepositoryImplTest {

    private lateinit var dao: FakeUserInteractionDao
    private lateinit var currentUserProvider: FakeCurrentUserProvider
    private lateinit var repository: UserInteractionRepositoryImpl

    private fun sampleInteraction(
        userId: String = "user-1",
        itemId: String = "item-1",
        clickCount: Int = 3,
        isFavorite: Boolean = true,
        lastInteractionTime: Long = 5000L
    ) = UserInteraction(
        userId = userId,
        itemId = itemId,
        clickCount = clickCount,
        isFavorite = isFavorite,
        lastInteractionTime = lastInteractionTime
    )

    @BeforeEach
    fun setup() {
        dao = FakeUserInteractionDao()
        currentUserProvider = FakeCurrentUserProvider("user-1")
        repository = UserInteractionRepositoryImpl(dao, currentUserProvider)
    }

    @Test
    fun `insert then get returns equal model - round trip conversion`() = runTest {
        val interaction = sampleInteraction()

        repository.insertOrUpdateInteraction(interaction)
        val loaded = repository.getInteraction("user-1", "item-1")

        assertEquals(interaction, loaded)
    }

    @Test
    fun `getInteraction returns null when missing`() = runTest {
        assertNull(repository.getInteraction("user-1", "nope"))
    }

    @Test
    fun `getUserInteractions aggregates all interactions for user`() = runTest {
        repository.insertOrUpdateInteraction(sampleInteraction(itemId = "a", clickCount = 2))
        repository.insertOrUpdateInteraction(sampleInteraction(itemId = "b", isFavorite = true))
        repository.insertOrUpdateInteraction(sampleInteraction(userId = "other", itemId = "c"))

        val interactions = repository.getUserInteractions("user-1")

        assertEquals(2, interactions.getClickCount("a"))
        assertTrue(interactions.isFavorite("b"))
    }

    @Test
    fun `getUserInteractions returns empty aggregate when no records - default value`() = runTest {
        val interactions = repository.getUserInteractions("user-1")

        assertEquals(0, interactions.getClickCount("anything"))
        assertEquals(false, interactions.isFavorite("anything"))
    }

    @Test
    fun `incrementClickCount creates first record with count 1`() = runTest {
        repository.incrementClickCount("item-1")

        val loaded = repository.getInteraction("user-1", "item-1")!!
        assertEquals(1, loaded.clickCount)
    }

    @Test
    fun `incrementClickCount increments existing record`() = runTest {
        repository.insertOrUpdateInteraction(sampleInteraction(clickCount = 4, isFavorite = false))

        repository.incrementClickCount("item-1")

        assertEquals(5, repository.getInteraction("user-1", "item-1")!!.clickCount)
    }

    @Test
    fun `incrementClickCount is ignored when there is no current user`() = runTest {
        currentUserProvider.userId = null

        repository.incrementClickCount("item-1")

        assertNull(repository.getInteraction("user-1", "item-1"))
    }

    @Test
    fun `setFavorite creates record when none exists`() = runTest {
        repository.setFavorite("item-1", true)

        val loaded = repository.getInteraction("user-1", "item-1")!!
        assertTrue(loaded.isFavorite)
        assertEquals(0, loaded.clickCount)
    }

    @Test
    fun `setFavorite updates existing record preserving click count`() = runTest {
        repository.insertOrUpdateInteraction(sampleInteraction(clickCount = 7, isFavorite = false))

        repository.setFavorite("item-1", true)

        val loaded = repository.getInteraction("user-1", "item-1")!!
        assertTrue(loaded.isFavorite)
        assertEquals(7, loaded.clickCount)
    }

    @Test
    fun `setFavorite is ignored when there is no current user`() = runTest {
        currentUserProvider.userId = null

        repository.setFavorite("item-1", true)

        assertNull(repository.getInteraction("user-1", "item-1"))
    }

    @Test
    fun `read failure propagates as exception`() = runTest {
        dao.failReads = true

        assertFailsWith<RuntimeException> {
            repository.getUserInteractions("user-1")
        }
    }
}
