package com.example.exchangeapp.ui.screen.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import com.example.exchangeapp.domain.recommendation.RecommendationEngine
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.repository.OrderRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val itemRepository: ItemRepository,
    private val recommendationEngine: RecommendationEngine,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _ordersState = MutableStateFlow<OrdersState>(OrdersState.Loading)
    val ordersState: StateFlow<OrdersState> = _ordersState.asStateFlow()

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private var allOrders: List<Order> = emptyList()
    private var refreshJob: Job? = null
    private var operationJob: Job? = null

    init {
        loadOrders()
    }

    private fun loadOrders() {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            _ordersState.value = OrdersState.Error("请先登录")
            return
        }

        _ordersState.value = OrdersState.Loading
        viewModelScope.launch {
            try {
                publishOrders(orderRepository.getOrdersByUserId(userId))
            } catch (e: Exception) {
                _ordersState.value = OrdersState.Error("加载失败: ${e.message ?: "未知错误"}")
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
                delay(300)
                publishOrders(orderRepository.getOrdersByUserId(userId))
                _refreshState.value = RefreshState.Success
            } catch (e: Exception) {
                _refreshState.value = RefreshState.Error("刷新失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    fun confirmOrder(orderId: String) {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null || _operationState.value is OperationState.Loading) return

        val order = allOrders.find { it.id == orderId }
        if (order == null || order.status != OrderStatus.PENDING) {
            _operationState.value = OperationState.Error("交换请求无法确认")
            return
        }
        if (order.user1Id == userId) {
            _operationState.value = OperationState.Error("已发送交换请求，请等待对方确认")
            return
        }
        if (order.user2Id != userId) {
            _operationState.value = OperationState.Error("无权操作此交换请求")
            return
        }

        _operationState.value = OperationState.Loading("正在确认交换...")
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                orderRepository.updateOrder(
                    order.copy(
                        status = OrderStatus.COMPLETED,
                        completedAt = now,
                        updatedAt = now
                    )
                )
                markItemsExchanged(order.item1Id, order.item2Id)
                recommendationEngine.recalculateScores()
                delay(300)
                loadOrders()
                _operationState.value = OperationState.Success("交换已完成，相关商品已下架")
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("确认失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    fun cancelOrder(orderId: String) {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null || _operationState.value is OperationState.Loading) return

        val order = allOrders.find { it.id == orderId }
        if (order == null || order.status != OrderStatus.PENDING) {
            _operationState.value = OperationState.Error("交换请求无法取消")
            return
        }
        if (order.user1Id != userId && order.user2Id != userId) {
            _operationState.value = OperationState.Error("无权操作此交换请求")
            return
        }

        _operationState.value = OperationState.Loading("正在取消交换请求...")
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                orderRepository.updateOrder(
                    order.copy(
                        status = OrderStatus.CANCELLED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                delay(300)
                loadOrders()
                _operationState.value = OperationState.Success("交换请求已取消")
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("取消失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    fun completeOrder(orderId: String) {
        val order = allOrders.find { it.id == orderId }
        if (order == null || order.status != OrderStatus.IN_PROGRESS) {
            _operationState.value = OperationState.Error("订单无法完成")
            return
        }

        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                orderRepository.updateOrder(
                    order.copy(status = OrderStatus.COMPLETED, completedAt = now, updatedAt = now)
                )
                markItemsExchanged(order.item1Id, order.item2Id)
                recommendationEngine.recalculateScores()
                loadOrders()
                _operationState.value = OperationState.Success("交换已完成，相关商品已下架")
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("完成失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    fun getOrderById(orderId: String): Order? = allOrders.find { it.id == orderId }

    fun getCounterpartContactInfo(orderId: String): String? {
        val userId = currentUserProvider.getCurrentUserId() ?: return null
        val order = allOrders.find { it.id == orderId } ?: return null
        return when (userId) {
            order.user1Id -> order.user2Id
            order.user2Id -> order.user1Id
            else -> null
        }
    }

    fun resetRefreshState() {
        _refreshState.value = RefreshState.Idle
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }

    fun isUserLoggedIn(): Boolean = currentUserProvider.getCurrentUserId() != null

    fun currentUserId(): String? = currentUserProvider.getCurrentUserId()

    private fun publishOrders(orders: List<Order>) {
        allOrders = orders.sortedByDescending { it.updatedAt }
        if (allOrders.isEmpty()) {
            _ordersState.value = OrdersState.Empty
            return
        }
        _ordersState.value = OrdersState.Success(
            orders = allOrders,
            pendingOrders = allOrders.filter { it.status == OrderStatus.PENDING },
            inProgressOrders = allOrders.filter { it.status == OrderStatus.IN_PROGRESS },
            completedOrders = allOrders.filter { it.status == OrderStatus.COMPLETED },
            cancelledOrders = allOrders.filter { it.status == OrderStatus.CANCELLED }
        )
    }

    private suspend fun markItemsExchanged(vararg itemIds: String) {
        val now = System.currentTimeMillis()
        itemIds.distinct().forEach { itemId ->
            val item = itemRepository.getItemById(itemId) ?: return@forEach
            if (item.status != ItemStatus.EXCHANGED) {
                itemRepository.updateItem(item.copy(status = ItemStatus.EXCHANGED, updatedAt = now))
            }
        }
    }
}

sealed class OrdersState {
    object Empty : OrdersState()
    object Loading : OrdersState()
    data class Success(
        val orders: List<Order>,
        val pendingOrders: List<Order>,
        val inProgressOrders: List<Order>,
        val completedOrders: List<Order>,
        val cancelledOrders: List<Order>
    ) : OrdersState()
    data class Error(val message: String) : OrdersState()
}

sealed class RefreshState {
    object Idle : RefreshState()
    object Loading : RefreshState()
    object Success : RefreshState()
    data class Error(val message: String) : RefreshState()
}

sealed class OperationState {
    object Idle : OperationState()
    data class Loading(val message: String) : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}
