package com.example.exchangeapp.ui.screen.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
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

/**
 * 订单详情界面的ViewModel
 *
 * 根据订单ID加载订单详情，解析两件交换物品的名称，并在订单进行中时
 * 展示对方User的真实联系方式(手机号)。订单完成后支持评价。
 *
 * **Validates: Requirements 8.4, 8.6, 8.7**
 *
 * Requirements:
 * - 8.4: WHEN User点击订单, THE App SHALL显示订单详情
 * - 8.6: WHILE订单状态为进行中, THE App SHALL显示对方User的联系方式
 * - 8.7: WHEN订单完成, THE App SHALL允许User对交换进行评价
 */
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val itemRepository: ItemRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private var currentOrderId: String? = null

    /**
     * 加载订单详情
     *
     * 解析两件交换物品名称；当订单为进行中(IN_PROGRESS)时，
     * 解析对方User并暴露其手机号作为联系方式 (Requirement 8.4, 8.6)。
     *
     * @param orderId 订单ID
     */
    fun load(orderId: String) {
        currentOrderId = orderId
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val order = orderRepository.getOrderById(orderId)
                if (order == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "订单不存在")
                    }
                    return@launch
                }

                // 解析两件交换物品的名称
                val item1Name = itemRepository.getItemById(order.item1Id)?.name
                val item2Name = itemRepository.getItemById(order.item2Id)?.name

                // 解析对方User的联系方式：仅在进行中订单展示 (Requirement 8.6)
                val counterpartContact = resolveCounterpartContact(order)

                _uiState.update {
                    it.copy(
                        order = order,
                        item1Name = item1Name,
                        item2Name = item2Name,
                        counterpartContact = counterpartContact,
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

    /**
     * 评价订单
     *
     * 实现Requirement 8.7: 订单完成后允许User对交换进行评价。
     * 将评分写入订单并持久化(状态保持为已完成)，随后重新加载。
     *
     * @param rating 评分(1-5)
     */
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
                // 重新加载以反映最新评分
                load(order.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "评价失败: ${e.message ?: "未知错误"}")
                }
            }
        }
    }

    /**
     * 解析对方User的真实联系方式(手机号)。
     *
     * 确定对方userId(相对于当前登录User取user1Id/user2Id的另一方)，
     * 通过[UserRepository.getUserById]获取User后返回其手机号。
     * 仅在订单为进行中(IN_PROGRESS)时有意义，其他状态返回null。
     *
     * @param order 订单
     * @return 对方User的手机号，无法获取或非进行中时返回null
     */
    private suspend fun resolveCounterpartContact(order: Order): String? {
        if (order.status != OrderStatus.IN_PROGRESS) return null

        val userId = currentUserProvider.getCurrentUserId() ?: return null
        val counterpartUserId = when (userId) {
            order.user1Id -> order.user2Id
            order.user2Id -> order.user1Id
            else -> return null
        }

        return userRepository.getUserById(counterpartUserId)?.phone
    }

    /**
     * 重置错误状态
     */
    fun resetError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 订单详情界面的UI状态
 *
 * @param order 订单领域模型，加载完成后非空
 * @param item1Name 物品1名称(发起方物品)，解析失败时为null
 * @param item2Name 物品2名称(接收方物品)，解析失败时为null
 * @param counterpartContact 对方User联系方式(手机号)，仅进行中订单有值 (Requirement 8.6)
 * @param isLoading 是否正在加载
 * @param error 错误信息，无错误时为null
 */
data class OrderDetailUiState(
    val order: Order? = null,
    val item1Name: String? = null,
    val item2Name: String? = null,
    val counterpartContact: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
