package com.example.exchangeapp.ui.screen.profile

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.User

/**
 * ProfileScreen的UI状态类
 */
data class ProfileUiState(
    val user: User? = null,
    val postedItems: List<Item> = emptyList(),
    val favoriteItems: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val exchangeCount: Int = 0
)