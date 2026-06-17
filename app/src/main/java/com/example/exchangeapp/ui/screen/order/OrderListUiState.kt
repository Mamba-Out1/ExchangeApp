package com.example.exchangeapp.ui.screen.order

import com.example.exchangeapp.domain.model.Order

/**
 * OrderListScreen的UI状态类
 */
data class OrderListUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)