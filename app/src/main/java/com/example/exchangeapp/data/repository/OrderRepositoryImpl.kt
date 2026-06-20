package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.local.dao.OrderDao
import com.example.exchangeapp.data.local.entity.OrderEntity
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import com.example.exchangeapp.domain.repository.OrderRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 订单仓库实现类。
 *
 * 基于 Room 的 [OrderDao] 完成交换订单的本地持久化，并负责 [OrderEntity]
 * 与领域模型 [Order] 之间的相互转换。其中：
 * - 订单状态(status)以枚举名称的字符串形式持久化。
 *
 * Requirements: 8.2（展示用户的全部交换订单）、8.4（查看订单详情）、10.1（数据本地持久化）。
 */
@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao
) : OrderRepository {

    override suspend fun getOrdersByUserId(userId: String): List<Order> {
        return orderDao.getOrdersByUserId(userId).map { it.toModel() }
    }

    override suspend fun getOrderById(orderId: String): Order? {
        return orderDao.getOrderById(orderId)?.toModel()
    }

    override suspend fun insertOrder(order: Order) {
        orderDao.insertOrder(order.toEntity())
    }

    override suspend fun updateOrder(order: Order) {
        orderDao.updateOrder(order.toEntity())
    }

    // region 转换逻辑（Entity <-> Model）

    /**
     * 将领域模型 [Order] 转换为持久化实体 [OrderEntity]。
     */
    private fun Order.toEntity(): OrderEntity {
        return OrderEntity(
            id = id,
            item1Id = item1Id,
            item2Id = item2Id,
            user1Id = user1Id,
            user2Id = user2Id,
            status = status.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            completedAt = completedAt,
            rating = rating
        )
    }

    /**
     * 将持久化实体 [OrderEntity] 转换为领域模型 [Order]。
     */
    private fun OrderEntity.toModel(): Order {
        return Order(
            id = id,
            item1Id = item1Id,
            item2Id = item2Id,
            user1Id = user1Id,
            user2Id = user2Id,
            status = decodeStatus(status),
            createdAt = createdAt,
            updatedAt = updatedAt,
            completedAt = completedAt,
            rating = rating
        )
    }

    /**
     * 将字符串解析为 [OrderStatus]；无法识别时回退为 [OrderStatus.PENDING]。
     */
    private fun decodeStatus(value: String): OrderStatus {
        return try {
            OrderStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            OrderStatus.PENDING
        }
    }

    // endregion
}
