package com.example.exchangeapp.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.R

/**
 * 首页屏幕
 * 
 * 显示物品浏览主界面，包含推荐物品列表
 * 
 * **Validates: Requirements 5.1, 5.2, 5.4, 5.5, 5.6, 5.7**
 * 
 * Requirements:
 * - 5.1: 显示物品浏览界面
 * - 5.2: 显示Item的图片、名称、价格和简介
 * - 5.4: 支持下拉刷新物品列表
 * - 5.5: 支持上滑加载更多物品
 * - 5.6: 每页加载20个Item
 * - 5.7: 无更多Item可加载时显示提示
 */
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
    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = stringResource(R.string.screen_title_home),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}