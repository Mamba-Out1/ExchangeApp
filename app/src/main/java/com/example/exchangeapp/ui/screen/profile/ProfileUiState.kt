package com.example.exchangeapp.ui.screen.profile

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.User

data class ProfileUiState(
    val user: User? = null,
    val postedItems: List<Item> = emptyList(),
    val favoriteItems: List<Item> = emptyList(),
    val completedOrders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val exchangeCount: Int = 0
)
