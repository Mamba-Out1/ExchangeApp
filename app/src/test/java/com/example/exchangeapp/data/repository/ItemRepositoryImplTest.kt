package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.local.entity.ItemEntity
import com.example.exchangeapp.data.repository.fake.FakeItemDao
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.domain.model.Location
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [ItemRepositoryImpl] 单元测试。
 *
 * 使用内存版假 DAO（[FakeItemDao]）验证：
 * - Entity <-> Model 转换的往返正确性（含图片 / 标签 JSON、位置拆分、状态枚举）；
 * - 转换过程中的默认值回退（非法 JSON、未知状态、缺失经纬度）；
 * - 数据读取失败时的错误处理路径。
 *
 * **验证需求: Requirements 2.8**
 */
class ItemRepositoryImplTest {

    private lateinit var dao: FakeItemDao
    private lateinit var repository: ItemRepositoryImpl

    private fun sampleItem(
        id: String = "item-1",
        status: ItemStatus = ItemStatus.AVAILABLE,
        location: Location? = Location(30.5, 114.3, "校区A"),
        images: List<String> = listOf("img1.png", "img2.png"),
        tags: List<String> = listOf("书籍", "教材")
    ) = Item(
        id = id,
        userId = "user-1",
        name = "二手教材",
        description = "九成新",
        estimatedPrice = 35.0,
        images = images,
        tags = tags,
        location = location,
        status = status,
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @BeforeEach
    fun setup() {
        dao = FakeItemDao()
        repository = ItemRepositoryImpl(dao)
    }

    @Test
    fun `insert then get returns equal model - round trip conversion`() = runTest {
        val item = sampleItem()

        repository.insertItem(item)
        val loaded = repository.getItemById(item.id)

        assertEquals(item, loaded)
    }

    @Test
    fun `round trip preserves null location and empty lists`() = runTest {
        val item = sampleItem(location = null, images = emptyList(), tags = emptyList())

        repository.insertItem(item)
        val loaded = repository.getItemById(item.id)

        assertEquals(item, loaded)
    }

    @Test
    fun `getItemById returns null when item is missing`() = runTest {
        assertNull(repository.getItemById("does-not-exist"))
    }

    @Test
    fun `getAllItems returns only available items`() = runTest {
        repository.insertItem(sampleItem(id = "available", status = ItemStatus.AVAILABLE))
        repository.insertItem(sampleItem(id = "exchanged", status = ItemStatus.EXCHANGED))

        val result = repository.getAllItems()

        assertEquals(1, result.size)
        assertEquals("available", result.first().id)
    }

    @Test
    fun `getItemsByUserId returns user owned items`() = runTest {
        repository.insertItem(sampleItem(id = "a").copy(userId = "user-1"))
        repository.insertItem(sampleItem(id = "b").copy(userId = "user-2"))

        val result = repository.getItemsByUserId("user-1")

        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun `deleteItem removes the item`() = runTest {
        val item = sampleItem()
        repository.insertItem(item)

        repository.deleteItem(item.id)

        assertNull(repository.getItemById(item.id))
    }

    // region 默认值回退 (Requirement 2.8)

    @Test
    fun `malformed images and tags JSON falls back to empty lists`() = runTest {
        dao.seed(
            ItemEntity(
                id = "broken",
                userId = "user-1",
                name = "n",
                description = "d",
                estimatedPrice = 1.0,
                images = "not-json",
                tags = "{also-not-json",
                latitude = null,
                longitude = null,
                address = null,
                status = "AVAILABLE",
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val loaded = repository.getItemById("broken")!!

        assertEquals(emptyList(), loaded.images)
        assertEquals(emptyList(), loaded.tags)
    }

    @Test
    fun `unknown status string falls back to AVAILABLE`() = runTest {
        dao.seed(
            ItemEntity(
                id = "weird-status",
                userId = "user-1",
                name = "n",
                description = "d",
                estimatedPrice = 1.0,
                images = "[]",
                tags = "[]",
                latitude = null,
                longitude = null,
                address = null,
                status = "TOTALLY_UNKNOWN",
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val loaded = repository.getItemById("weird-status")!!

        assertEquals(ItemStatus.AVAILABLE, loaded.status)
    }

    @Test
    fun `location is null when latitude or longitude is missing`() = runTest {
        dao.seed(
            ItemEntity(
                id = "no-loc",
                userId = "user-1",
                name = "n",
                description = "d",
                estimatedPrice = 1.0,
                images = "[]",
                tags = "[]",
                latitude = 30.0,
                longitude = null,
                address = "somewhere",
                status = "AVAILABLE",
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val loaded = repository.getItemById("no-loc")!!

        assertNull(loaded.location)
    }

    // endregion

    // region 错误处理路径 (Requirement 2.8)

    @Test
    fun `read failure propagates as exception`() = runTest {
        dao.failReads = true

        assertFailsWith<RuntimeException> {
            repository.getAllItems()
        }
    }

    @Test
    fun `getItemsByTag read failure propagates as exception`() = runTest {
        dao.failReads = true

        assertFailsWith<RuntimeException> {
            repository.getItemsByTag("书籍")
        }
    }

    // endregion
}
