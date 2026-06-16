package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.local.entity.OrderEntity
import com.example.exchangeapp.data.repository.fake.FakeOrderDao
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * [OrderRepositoryImpl] 单元测试。
 *
 * 使用内存版假 DAO（[FakeOrderDao]）验证 Entity <-> Model 转换的往返正确性（含订单
 * 状态枚举与可空的 completedAt）、未知状态的默认值回退，以及读取失败的错误处理路径。
 *
 * **验证需求: Requirements 2.8**
 */
class OrderRepositoryImplTest {

    private lateinit var dao: FakeOrderDao
    private lateinit var repository: OrderRepositoryImpl

    private fun sampleOrder(
        id: String = "order-1",
        user1Id: String = "user-1",
        user2Id: String = "user-2",
        status: OrderStatus = OrderStatus.PENDING,
        completedAt: Long? = null
    ) = Order(
        id = id,
        item1Id = "item-1",
        item2Id = "item-2",
        user1Id = user1Id,
        user2Id = user2Id,
        status = status,
        createdAt = 1000L,
        updatedAt = 2000L,
        completedAt = completedAt
    )

    @BeforeEach
    fun setup() {
        dao = FakeOrderDao()
        repository = OrderRepositoryImpl(dao)
    }

    @Test
    fun `insert then get returns equal model - round trip conversion`() = runTest {
        val order = sampleOrder(status = OrderStatus.COMPLETED, completedAt = 3000L)

        repository.insertOrder(order)
        val loaded = repository.getOrderById(order.id)

        assertEquals(order, loaded)
    }

    @Test
    fun `round trip preserves null completedAt`() = runTest {
        val order = sampleOrder(completedAt = null)

        repository.insertOrder(order)
        val loaded = repository.getOrderById(order.id)

        assertEquals(order, loaded)
    }

    @Test
    fun `getOrdersByUserId returns orders where user participates`() = runTest {
        repository.insertOrder(sampleOrder(id = "as-user1", user1Id = "me", user2Id = "x"))
        repository.insertOrder(sampleOrder(id = "as-user2", user1Id = "y", user2Id = "me"))
        repository.insertOrder(sampleOrder(id = "unrelated", user1Id = "a", user2Id = "b"))

        val result = repository.getOrdersByUserId("me").map { it.id }

        assertEquals(setOf("as-user1", "as-user2"), result.toSet())
    }

    @Test
    fun `getOrderById returns null when order is missing`() = runTest {
        assertNull(repository.getOrderById("missing"))
    }

    @Test
    fun `updateOrder overwrites existing order`() = runTest {
        val order = sampleOrder()
        repository.insertOrder(order)

        val updated = order.copy(status = OrderStatus.CANCELLED)
        repository.updateOrder(updated)

        assertEquals(OrderStatus.CANCELLED, repository.getOrderById(order.id)?.status)
    }

    @Test
    fun `unknown status string falls back to PENDING`() = runTest {
        dao.seed(
            OrderEntity(
                id = "weird",
                item1Id = "i1",
                item2Id = "i2",
                user1Id = "u1",
                user2Id = "u2",
                status = "NOT_A_REAL_STATUS",
                createdAt = 1L,
                updatedAt = 1L,
                completedAt = null
            )
        )

        val loaded = repository.getOrderById("weird")!!

        assertEquals(OrderStatus.PENDING, loaded.status)
    }

    @Test
    fun `read failure propagates as exception`() = runTest {
        dao.failReads = true

        assertFailsWith<RuntimeException> {
            repository.getOrdersByUserId("user-1")
        }
    }
}
