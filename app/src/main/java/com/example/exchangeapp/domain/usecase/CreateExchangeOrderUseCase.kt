package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import com.example.exchangeapp.domain.repository.OrderRepository
import java.util.UUID
import javax.inject.Inject

/**
 * 发起交换（创建交换订单）的 Use Case。
 *
 * 封装单一业务逻辑：当用户在物品详情页用自己的某件物品向他人的物品发起交换时，
 * 构建一条状态为 [OrderStatus.PENDING] 的新 [Order] 并持久化到 [OrderRepository]，
 * 等待物品所有者确认。本 Use Case 以 [Result] 形式向上层（如 ViewModel）传播
 * 成功结果或失败信息。
 *
 * 设计说明：
 * - item1Id / user1Id 记录物品所有者一侧；item2Id / user2Id 记录发起方一侧。
 *   按既有约定，user1Id 为发起方（buyer），user2Id 为物品所有者（seller）。
 * - 新订单ID由 [UUID] 生成，创建与更新时间取当前时刻，completedAt 与 rating 为空。
 * - 当持久化失败时（如数据写入异常），失败信息通过 [Result.failure] 向上传播，
 *   调用方可据此提示用户重试。
 *
 * @property orderRepository 订单仓库，负责订单的读取与持久化。
 *
 * **验证需求: Requirements 8.2**
 */
class CreateExchangeOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {

    /**
     * 发起一次交换，创建待确认订单。
     *
     * @param myItemId 发起方（当前用户）用于交换的物品ID。
     * @param theirItemId 对方（物品所有者）被请求交换的物品ID。
     * @param myUserId 发起方用户ID（buyer）。
     * @param theirUserId 物品所有者用户ID（seller）。
     * @return 成功时返回 [Result.success] 包装的新建 [Order]；持久化失败时返回 [Result.failure]。
     */
    suspend operator fun invoke(
        myItemId: String,
        theirItemId: String,
        myUserId: String,
        theirUserId: String
    ): Result<Order> = runCatching {
        val now = System.currentTimeMillis()
        val order = Order(
            id = UUID.randomUUID().toString(),
            item1Id = theirItemId,
            item2Id = myItemId,
            user1Id = myUserId,
            user2Id = theirUserId,
            status = OrderStatus.PENDING,
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            rating = null
        )
        orderRepository.insertOrder(order)
        order
    }
}
