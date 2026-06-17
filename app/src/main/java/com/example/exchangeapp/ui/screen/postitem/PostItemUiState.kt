package com.example.exchangeapp.ui.screen.postitem

/**
 * PostItemScreen的UI状态类
 */
data class PostItemUiState(
    val images: List<String> = emptyList(),
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val isSubmitted: Boolean = false
)