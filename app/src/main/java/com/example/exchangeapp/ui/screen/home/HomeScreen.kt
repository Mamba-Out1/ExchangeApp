package com.example.exchangeapp.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.ui.component.EmptyStateView
import com.example.exchangeapp.ui.component.ErrorView
import com.example.exchangeapp.ui.component.ItemCard
import com.example.exchangeapp.ui.component.LoadingView
import com.example.exchangeapp.ui.component.LocationPermissionRequester
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

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

    LocationPermissionRequester(
        onPermissionResult = { granted ->
            if (granted) onRefresh()
        }
    )

    LaunchedEffect(uiState.error) {
        if (uiState.error != null && (uiState.items.isNotEmpty() || uiState.recommendedItems.isNotEmpty())) {
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
            uiState.isLoading && uiState.items.isEmpty() && uiState.recommendedItems.isEmpty() -> {
                LoadingView(modifier = contentModifier)
            }

            uiState.error != null && uiState.items.isEmpty() && uiState.recommendedItems.isEmpty() -> {
                ErrorView(
                    title = "加载失败",
                    message = uiState.error,
                    onRetryClick = onRefresh,
                    modifier = contentModifier
                )
            }

            uiState.items.isEmpty() && uiState.recommendedItems.isEmpty() -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = contentModifier
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            EmptyStateView(
                                title = "暂无商品",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

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
            item(key = "recommendations") {
                RecommendationStrip(
                    items = uiState.recommendedItems,
                    onItemClick = onItemClick,
                    onToggleFavorite = onToggleFavorite
                )
            }

            item(key = "all_items_header") {
                SectionHeader(
                    title = "全部商品",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            if (uiState.items.isEmpty()) {
                item(key = "all_items_empty") {
                    Text(
                        text = "暂无可浏览商品",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
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
            }

            item(key = "list_footer") {
                ListFooter(
                    isLoadingMore = uiState.isLoadingMore,
                    hasMoreItems = uiState.hasMoreItems
                )
            }
        }
    }
}

@Composable
private fun RecommendationStrip(
    items: List<com.example.exchangeapp.domain.model.Item>,
    onItemClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "猜你喜欢",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        if (items.isEmpty()) {
            Text(
                text = "多浏览、收藏，或发布带有想要标签的易物商品后，推荐会更懂你。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                items(
                    items = items,
                    key = { "recommended_${it.id}" }
                ) { item ->
                    ItemCard(
                        item = item,
                        isFavorite = false,
                        distance = null,
                        onItemClick = { onItemClick(item.id) },
                        onFavoriteClick = { onToggleFavorite(item.id) },
                        modifier = Modifier
                            .width(240.dp)
                            .padding(end = 12.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.fillMaxWidth()
    )
}

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
            isLoadingMore -> CircularProgressIndicator()
            !hasMoreItems -> {
                Text(
                    text = "没有更多商品",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun snapshotFlowReachedEnd(
    lazyListState: LazyListState,
    loadMoreThreshold: Int = 3
) = snapshotFlow {
    val layoutInfo = lazyListState.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 1 - loadMoreThreshold
}
