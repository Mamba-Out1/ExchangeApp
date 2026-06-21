package com.example.exchangeapp.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import com.example.exchangeapp.domain.model.User
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.repository.OrderRepository
import com.example.exchangeapp.domain.repository.UserInteractionRepository
import com.example.exchangeapp.domain.repository.UserRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.usecase.DeleteItemUseCase
import com.example.exchangeapp.domain.usecase.GetItemDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val currentUserProvider: CurrentUserProvider,
    private val userRepository: UserRepository,
    private val itemRepository: ItemRepository,
    private val userInteractionRepository: UserInteractionRepository,
    private val orderRepository: OrderRepository,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val getItemDetailsUseCase: GetItemDetailsUseCase
) : ViewModel() {

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    private val _publishedItemsState = MutableStateFlow<PublishedItemsState>(PublishedItemsState.Loading)
    val publishedItemsState: StateFlow<PublishedItemsState> = _publishedItemsState.asStateFlow()

    private val _favoriteItemsState = MutableStateFlow<FavoriteItemsState>(FavoriteItemsState.Loading)
    val favoriteItemsState: StateFlow<FavoriteItemsState> = _favoriteItemsState.asStateFlow()

    private val _orderCountState = MutableStateFlow<OrderCountState>(OrderCountState.Loading)
    val orderCountState: StateFlow<OrderCountState> = _orderCountState.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private var loadDataJob: Job? = null

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            _userState.value = UserState.Error("请先登录")
            _publishedItemsState.value = PublishedItemsState.Error("请先登录")
            _favoriteItemsState.value = FavoriteItemsState.Error("请先登录")
            _orderCountState.value = OrderCountState.Error("请先登录")
            return
        }

        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            try {
                val userJob = launch { loadUserData(userId) }
                val publishedItemsJob = launch { loadPublishedItems(userId) }
                val favoriteItemsJob = launch { loadFavoriteItems(userId) }
                val orderHistoryJob = launch { loadOrderHistory(userId) }

                userJob.join()
                publishedItemsJob.join()
                favoriteItemsJob.join()
                orderHistoryJob.join()

                _actionState.value = ActionState.Idle
            } catch (e: Exception) {
                _actionState.value = ActionState.Error("加载失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    private suspend fun loadUserData(userId: String) {
        _userState.value = UserState.Loading
        try {
            val user = userRepository.getUserById(userId)
            _userState.value = if (user != null) {
                UserState.Success(user)
            } else {
                UserState.Error("用户信息不存在")
            }
        } catch (e: Exception) {
            _userState.value = UserState.Error("加载用户信息失败: ${e.message ?: "未知错误"}")
        }
    }

    private suspend fun loadPublishedItems(userId: String) {
        _publishedItemsState.value = PublishedItemsState.Loading
        try {
            val items = itemRepository.getItemsByUserId(userId).sortedByDescending { it.updatedAt }
            _publishedItemsState.value = if (items.isEmpty()) {
                PublishedItemsState.Empty
            } else {
                PublishedItemsState.Success(items)
            }
        } catch (e: Exception) {
            _publishedItemsState.value = PublishedItemsState.Error("加载发布物品失败: ${e.message ?: "未知错误"}")
        }
    }

    private suspend fun loadFavoriteItems(userId: String) {
        _favoriteItemsState.value = FavoriteItemsState.Loading
        try {
            val favoriteItemIds = userInteractionRepository.getUserInteractions(userId)
                .interactions
                .filter { it.isFavorite }
                .map { it.itemId }

            if (favoriteItemIds.isEmpty()) {
                _favoriteItemsState.value = FavoriteItemsState.Empty
                return
            }

            val favoriteItems = favoriteItemIds.mapNotNull { itemId ->
                getItemDetailsUseCase(itemId).getOrNull()
            }
            _favoriteItemsState.value = if (favoriteItems.isEmpty()) {
                FavoriteItemsState.Empty
            } else {
                FavoriteItemsState.Success(favoriteItems)
            }
        } catch (e: Exception) {
            _favoriteItemsState.value = FavoriteItemsState.Error("加载收藏物品失败: ${e.message ?: "未知错误"}")
        }
    }

    private suspend fun loadOrderHistory(userId: String) {
        _orderCountState.value = OrderCountState.Loading
        try {
            val completedOrders = orderRepository.getOrdersByUserId(userId)
                .filter { it.status == OrderStatus.COMPLETED }
                .sortedByDescending { it.completedAt ?: it.updatedAt }
            _orderCountState.value = OrderCountState.Success(completedOrders.size, completedOrders)
        } catch (e: Exception) {
            _orderCountState.value = OrderCountState.Error("加载交换记录失败: ${e.message ?: "未知错误"}")
        }
    }

    fun deleteItem(itemId: String) {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            _actionState.value = ActionState.Error("请先登录")
            return
        }

        _actionState.value = ActionState.Deleting
        viewModelScope.launch {
            try {
                val result = deleteItemUseCase(itemId)
                if (result.isSuccess) {
                    loadPublishedItems(userId)
                    _actionState.value = ActionState.DeleteSuccess
                } else {
                    _actionState.value = ActionState.Error(
                        result.exceptionOrNull()?.message ?: "删除失败"
                    )
                }
            } catch (e: Exception) {
                _actionState.value = ActionState.Error("删除失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    fun editItem(itemId: String) {
        _actionState.value = ActionState.NavigateToEdit(itemId)
    }

    fun viewFavoriteItem(itemId: String) {
        _actionState.value = ActionState.NavigateToItemDetail(itemId)
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }

    fun refresh() {
        loadProfileData()
    }

    fun isUserLoggedIn(): Boolean = currentUserProvider.getCurrentUserId() != null
}

sealed class UserState {
    object Loading : UserState()
    data class Success(val user: User) : UserState()
    data class Error(val message: String) : UserState()
}

sealed class PublishedItemsState {
    object Loading : PublishedItemsState()
    object Empty : PublishedItemsState()
    data class Success(val items: List<Item>) : PublishedItemsState()
    data class Error(val message: String) : PublishedItemsState()
}

sealed class FavoriteItemsState {
    object Loading : FavoriteItemsState()
    object Empty : FavoriteItemsState()
    data class Success(val items: List<Item>) : FavoriteItemsState()
    data class Error(val message: String) : FavoriteItemsState()
}

sealed class OrderCountState {
    object Loading : OrderCountState()
    data class Success(
        val count: Int,
        val completedOrders: List<Order> = emptyList()
    ) : OrderCountState()
    data class Error(val message: String) : OrderCountState()
}

sealed class ActionState {
    object Idle : ActionState()
    object Deleting : ActionState()
    object DeleteSuccess : ActionState()
    data class NavigateToEdit(val itemId: String) : ActionState()
    data class NavigateToItemDetail(val itemId: String) : ActionState()
    data class Error(val message: String) : ActionState()
}
