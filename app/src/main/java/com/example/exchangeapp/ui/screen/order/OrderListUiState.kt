package com.example.exchangeapp.ui.screen.order

import com.example.exchangeapp.domain.model.Order

/**
 * OrderListScreen的UI状态类
 *
 * 聚合了OrderListViewModel的多个状态(订单列表、刷新、操作)，
 * 以无状态(stateless)的方式驱动订单列表界面。
 *
 * @param orders 当前用户的所有订单(已按更新时间降序排序)
 * @param isLoading 是否处于初始加载状态(尚无任何订单数据)
 * @param isRefreshing 是否处于下拉刷新状态
 * @param error 加载错误信息，无错误时为null
 * @param operationMessage 订单操作(确认/取消)的结果提示信息，用于Snackbar展示
 */
data class OrderListUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val operationMessage: String? = null,
    val currentUserId: String? = null
)
