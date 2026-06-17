package com.example.exchangeapp.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.Item
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

/**
 * 个人中心界面的ViewModel
 *
 * 管理用户个人信息、发布物品列表、收藏列表和历史交换记录状态
 *
 * **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7**
 *
 * Requirements:
 * - 7.1: 显示个人中心界面
 * - 7.2: 显示User的头像、昵称和联系方式
 * - 7.3: 显示User发布的所有Item列表
 * - 7.4: 显示User的Favorite_List
 * - 7.5: WHEN User点击已发布Item, THE App SHALL允许User编辑或删除该Item
 * - 7.6: WHEN User点击收藏Item, THE App SHALL跳转到该Item详情页
 * - 7.7: THE App SHALL显示User的历史交换记录数量
 */
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

    // 用户信息状态
    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    // 发布物品列表状态
    private val _publishedItemsState = MutableStateFlow<PublishedItemsState>(PublishedItemsState.Loading)
    val publishedItemsState: StateFlow<PublishedItemsState> = _publishedItemsState.asStateFlow()

    // 收藏物品列表状态
    private val _favoriteItemsState = MutableStateFlow<FavoriteItemsState>(FavoriteItemsState.Loading)
    val favoriteItemsState: StateFlow<FavoriteItemsState> = _favoriteItemsState.asStateFlow()

    // 历史交换记录数量
    private val _orderCountState = MutableStateFlow<OrderCountState>(OrderCountState.Loading)
    val orderCountState: StateFlow<OrderCountState> = _orderCountState.asStateFlow()

    // 操作状态（删除、编辑等）
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private var loadDataJob: Job? = null

    /**
     * 初始化ViewModel，加载个人中心数据
     */
    init {
        loadProfileData()
    }

    /**
     * 加载个人中心数据
     *
     * 实现Requirement 7.1: 显示个人中心界面
     */
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
                // 并行加载所有数据
                val userJob = launch { loadUserData(userId) }
                val publishedItemsJob = launch { loadPublishedItems(userId) }
                val favoriteItemsJob = launch { loadFavoriteItems(userId) }
                val orderCountJob = launch { loadOrderCount(userId) }

                // 等待所有任务完成
                userJob.join()
                publishedItemsJob.join()
                favoriteItemsJob.join()
                orderCountJob.join()

                // 更新整体加载状态
                _actionState.value = ActionState.Idle
            } catch (e: Exception) {
                // 处理异常
                _actionState.value = ActionState.Error("加载失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 加载用户数据
     *
     * 实现Requirement 7.2: 显示User的头像、昵称和联系方式
     */
    private suspend fun loadUserData(userId: String) {
        _userState.value = UserState.Loading
        try {
            val user = userRepository.getUserById(userId)
            if (user != null) {
                _userState.value = UserState.Success(user)
            } else {
                _userState.value = UserState.Error("用户信息不存在")
            }
        } catch (e: Exception) {
            _userState.value = UserState.Error("加载用户信息失败: ${e.message ?: "未知错误"}")
        }
    }

    /**
     * 加载用户发布的物品列表
     *
     * 实现Requirement 7.3: 显示User发布的所有Item列表
     */
    private suspend fun loadPublishedItems(userId: String) {
        _publishedItemsState.value = PublishedItemsState.Loading
        try {
            val items = itemRepository.getItemsByUserId(userId)
            if (items.isEmpty()) {
                _publishedItemsState.value = PublishedItemsState.Empty
            } else {
                _publishedItemsState.value = PublishedItemsState.Success(items)
            }
        } catch (e: Exception) {
            _publishedItemsState.value = PublishedItemsState.Error("加载发布物品失败: ${e.message ?: "未知错误"}")
        }
    }

    /**
     * 加载用户收藏的物品列表
     *
     * 实现Requirement 7.4: 显示User的Favorite_List
     */
    private suspend fun loadFavoriteItems(userId: String) {
        _favoriteItemsState.value = FavoriteItemsState.Loading
        try {
            // 获取用户的交互记录
            val userInteractions = userInteractionRepository.getUserInteractions(userId)
            
            // 筛选出收藏的物品ID
            val favoriteItemIds = userInteractions.interactions
                .filter { it.isFavorite }
                .map { it.itemId }
            
            if (favoriteItemIds.isEmpty()) {
                _favoriteItemsState.value = FavoriteItemsState.Empty
            } else {
                // 根据物品ID获取完整的物品信息，使用GetItemDetailsUseCase
                val favoriteItems = mutableListOf<Item>()
                for (itemId in favoriteItemIds) {
                    val result = getItemDetailsUseCase(itemId)
                    if (result.isSuccess) {
                        favoriteItems.add(result.getOrThrow())
                    }
                    // 如果物品不存在或获取失败，跳过该物品
                }
                
                if (favoriteItems.isEmpty()) {
                    _favoriteItemsState.value = FavoriteItemsState.Empty
                } else {
                    _favoriteItemsState.value = FavoriteItemsState.Success(favoriteItems)
                }
            }
        } catch (e: Exception) {
            _favoriteItemsState.value = FavoriteItemsState.Error("加载收藏物品失败: ${e.message ?: "未知错误"}")
        }
    }

    /**
     * 加载用户历史交换记录数量
     *
     * 实现Requirement 7.7: THE App SHALL显示User的历史交换记录数量
     */
    private suspend fun loadOrderCount(userId: String) {
        _orderCountState.value = OrderCountState.Loading
        try {
            val orders = orderRepository.getOrdersByUserId(userId)
            val completedOrders = orders.count { order ->
                order.status == com.example.exchangeapp.domain.model.OrderStatus.COMPLETED
            }
            _orderCountState.value = OrderCountState.Success(completedOrders)
        } catch (e: Exception) {
            _orderCountState.value = OrderCountState.Error("加载交换记录失败: ${e.message ?: "未知错误"}")
        }
    }

    /**
     * 删除用户发布的物品
     *
     * 实现Requirement 7.5的一部分: 允许User删除该Item
     *
     * @param itemId 要删除的物品ID
     */
    fun deleteItem(itemId: String) {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            _actionState.value = ActionState.Error("请先登录")
            return
        }

        _actionState.value = ActionState.Deleting
        viewModelScope.launch {
            try {
                // 调用删除用例
                val result = deleteItemUseCase(itemId)
                when {
                    result.isSuccess -> {
                        // 重新加载发布物品列表
                        loadPublishedItems(userId)
                        _actionState.value = ActionState.DeleteSuccess
                    }
                    result.isFailure -> {
                        _actionState.value = ActionState.Error(
                            result.exceptionOrNull()?.message ?: "删除失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _actionState.value = ActionState.Error("删除失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 编辑物品（这里只处理导航，实际编辑在ItemDetailScreen中处理）
     * 
     * 实现Requirement 7.5的一部分: 允许User编辑该Item
     *
     * @param itemId 要编辑的物品ID
     */
    fun editItem(itemId: String) {
        _actionState.value = ActionState.NavigateToEdit(itemId)
    }

    /**
     * 查看收藏物品详情
     *
     * 实现Requirement 7.6: WHEN User点击收藏Item, THE App SHALL跳转到该Item详情页
     *
     * @param itemId 要查看的物品ID
     */
    fun viewFavoriteItem(itemId: String) {
        _actionState.value = ActionState.NavigateToItemDetail(itemId)
    }

    /**
     * 重置操作状态
     */
    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }

    /**
     * 刷新所有数据
     */
    fun refresh() {
        loadProfileData()
    }

    /**
     * 检查用户是否已登录
     */
    fun isUserLoggedIn(): Boolean {
        return currentUserProvider.getCurrentUserId() != null
    }
}

/**
 * 用户信息状态密封类
 */
sealed class UserState {
    /**
     * 加载中状态，表示正在加载用户信息
     */
    object Loading : UserState()

    /**
     * 成功状态，包含用户信息
     *
     * @param user 用户信息
     */
    data class Success(val user: User) : UserState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : UserState()
}

/**
 * 发布物品状态密封类
 */
sealed class PublishedItemsState {
    /**
     * 加载中状态，表示正在加载发布物品
     */
    object Loading : PublishedItemsState()

    /**
     * 空状态，表示没有发布物品
     */
    object Empty : PublishedItemsState()

    /**
     * 成功状态，包含发布物品列表
     *
     * @param items 发布物品列表
     */
    data class Success(val items: List<Item>) : PublishedItemsState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : PublishedItemsState()
}

/**
 * 收藏物品状态密封类
 */
sealed class FavoriteItemsState {
    /**
     * 加载中状态，表示正在加载收藏物品
     */
    object Loading : FavoriteItemsState()

    /**
     * 空状态，表示没有收藏物品
     */
    object Empty : FavoriteItemsState()

    /**
     * 成功状态，包含收藏物品列表
     *
     * @param items 收藏物品列表
     */
    data class Success(val items: List<Item>) : FavoriteItemsState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : FavoriteItemsState()
}

/**
 * 订单数量状态密封类
 */
sealed class OrderCountState {
    /**
     * 加载中状态，表示正在加载订单数量
     */
    object Loading : OrderCountState()

    /**
     * 成功状态，包含已完成订单数量
     *
     * @param count 已完成订单数量
     */
    data class Success(val count: Int) : OrderCountState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : OrderCountState()
}

/**
 * 操作状态密封类
 */
sealed class ActionState {
    /**
     * 空闲状态，初始状态
     */
    object Idle : ActionState()

    /**
     * 删除中状态，表示正在删除物品
     */
    object Deleting : ActionState()

    /**
     * 删除成功状态
     */
    object DeleteSuccess : ActionState()

    /**
     * 导航到编辑物品界面
     *
     * @param itemId 要编辑的物品ID
     */
    data class NavigateToEdit(val itemId: String) : ActionState()

    /**
     * 导航到物品详情界面
     *
     * @param itemId 要查看的物品ID
     */
    data class NavigateToItemDetail(val itemId: String) : ActionState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : ActionState()
}