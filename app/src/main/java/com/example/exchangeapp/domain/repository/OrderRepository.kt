package com.example.exchangeapp.domain.repository

import com.example.exchangeapp.domain.model.Order

/**
 * 订单仓库接口。
 *
 * 负责交换订单数据的读取与持久化。
 *
 * Requirements: 8.2（交换订单管理）、10.1（数据本地持久化）。
 */
interface OrderRepository {
    /**
     * 获取指定用户参与的全部订单（作为发起方或接收方），按创建时间倒序。
     */
    suspend fun getOrdersByUserId(userId: String): List<Order>

    /**
     * 根据订单ID获取单个订单，不存在时返回 null。
     */
    suspend fun getOrderById(orderId: String): Order?

    /**
     * 新增订单；若已存在相同ID则替换。
     */
    suspend fun insertOrder(order: Order)

    /**
     * 更新已存在的订单信息（如状态变更）。
     */
    suspend fun updateOrder(order: Order)
}
