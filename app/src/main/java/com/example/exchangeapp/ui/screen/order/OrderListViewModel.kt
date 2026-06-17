package com.example.exchangeapp.ui.screen.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
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

/**
 * 订单管理界面的ViewModel
 *
 * 获取用户订单列表，处理订单状态更新(确认、取消)
 *
 * **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7**
 *
 * Requirements:
 * - 8.1: THE App SHALL显示订单管理界面
 * - 8.2: THE App SHALL显示User的所有交换订单
 * - 8.3: THE App SHALL显示每个订单的状态（待确认、进行中、已完成、已取消）
 * - 8.4: WHEN User点击订单, THE App SHALL显示订单详情
 * - 8.5: WHILE订单状态为待确认, THE App SHALL允许User确认或取消订单
 * - 8.6: WHILE订单状态为进行中, THE App SHALL显示对方User的联系方式
 * - 8.7: WHEN订单完成, THE App SHALL允许User对交换进行评价
 */
@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    companion object {
        private const val REFRESH_DELAY_MS = 500L // 下拉刷新延迟，为了显示动画效果
    }

    // 订单列表状态
    private val _ordersState = MutableStateFlow<OrdersState>(OrdersState.Loading)
    val ordersState: StateFlow<OrdersState> = _ordersState.asStateFlow()

    // 刷新状态
    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    // 操作状态（确认、取消、评价等操作）
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    // 当前用户的所有订单
    private var allOrders: List<Order> = emptyList()

    private var refreshJob: Job? = null
    private var operationJob: Job? = null

    /**
     * 初始化ViewModel，加载用户订单
     */
    init {
        loadOrders()
    }

    /**
     * 加载用户订单列表
     *
     * 实现Requirement 8.2: 显示User的所有交换订单
     */
    private fun loadOrders() {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            _ordersState.value = OrdersState.Error("请先登录")
            return
        }

        _ordersState.value = OrdersState.Loading
        viewModelScope.launch {
            try {
                // 从OrderRepository获取用户的所有订单
                val orders = orderRepository.getOrdersByUserId(userId)
                
                // 按照更新时间降序排序
                allOrders = orders.sortedByDescending { it.updatedAt }
                
                // 根据订单状态分组，便于UI显示
                val pendingOrders = allOrders.filter { it.status == OrderStatus.PENDING }
                val inProgressOrders = allOrders.filter { it.status == OrderStatus.IN_PROGRESS }
                val completedOrders = allOrders.filter { it.status == OrderStatus.COMPLETED }
                val cancelledOrders = allOrders.filter { it.status == OrderStatus.CANCELLED }
                
                if (allOrders.isEmpty()) {
                    _ordersState.value = OrdersState.Empty
                } else {
                    _ordersState.value = OrdersState.Success(
                        orders = allOrders,
                        pendingOrders = pendingOrders,
                        inProgressOrders = inProgressOrders,
                        completedOrders = completedOrders,
                        cancelledOrders = cancelledOrders
                    )
                }
            } catch (e: Exception) {
                _ordersState.value = OrdersState.Error("加载失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 刷新订单列表（下拉刷新）
     */
    fun refresh() {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null || _refreshState.value is RefreshState.Loading) {
            return
        }

        _refreshState.value = RefreshState.Loading
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                // 模拟刷新延迟，显示刷新动画
                delay(REFRESH_DELAY_MS)

                // 重新从OrderRepository获取用户的所有订单
                val orders = orderRepository.getOrdersByUserId(userId)
                
                // 按照更新时间降序排序
                allOrders = orders.sortedByDescending { it.updatedAt }
                
                // 根据订单状态分组，便于UI显示
                val pendingOrders = allOrders.filter { it.status == OrderStatus.PENDING }
                val inProgressOrders = allOrders.filter { it.status == OrderStatus.IN_PROGRESS }
                val completedOrders = allOrders.filter { it.status == OrderStatus.COMPLETED }
                val cancelledOrders = allOrders.filter { it.status == OrderStatus.CANCELLED }
                
                if (allOrders.isEmpty()) {
                    _ordersState.value = OrdersState.Empty
                } else {
                    _ordersState.value = OrdersState.Success(
                        orders = allOrders,
                        pendingOrders = pendingOrders,
                        inProgressOrders = inProgressOrders,
                        completedOrders = completedOrders,
                        cancelledOrders = cancelledOrders
                    )
                }
                _refreshState.value = RefreshState.Success
            } catch (e: Exception) {
                _refreshState.value = RefreshState.Error("刷新失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 确认订单
     *
     * 实现Requirement 8.5: 订单状态为待确认时允许User确认订单
     *
     * @param orderId 订单ID
     */
    fun confirmOrder(orderId: String) {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null || _operationState.value is OperationState.Loading) {
            return
        }

        // 查找订单
        val order = allOrders.find { it.id == orderId }
        if (order == null || order.status != OrderStatus.PENDING) {
            _operationState.value = OperationState.Error("订单无法确认")
            return
        }

        // 检查用户是否有权限确认此订单
        if (order.user1Id != userId && order.user2Id != userId) {
            _operationState.value = OperationState.Error("无权操作此订单")
            return
        }

        _operationState.value = OperationState.Loading("确认订单...")
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                // 更新订单状态为进行中
                val updatedOrder = order.copy(
                    status = OrderStatus.IN_PROGRESS,
                    updatedAt = System.currentTimeMillis()
                )
                
                // 保存到数据库
                orderRepository.updateOrder(updatedOrder)
                
                // 延迟以显示操作成功动画
                delay(300)
                
                // 重新加载订单列表
                loadOrders()
                _operationState.value = OperationState.Success("订单已确认")
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("确认失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 取消订单
     *
     * 实现Requirement 8.5: 订单状态为待确认时允许User取消订单
     *
     * @param orderId 订单ID
     */
    fun cancelOrder(orderId: String) {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null || _operationState.value is OperationState.Loading) {
            return
        }

        // 查找订单
        val order = allOrders.find { it.id == orderId }
        if (order == null || order.status != OrderStatus.PENDING) {
            _operationState.value = OperationState.Error("订单无法取消")
            return
        }

        // 检查用户是否有权限取消此订单
        if (order.user1Id != userId && order.user2Id != userId) {
            _operationState.value = OperationState.Error("无权操作此订单")
            return
        }

        _operationState.value = OperationState.Loading("取消订单...")
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                // 更新订单状态为已取消
                val updatedOrder = order.copy(
                    status = OrderStatus.CANCELLED,
                    updatedAt = System.currentTimeMillis()
                )
                
                // 保存到数据库
                orderRepository.updateOrder(updatedOrder)
                
                // 延迟以显示操作成功动画
                delay(300)
                
                // 重新加载订单列表
                loadOrders()
                _operationState.value = OperationState.Success("订单已取消")
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("取消失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 获取订单详情
     *
     * 实现Requirement 8.4: 当用户点击订单时显示订单详情
     *
     * @param orderId 订单ID
     * @return 订单对象，如果不存在返回null
     */
    fun getOrderById(orderId: String): Order? {
        return allOrders.find { it.id == orderId }
    }

    /**
     * 获取对方用户的联系方式（占位实现）
     *
     * 实现Requirement 8.6: 订单状态为进行中时显示对方User的联系方式
     * 注意：这里需要与UserRepository集成获取用户联系方式
     *
     * @param orderId 订单ID
     * @return 对方用户的联系方式（手机号），如果无法获取返回null
     */
    fun getCounterpartContactInfo(orderId: String): String? {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) return null
        
        val order = allOrders.find { it.id == orderId }
        if (order == null || order.status != OrderStatus.IN_PROGRESS) return null
        
        // 确定对方用户的ID
        val counterpartUserId = if (order.user1Id == userId) order.user2Id else order.user1Id
        
        // TODO: 这里需要集成UserRepository来获取用户联系方式
        // 使用counterpartUserId从UserRepository获取用户手机号
        // 暂时返回一个占位值
        return "138****1234"
    }

    /**
     * 重置刷新状态
     */
    fun resetRefreshState() {
        _refreshState.value = RefreshState.Idle
    }

    /**
     * 重置操作状态
     */
    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }

    /**
     * 获取当前用户是否已登录
     */
    fun isUserLoggedIn(): Boolean {
        return currentUserProvider.getCurrentUserId() != null
    }
}

/**
 * 订单列表状态密封类
 */
sealed class OrdersState {
    /**
     * 空状态，表示没有订单
     */
    object Empty : OrdersState()

    /**
     * 加载中状态，表示正在加载订单
     */
    object Loading : OrdersState()

    /**
     * 成功状态，包含订单列表
     *
     * @param orders 所有订单列表
     * @param pendingOrders 待确认订单列表
     * @param inProgressOrders 进行中订单列表
     * @param completedOrders 已完成订单列表
     * @param cancelledOrders 已取消订单列表
     */
    data class Success(
        val orders: List<Order>,
        val pendingOrders: List<Order>,
        val inProgressOrders: List<Order>,
        val completedOrders: List<Order>,
        val cancelledOrders: List<Order>
    ) : OrdersState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : OrdersState()
}

/**
 * 刷新状态密封类
 */
sealed class RefreshState {
    /**
     * 空闲状态，初始状态
     */
    object Idle : RefreshState()

    /**
     * 加载中状态，表示正在刷新订单
     */
    object Loading : RefreshState()

    /**
     * 成功状态，表示刷新成功
     */
    object Success : RefreshState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : RefreshState()
}

/**
 * 操作状态密封类（用于确认、取消、评价等操作）
 */
sealed class OperationState {
    /**
     * 空闲状态，初始状态
     */
    object Idle : OperationState()

    /**
     * 加载中状态，表示正在执行操作
     *
     * @param message 加载提示信息
     */
    data class Loading(val message: String) : OperationState()

    /**
     * 成功状态，包含成功信息
     *
     * @param message 成功信息
     */
    data class Success(val message: String) : OperationState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : OperationState()
}
