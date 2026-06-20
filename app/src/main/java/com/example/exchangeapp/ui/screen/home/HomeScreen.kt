package com.example.exchangeapp.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.ui.component.EmptyStateView
import com.example.exchangeapp.ui.component.ErrorView
import com.example.exchangeapp.ui.component.ItemCard
import com.example.exchangeapp.ui.component.LoadingView
import com.example.exchangeapp.ui.component.LocationPermissionRequester
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
/**
 * 首页屏幕
 *
 * 显示物品浏览主界面，包含推荐物品列表，支持下拉刷新和上滑加载更多
 *
 * **Validates: Requirements 5.1, 5.2, 5.4, 5.5, 5.6, 5.7**
 *
 * Requirements:
 * - 5.1: 显示物品浏览界面
 * - 5.2: 显示Item的图片、名称、价格和简介 (通过ItemCard实现)
 * - 5.4: 支持下拉刷新物品列表 (通过PullToRefreshBox实现)
 * - 5.5: 支持上滑加载更多物品 (通过LazyListState检测列表底部触发loadMore)
 * - 5.6: 每页加载20个Item (由HomeViewModel控制分页)
 * - 5.7: 无更多Item可加载时显示"没有更多物品"提示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 请求位置权限 (Requirement 15.1)；授权后刷新以使用真实位置进行推荐，
    // 拒绝时由LocationService回退到默认校区位置 (Requirement 15.6)。
    LocationPermissionRequester(
        onPermissionResult = { granted ->
            if (granted) {
                onRefresh()
            }
        }
    )

    // 在已有数据的情况下出现错误（如加载更多失败）时，通过Snackbar提示，避免覆盖列表
    LaunchedEffect(uiState.error) {
        if (uiState.error != null && uiState.items.isNotEmpty()) {
            snackbarHostState.showSnackbar(uiState.error)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when {
            // 初始加载状态（尚无任何数据）
            uiState.isLoading && uiState.items.isEmpty() -> {
                LoadingView(modifier = contentModifier)
            }
            // 错误状态且无数据，提供重试入口
            uiState.error != null && uiState.items.isEmpty() -> {
                ErrorView(
                    title = "加载失败",
                    message = uiState.error,
                    onRetryClick = onRefresh,
                    modifier = contentModifier
                )
            }
            // 空状态
            !uiState.isLoading && uiState.items.isEmpty() -> {
                // 即使为空也允许下拉刷新重新拉取
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = contentModifier
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            EmptyStateView(
                                title = "暂无物品",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            // 成功状态 - 显示物品列表
            else -> {
                HomeItemList(
                    uiState = uiState,
                    onRefresh = onRefresh,
                    onLoadMore = onLoadMore,
                    onItemClick = onItemClick,
                    onToggleFavorite = onToggleFavorite,
                    modifier = contentModifier
                )
            }
        }
    }
}

/**
 * 物品列表内容
 *
 * 使用LazyColumn渲染推荐物品，结合PullToRefreshBox实现下拉刷新，
 * 并通过监听LazyListState在接近列表底部时触发加载更多。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeItemList(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    // 上滑加载更多：当最后可见项接近列表末尾时触发loadMore (Requirement 5.5)
    LaunchedEffect(lazyListState, uiState.hasMoreItems, uiState.isLoadingMore) {
        snapshotFlowReachedEnd(lazyListState)
            .distinctUntilChanged()
            .filter { reachedEnd -> reachedEnd }
            .collect {
                if (uiState.hasMoreItems && !uiState.isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = uiState.items,
                key = { it.id }
            ) { item ->
                ItemCard(
                    item = item,
                    isFavorite = false,
                    distance = null,
                    onItemClick = { onItemClick(item.id) },
                    onFavoriteClick = { onToggleFavorite(item.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 列表底部：加载更多指示器或"没有更多物品"提示 (Requirement 5.7)
            item(key = "list_footer") {
                ListFooter(
                    isLoadingMore = uiState.isLoadingMore,
                    hasMoreItems = uiState.hasMoreItems
                )
            }
        }
    }
}

/**
 * 列表底部页脚
 *
 * - 正在加载更多时显示进度指示器
 * - 没有更多数据时显示"没有更多物品"提示 (Requirement 5.7)
 */
@Composable
private fun ListFooter(
    isLoadingMore: Boolean,
    hasMoreItems: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingMore -> {
                CircularProgressIndicator()
            }
            !hasMoreItems -> {
                Text(
                    text = "没有更多物品",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 监听LazyListState，当最后一个可见项接近列表末尾(剩余阈值内)时发出true。
 *
 * 用于实现上滑加载更多 (Requirement 5.5)，阈值保证在用户滑到底部之前就预加载。
 */
private fun snapshotFlowReachedEnd(
    lazyListState: LazyListState,
    loadMoreThreshold: Int = 3
) = snapshotFlow {
    val layoutInfo = lazyListState.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    // 列表中至少要有数据，且最后可见项进入阈值范围
    totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 1 - loadMoreThreshold
}
