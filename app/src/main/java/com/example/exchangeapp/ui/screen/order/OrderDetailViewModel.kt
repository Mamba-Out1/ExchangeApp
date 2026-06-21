package com.example.exchangeapp.ui.screen.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import com.example.exchangeapp.domain.model.User
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.repository.OrderRepository
import com.example.exchangeapp.domain.repository.UserRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val itemRepository: ItemRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    fun load(orderId: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val order = orderRepository.getOrderById(orderId)
                if (order == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "交换记录不存在")
                    }
                    return@launch
                }

                val item1 = itemRepository.getItemById(order.item1Id)
                val item2 = itemRepository.getItemById(order.item2Id)
                val counterpart = resolveCounterpartUser(order)

                _uiState.update {
                    it.copy(
                        order = order,
                        item1 = item1,
                        item2 = item2,
                        counterpartUser = counterpart,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败: ${e.message ?: "未知错误"}"
                    )
                }
            }
        }
    }

    fun rateOrder(rating: Int) {
        val order = _uiState.value.order ?: return

        viewModelScope.launch {
            try {
                val updatedOrder = order.copy(
                    rating = rating,
                    status = OrderStatus.COMPLETED,
                    updatedAt = System.currentTimeMillis()
                )
                orderRepository.updateOrder(updatedOrder)
                load(order.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "评价失败: ${e.message ?: "未知错误"}")
                }
            }
        }
    }

    fun resetError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun resolveCounterpartUser(order: Order): User? {
        val currentUserId = currentUserProvider.getCurrentUserId() ?: return null
        val counterpartUserId = when (currentUserId) {
            order.user1Id -> order.user2Id
            order.user2Id -> order.user1Id
            else -> return null
        }
        return userRepository.getUserById(counterpartUserId)
    }
}

data class OrderDetailUiState(
    val order: Order? = null,
    val item1: Item? = null,
    val item2: Item? = null,
    val counterpartUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
