package com.example.exchangeapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.exchangeapp.ui.screen.chat.ChatViewModel
import com.example.exchangeapp.ui.screen.home.HomeScreen
import com.example.exchangeapp.ui.screen.home.HomeViewModel
import com.example.exchangeapp.ui.screen.home.ItemsState
import com.example.exchangeapp.ui.screen.home.LoadMoreState
import com.example.exchangeapp.ui.screen.home.RefreshState
import com.example.exchangeapp.ui.screen.login.LoginScreen
import com.example.exchangeapp.ui.screen.login.LoginViewModel
import com.example.exchangeapp.ui.screen.order.OrderListScreen
import com.example.exchangeapp.ui.screen.order.OrderListViewModel
import com.example.exchangeapp.ui.screen.postitem.PostItemScreen
import com.example.exchangeapp.ui.screen.postitem.PostItemViewModel
import com.example.exchangeapp.ui.screen.profile.ProfileScreen
import com.example.exchangeapp.ui.screen.profile.ProfileViewModel

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
                val uiState = when (itemsState) {
                    is ItemsState.Empty -> com.example.exchangeapp.ui.screen.home.HomeUiState()
                    is ItemsState.Loading -> com.example.exchangeapp.ui.screen.home.HomeUiState(isLoading = true)
                    else -> {
                        if (itemsState is ItemsState.Success) {
                            com.example.exchangeapp.ui.screen.home.HomeUiState(
                                items = itemsState.items,
                                isLoading = false,
                                isRefreshing = refreshState is RefreshState.Loading,
                                isLoadingMore = loadMoreState is LoadMoreState.Loading,
                                hasMoreItems = loadMoreState !is LoadMoreState.NoMoreItems,
                                error = null
                            )
                        } else if (itemsState is ItemsState.Error) {
                            com.example.exchangeapp.ui.screen.home.HomeUiState(error = itemsState.message)
                        } else {
                            com.example.exchangeapp.ui.screen.home.HomeUiState()
                        }
                    }
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
                val viewModel: PostItemViewModel = hiltViewModel()
                // 简化：直接显示屏幕，不连接ViewModel方法
                PostItemScreen(
                    uiState = com.example.exchangeapp.ui.screen.postitem.PostItemUiState(),
                    onImageSelected = { uri ->
                        // 占位符实现
                    },
                    onImageRemoved = { index ->
                        // 占位符实现
                    },
                    onFieldChanged = { field, value ->
                        // 占位符实现
                    },
                    onAnalyzeImage = { imageUri ->
                        // 占位符实现
                    },
                    onSubmit = {
                        // 占位符实现
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // 个人中心
            composable(Routes.PROFILE) {
                val viewModel: ProfileViewModel = hiltViewModel()
                // 简化：直接显示屏幕，不连接ViewModel方法
                ProfileScreen(
                    uiState = com.example.exchangeapp.ui.screen.profile.ProfileUiState(),
                    onItemClick = { itemId ->
                        navController.navigate(Routes.itemDetail(itemId))
                    },
                    onFavoriteClick = { itemId ->
                        navController.navigate(Routes.itemDetail(itemId))
                    },
                    onEditItem = { itemId ->
                        // TODO: 导航到编辑页面
                    },
                    onDeleteItem = { itemId ->
                        // 占位符实现
                    },
                    onLogout = {
                        // 登出逻辑，导航回登录页
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            
            // 订单管理
            composable(Routes.ORDERS) {
                val viewModel: OrderListViewModel = hiltViewModel()
                // 简化：直接显示屏幕，不连接ViewModel方法
                OrderListScreen(
                    uiState = com.example.exchangeapp.ui.screen.order.OrderListUiState(),
                    onOrderClick = { orderId ->
                        // TODO: 导航到订单详情页
                    },
                    onConfirmOrder = { orderId ->
                        // 占位符实现
                    },
                    onCancelOrder = { orderId ->
                        // 占位符实现
                    },
                    onRateOrder = { orderId, rating ->
                        // 占位符实现
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
                val viewModel: ChatViewModel = hiltViewModel()
                // 简化：直接显示屏幕，不连接ViewModel方法
                
                ChatScreen(
                    uiState = com.example.exchangeapp.ui.screen.chat.ChatUiState(otherUserId = userId),
                    onMessageSent = { message ->
                        // 占位符实现
                    },
                    onImageSent = { imageUri ->
                        // 占位符实现
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // 收藏列表（TODO: 待实现）
            composable(Routes.FAVORITES) {
                // TODO: 实现收藏列表屏幕
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "收藏列表",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "收藏列表功能开发中")
                    }
                }
            }
            
            // 物品详情（TODO: 待实现）
            composable(
                route = Routes.ITEM_DETAIL_WITH_ID,
                arguments = listOf(
                    navArgument("itemId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                // TODO: 实现物品详情屏幕
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "物品详情",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "物品详情($itemId)功能开发中")
                    }
                }
            }
        }
    }
}