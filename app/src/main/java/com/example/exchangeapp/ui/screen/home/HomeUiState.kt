package com.example.exchangeapp.ui.screen.home

import com.example.exchangeapp.domain.model.Item

data class HomeUiState(
    val recommendedItems: List<Item> = emptyList(),
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreItems: Boolean = true,
    val error: String? = null
)
