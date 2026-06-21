package com.example.exchangeapp.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.recommendation.RecommendationEngine
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.service.LocationService
import com.example.exchangeapp.domain.usecase.GetRecommendedItemsUseCase
import com.example.exchangeapp.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecommendedItemsUseCase: GetRecommendedItemsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val recommendationEngine: RecommendationEngine,
    private val itemRepository: ItemRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val locationService: LocationService
) : ViewModel() {

    companion object {
        private const val ITEMS_PER_PAGE = 20
        private const val RECOMMENDED_LIMIT = 10
        private const val REFRESH_DELAY_MS = 500L
        private const val RECALC_INTERVAL_MS = 10_000L
    }

    private val _itemsState = MutableStateFlow<ItemsState>(ItemsState.Empty)
    val itemsState: StateFlow<ItemsState> = _itemsState.asStateFlow()

    private val _loadMoreState = MutableStateFlow<LoadMoreState>(LoadMoreState.Idle)
    val loadMoreState: StateFlow<LoadMoreState> = _loadMoreState.asStateFlow()

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    private var currentPage = 0
    private var allItems: List<Item> = emptyList()
    private var recommendedItems: List<Item> = emptyList()
    private var userLocation: Location? = null

    private var loadMoreJob: Job? = null
    private var refreshJob: Job? = null
    private var periodicRecalcJob: Job? = null

    init {
        loadInitialData()
    }

    fun startPeriodicRecalculation() {
        periodicRecalcJob?.cancel()
        periodicRecalcJob = viewModelScope.launch {
            while (isActive) {
                delay(RECALC_INTERVAL_MS)
                refreshRecommendationsSilently()
            }
        }
    }

    private fun loadInitialData() {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            _itemsState.value = ItemsState.Error("请先登录")
            return
        }

        _itemsState.value = ItemsState.Loading
        viewModelScope.launch {
            try {
                userLocation = locationService.getCurrentLocation()
                loadHomeData(userId)
                publishFirstPage()
            } catch (e: Exception) {
                _itemsState.value = ItemsState.Error("加载失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    fun refresh() {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null || _refreshState.value is RefreshState.Loading) return

        _refreshState.value = RefreshState.Loading
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                delay(REFRESH_DELAY_MS)
                userLocation = locationService.getCurrentLocation()
                recommendationEngine.recalculateScores()
                loadHomeData(userId)
                publishFirstPage()
                _refreshState.value = RefreshState.Success
                _loadMoreState.value = LoadMoreState.Idle
            } catch (e: Exception) {
                _refreshState.value = RefreshState.Error("刷新失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    fun loadMore() {
        if (_loadMoreState.value is LoadMoreState.Loading ||
            allItems.isEmpty() ||
            _itemsState.value !is ItemsState.Success
        ) {
            return
        }

        val currentItems = (_itemsState.value as? ItemsState.Success)?.items ?: return
        if (currentItems.size >= allItems.size) {
            _loadMoreState.value = LoadMoreState.NoMoreItems
            return
        }

        _loadMoreState.value = LoadMoreState.Loading
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            try {
                delay(500)
                val nextPage = currentPage + 1
                val endIndex = minOf(nextPage * ITEMS_PER_PAGE, allItems.size)
                val nextItems = allItems.take(endIndex)
                currentPage = nextPage
                _itemsState.value = ItemsState.Success(
                    items = nextItems,
                    recommendedItems = recommendedItems
                )
                _loadMoreState.value = if (nextItems.size >= allItems.size) {
                    LoadMoreState.NoMoreItems
                } else {
                    LoadMoreState.Success
                }
            } catch (e: Exception) {
                _loadMoreState.value = LoadMoreState.Error("加载失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            val result = toggleFavoriteUseCase(itemId)
            if (result.isSuccess) {
                refreshRecommendationsSilently()
            }
        }
    }

    fun resetLoadMoreState() {
        _loadMoreState.value = LoadMoreState.Idle
    }

    fun resetRefreshState() {
        _refreshState.value = RefreshState.Idle
    }

    fun isUserLoggedIn(): Boolean = currentUserProvider.getCurrentUserId() != null

    override fun onCleared() {
        super.onCleared()
        periodicRecalcJob?.cancel()
    }

    private suspend fun loadHomeData(userId: String) {
        allItems = itemRepository.getAllItems().sortedByDescending { it.createdAt }
        recommendedItems = getRecommendedItemsUseCase(
            userId = userId,
            userLocation = userLocation,
            limit = RECOMMENDED_LIMIT
        ).getOrElse { emptyList() }
    }

    private fun publishFirstPage() {
        currentPage = 1
        val firstPage = allItems.take(ITEMS_PER_PAGE)
        _itemsState.value = if (allItems.isEmpty()) {
            if (recommendedItems.isEmpty()) ItemsState.Empty
            else ItemsState.Success(items = emptyList(), recommendedItems = recommendedItems)
        } else {
            ItemsState.Success(items = firstPage, recommendedItems = recommendedItems)
        }
    }

    private suspend fun refreshRecommendationsSilently() {
        val userId = currentUserProvider.getCurrentUserId() ?: return
        val current = _itemsState.value as? ItemsState.Success ?: return

        recommendationEngine.recalculateScores()
        recommendedItems = getRecommendedItemsUseCase(
            userId = userId,
            userLocation = userLocation,
            limit = RECOMMENDED_LIMIT
        ).getOrElse { recommendedItems }

        val freshAllItems = itemRepository.getAllItems().sortedByDescending { it.createdAt }
        allItems = freshAllItems
        val visibleCount = current.items.size.coerceAtLeast(ITEMS_PER_PAGE).coerceAtMost(allItems.size)
        val visibleItems = allItems.take(visibleCount)
        currentPage = ((visibleCount + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE).coerceAtLeast(1)
        _itemsState.value = ItemsState.Success(
            items = visibleItems,
            recommendedItems = recommendedItems
        )
    }
}

sealed class ItemsState {
    object Empty : ItemsState()
    object Loading : ItemsState()
    data class Success(
        val items: List<Item>,
        val recommendedItems: List<Item>
    ) : ItemsState()
    data class Error(val message: String) : ItemsState()
}

sealed class LoadMoreState {
    object Idle : LoadMoreState()
    object Loading : LoadMoreState()
    object Success : LoadMoreState()
    data class Error(val message: String) : LoadMoreState()
    object NoMoreItems : LoadMoreState()
}

sealed class RefreshState {
    object Idle : RefreshState()
    object Loading : RefreshState()
    object Success : RefreshState()
    data class Error(val message: String) : RefreshState()
}
