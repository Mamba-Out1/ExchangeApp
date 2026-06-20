package com.example.exchangeapp.ui.screen.favorites

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.ui.component.EmptyStateView
import com.example.exchangeapp.ui.component.ErrorView
import com.example.exchangeapp.ui.component.ItemCard
import com.example.exchangeapp.ui.component.LoadingView

/**
 * 收藏列表屏幕
 *
 * 显示当前用户收藏的所有物品，支持点击跳转到物品详情页。
 * 这是一个无状态(stateless)的Composable，所有数据与回调由调用方注入，
 * 与ProfileScreen保持一致的样式与结构。
 *
 * **Validates: Requirements 7.4, 7.6, 10.5**
 *
 * Requirements:
 * - 7.4: 显示User的Favorite_List
 * - 7.6: WHEN User点击收藏Item, THE App SHALL跳转到该Item详情页
 * - 10.5: 在个人中心显示完整的Favorite_List
 *
 * @param favoriteItems 收藏物品列表
 * @param isLoading 是否处于加载中状态
 * @param error 错误信息（为空表示无错误）
 * @param onItemClick 点击收藏物品回调，传入物品ID用于跳转到详情页 (Requirement 7.6)
 * @param onRetry 重试/刷新回调
 * @param modifier Modifier修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoriteItems: List<Item>,
    isLoading: Boolean,
    error: String?,
    onItemClick: (String) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的收藏",
                        maxLines = 1
                    )
                }
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when {
            // 加载状态
            isLoading -> {
                LoadingView(modifier = contentModifier)
            }
            // 错误状态，提供重试入口
            error != null -> {
                ErrorView(
                    title = "加载失败",
                    message = error,
                    onRetryClick = onRetry,
                    modifier = contentModifier
                )
            }
            // 空状态
            favoriteItems.isEmpty() -> {
                EmptyStateView(
                    iconResId = android.R.drawable.star_big_on,
                    title = "暂无收藏",
                    description = "你还没有收藏任何物品",
                    modifier = contentModifier
                )
            }
            // 内容状态 (Requirement 7.4, 10.5)
            else -> {
                LazyColumn(
                    modifier = contentModifier
                ) {
                    items(
                        items = favoriteItems,
                        key = { "favorite_${it.id}" }
                    ) { item ->
                        ItemCard(
                            item = item,
                            isFavorite = true,
                            distance = null,
                            // 点击物品跳转到详情页 (Requirement 7.6)
                            onItemClick = { onItemClick(item.id) },
                            onFavoriteClick = { onItemClick(item.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // 底部留白
                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
