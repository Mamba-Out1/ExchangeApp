package com.example.exchangeapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.exchangeapp.ui.screen.chat.ChatScreen
import com.example.exchangeapp.ui.screen.favorites.FavoritesScreen
import com.example.exchangeapp.ui.screen.home.HomeScreen
import com.example.exchangeapp.ui.screen.home.HomeViewModel
import com.example.exchangeapp.ui.screen.itemdetail.ItemDetailScreen
import com.example.exchangeapp.ui.screen.itemdetail.ItemDetailViewModel
import com.example.exchangeapp.ui.screen.home.ItemsState
import com.example.exchangeapp.ui.screen.home.LoadMoreState
import com.example.exchangeapp.ui.screen.home.RefreshState
import com.example.exchangeapp.ui.screen.login.LoginScreen
import com.example.exchangeapp.ui.screen.login.LoginViewModel
import com.example.exchangeapp.ui.screen.order.OrderListScreen
import com.example.exchangeapp.ui.screen.order.OrderListViewModel
import com.example.exchangeapp.ui.screen.order.OrdersState
import com.example.exchangeapp.ui.screen.order.OperationState
import com.example.exchangeapp.ui.screen.order.RefreshState as OrderRefreshState
import com.example.exchangeapp.ui.screen.postitem.PostItemScreen
import com.example.exchangeapp.ui.screen.profile.ActionState
import com.example.exchangeapp.ui.screen.profile.FavoriteItemsState
import com.example.exchangeapp.ui.screen.profile.OrderCountState
import com.example.exchangeapp.ui.screen.profile.ProfileScreen
import com.example.exchangeapp.ui.screen.profile.ProfileViewModel
import com.example.exchangeapp.ui.screen.profile.PublishedItemsState
import com.example.exchangeapp.ui.screen.profile.UserState

/**
 * 应用的主导航图
 * 
 * 包含登录流和主应用流两个独立的导航图
 * 
 * **Requirements: 所有界面**
 */
@Composable
fun ExchangeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Routes.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 登录屏幕
        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = hiltViewModel()
            val loginState by viewModel.loginState.collectAsStateWithLifecycle()
            
            LoginScreen(
                loginState = loginState,
                onLogin = { phone, password ->
                    viewModel.updatePhoneNumber(phone)
                    viewModel.updatePassword(password)
                    viewModel.login()
                },
                onLoginSuccess = {
                    // 登录成功后导航到主界面
                    navController.navigate(Routes.HOME) {
                        // 清除登录屏幕，防止返回
                        popUpTo(Routes.LOGIN) {
                            inclusive = true
                        }
                    }
                },
                onResetState = { viewModel.resetState() }
            )
        }
        
        // 主应用导航图（包含底部导航栏标签）
        navigation(
            startDestination = Routes.HOME,
            route = "main"
        ) {
            // 首页
            composable(Routes.HOME) {
                val viewModel: HomeViewModel = hiltViewModel()
                val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
                val loadMoreState by viewModel.loadMoreState.collectAsStateWithLifecycle()
                val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
                
                // 将ViewModel的状态转换为UI状态
                val currentItemsState = itemsState
                val uiState = when (currentItemsState) {
                    is ItemsState.Empty -> com.example.exchangeapp.ui.screen.home.HomeUiState()
                    is ItemsState.Loading -> com.example.exchangeapp.ui.screen.home.HomeUiState(isLoading = true)
                    is ItemsState.Success -> com.example.exchangeapp.ui.screen.home.HomeUiState(
                        items = currentItemsState.items,
                        isLoading = false,
                        isRefreshing = refreshState is RefreshState.Loading,
                        isLoadingMore = loadMoreState is LoadMoreState.Loading,
                        hasMoreItems = loadMoreState !is LoadMoreState.NoMoreItems,
                        error = null
                    )
                    is ItemsState.Error -> com.example.exchangeapp.ui.screen.home.HomeUiState(
                        error = currentItemsState.message
                    )
                    else -> com.example.exchangeapp.ui.screen.home.HomeUiState()
                }
                
                HomeScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onItemClick = { itemId ->
                        // 导航到物品详情页
                        navController.navigate(Routes.itemDetail(itemId))
                    },
                    onToggleFavorite = { itemId ->
                        viewModel.toggleFavorite(itemId)
                    },
                    onNavigateToChat = { userId ->
                        navController.navigate(Routes.chatWithUser(userId))
                    }
                )
            }
            
            // 发布物品
            composable(Routes.POST_ITEM) {
                // PostItemScreen通过hiltViewModel()连接PostItemViewModel，
                // 此处仅提供导航回调。图片选择(系统相册/权限)由独立任务实现。
                PostItemScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onPostSuccess = { itemId ->
                        // 发布成功后跳转到物品详情页 (Requirement 6.7)
                        navController.navigate(Routes.itemDetail(itemId)) {
                            popUpTo(Routes.POST_ITEM) {
                                inclusive = true
                            }
                        }
                    },
                    onAddImageClick = {
                        // 占位符：图片选择由权限/相册任务(17.x)实现
                    }
                )
            }
            
            // 个人中心
            composable(Routes.PROFILE) {
                val viewModel: ProfileViewModel = hiltViewModel()
                val userState by viewModel.userState.collectAsStateWithLifecycle()
                val publishedItemsState by viewModel.publishedItemsState.collectAsStateWithLifecycle()
                val favoriteItemsState by viewModel.favoriteItemsState.collectAsStateWithLifecycle()
                val orderCountState by viewModel.orderCountState.collectAsStateWithLifecycle()
                val actionState by viewModel.actionState.collectAsStateWithLifecycle()

                // 将ViewModel的多个状态聚合为ProfileUiState
                val uiState = com.example.exchangeapp.ui.screen.profile.ProfileUiState(
                    user = (userState as? UserState.Success)?.user,
                    postedItems = (publishedItemsState as? PublishedItemsState.Success)?.items
                        ?: emptyList(),
                    favoriteItems = (favoriteItemsState as? FavoriteItemsState.Success)?.items
                        ?: emptyList(),
                    isLoading = userState is UserState.Loading,
                    error = (userState as? UserState.Error)?.message,
                    exchangeCount = (orderCountState as? OrderCountState.Success)?.count ?: 0
                )

                // 处理操作状态产生的导航副作用 (Requirement 7.5, 7.6)
                LaunchedEffect(actionState) {
                    when (val state = actionState) {
                        is ActionState.NavigateToItemDetail -> {
                            navController.navigate(Routes.itemDetail(state.itemId))
                            viewModel.resetActionState()
                        }
                        is ActionState.NavigateToEdit -> {
                            // 编辑通过物品详情页处理
                            navController.navigate(Routes.itemDetail(state.itemId))
                            viewModel.resetActionState()
                        }
                        else -> Unit
                    }
                }

                ProfileScreen(
                    uiState = uiState,
                    onItemClick = { itemId ->
                        navController.navigate(Routes.itemDetail(itemId))
                    },
                    onFavoriteClick = { itemId ->
                        // 通过ViewModel记录并跳转到收藏物品详情 (Requirement 7.6)
                        viewModel.viewFavoriteItem(itemId)
                    },
                    onEditItem = { itemId ->
                        viewModel.editItem(itemId)
                    },
                    onDeleteItem = { itemId ->
                        viewModel.deleteItem(itemId)
                    },
                    onLogout = {
                        // 登出逻辑，导航回登录页
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    },
                    onRetry = { viewModel.refresh() }
                )
            }
            
            // 订单管理
            composable(Routes.ORDERS) {
                val viewModel: OrderListViewModel = hiltViewModel()
                val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
                val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
                val operationState by viewModel.operationState.collectAsStateWithLifecycle()

                // 将ViewModel的多个状态聚合为OrderListUiState
                val currentOrdersState = ordersState
                val operationMessage = when (val op = operationState) {
                    is OperationState.Success -> op.message
                    is OperationState.Error -> op.message
                    else -> null
                }
                val uiState = when (currentOrdersState) {
                    is OrdersState.Loading -> com.example.exchangeapp.ui.screen.order.OrderListUiState(
                        isLoading = true
                    )
                    is OrdersState.Empty -> com.example.exchangeapp.ui.screen.order.OrderListUiState(
                        isRefreshing = refreshState is OrderRefreshState.Loading,
                        operationMessage = operationMessage
                    )
                    is OrdersState.Success -> com.example.exchangeapp.ui.screen.order.OrderListUiState(
                        orders = currentOrdersState.orders,
                        isRefreshing = refreshState is OrderRefreshState.Loading,
                        operationMessage = operationMessage
                    )
                    is OrdersState.Error -> com.example.exchangeapp.ui.screen.order.OrderListUiState(
                        error = currentOrdersState.message
                    )
                }

                // 操作结果(确认/取消)反馈展示后重置，避免重复触发Snackbar
                LaunchedEffect(operationState) {
                    if (operationState is OperationState.Success || operationState is OperationState.Error) {
                        viewModel.resetOperationState()
                    }
                }

                OrderListScreen(
                    uiState = uiState,
                    onOrderClick = { orderId ->
                        // TODO: 导航到订单详情页(订单详情界面由后续任务实现)
                    },
                    onConfirmOrder = { orderId ->
                        viewModel.confirmOrder(orderId)
                    },
                    onCancelOrder = { orderId ->
                        viewModel.cancelOrder(orderId)
                    },
                    onRateOrder = { orderId, rating ->
                        // TODO: 订单评价由后续任务实现
                    },
                    onRefresh = {
                        viewModel.refresh()
                    }
                )
            }
            
            // 聊天
            composable(
                route = Routes.CHAT_WITH_USER,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""

                // ChatScreen通过hiltViewModel()连接ChatViewModel，
                // 内部根据otherUserId初始化会话并观察消息流。
                ChatScreen(
                    otherUserId = userId,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // 收藏列表 (Requirement 7.4, 7.6, 10.5)
            composable(Routes.FAVORITES) {
                // 复用ProfileViewModel加载收藏数据
                val viewModel: ProfileViewModel = hiltViewModel()
                val favoriteItemsState by viewModel.favoriteItemsState.collectAsStateWithLifecycle()
                val actionState by viewModel.actionState.collectAsStateWithLifecycle()

                // 将FavoriteItemsState映射为FavoritesScreen所需的参数
                val currentFavoriteItemsState = favoriteItemsState
                val favoriteItems = (currentFavoriteItemsState as? FavoriteItemsState.Success)?.items
                    ?: emptyList()
                val isLoading = currentFavoriteItemsState is FavoriteItemsState.Loading
                val error = (currentFavoriteItemsState as? FavoriteItemsState.Error)?.message

                // 处理操作状态产生的导航副作用 (Requirement 7.6)
                LaunchedEffect(actionState) {
                    when (val state = actionState) {
                        is ActionState.NavigateToItemDetail -> {
                            navController.navigate(Routes.itemDetail(state.itemId))
                            viewModel.resetActionState()
                        }
                        else -> Unit
                    }
                }

                FavoritesScreen(
                    favoriteItems = favoriteItems,
                    isLoading = isLoading,
                    error = error,
                    onItemClick = { itemId ->
                        // 通过ViewModel记录并跳转到收藏物品详情 (Requirement 7.6)
                        viewModel.viewFavoriteItem(itemId)
                    },
                    onRetry = { viewModel.refresh() }
                )
            }
            
            // 物品详情
            composable(
                route = Routes.ITEM_DETAIL_WITH_ID,
                arguments = listOf(
                    navArgument("itemId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                val viewModel: ItemDetailViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // 进入屏幕时加载物品详情
                LaunchedEffect(itemId) {
                    if (itemId.isNotEmpty()) {
                        viewModel.loadItemDetails(itemId)
                    }
                }

                ItemDetailScreen(
                    uiState = uiState,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onToggleFavorite = {
                        viewModel.toggleFavorite()
                    },
                    onContactSeller = {
                        // 导航到与卖家的聊天界面
                        uiState.item?.userId?.let { sellerId ->
                            navController.navigate(Routes.chatWithUser(sellerId))
                        }
                    },
                    onMatchedItemClick = { matchedItemId ->
                        // 导航到匹配物品的详情页
                        navController.navigate(Routes.itemDetail(matchedItemId))
                    }
                )
            }
        }
    }
}