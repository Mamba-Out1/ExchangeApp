package com.example.exchangeapp.ui.screen.home

import com.example.exchangeapp.domain.model.Item

/**
 * HomeScreen的UI状态类
 */
data class HomeUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreItems: Boolean = true,
    val error: String? = null
)