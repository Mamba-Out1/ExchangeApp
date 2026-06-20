package com.example.exchangeapp.ui.component

import android.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.ui.theme.ExchangeAppTheme

/**
 * 组件预览文件，用于展示所有通用Composable组件
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ComponentPreview() {
    ExchangeAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 预览ItemCard
            TextPreviewSection("ItemCard 组件")
            ItemCardPreview()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 预览ItemImageCarousel
            TextPreviewSection("ItemImageCarousel 组件")
            ItemImageCarouselPreview()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 预览LoadingView
            TextPreviewSection("LoadingView 组件")
            LoadingView(
                message = "正在加载...",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 预览EmptyStateView
            TextPreviewSection("EmptyStateView 组件")
            EmptyStateView(
                iconResId = android.R.drawable.ic_menu_view,
                title = "暂无物品",
                description = "还没有物品发布，去发布第一个物品吧！",
                actionText = "浏览物品",
                onActionClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 预览ErrorView
            TextPreviewSection("ErrorView 组件")
            ErrorView(
                title = "网络连接失败",
                message = "无法连接到服务器，请检查您的网络连接后重试。",
                retryText = "重试",
                onRetryClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

/**
 * 预览ItemCard组件
 */
@Preview(showBackground = true)
@Composable
fun ItemCardPreview() {
    ExchangeAppTheme {
        // 创建预览物品
        val previewItem = Item(
            id = "preview-1",
            userId = "user-123",
            name = "iPhone 13 Pro",
            description = "九成新，使用一年，电池健康度95%，无划痕，功能完好。",
            estimatedPrice = 4500.0,
            images = listOf("https://example.com/image.jpg"),
            tags = listOf("电子产品", "手机"),
            location = null,
            status = ItemStatus.AVAILABLE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            ItemCard(
                item = previewItem,
                isFavorite = false,
                distance = "500米",
                onItemClick = {},
                onFavoriteClick = {}
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ItemCard(
                item = previewItem.copy(
                    name = "二手教科书 - 高等数学",
                    description = "几乎全新，仅使用一学期，无笔记无划痕。",
                    estimatedPrice = 30.0,
                    tags = listOf("书籍", "教材")
                ),
                isFavorite = true,
                distance = "1.2公里",
                onItemClick = {},
                onFavoriteClick = {}
            )
        }
    }
}

/**
 * 预览ItemImageCarousel组件
 */
@Preview(showBackground = true)
@Composable
fun ItemImageCarouselPreview() {
    ExchangeAppTheme {
        // 创建预览图片列表
        val previewImages = listOf(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg",
            "https://example.com/image3.jpg"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            ItemImageCarousel(
                images = previewImages,
                autoPlay = true,
                onImageClick = {}
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 空状态预览
            ItemImageCarousel(
                images = emptyList(),
                autoPlay = false,
                onImageClick = {}
            )
        }
    }
}

/**
 * 预览LoadingView组件
 */
@Preview(showBackground = true)
@Composable
fun LoadingViewPreview() {
    ExchangeAppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 全屏加载
            LoadingView(
                message = "正在加载物品列表...",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 内联加载
            InlineLoadingView()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 进度条加载
            ProgressLoadingView(
                progress = 0.65f,
                message = "正在上传图片..."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 加载更多
            LoadMoreLoadingView()
        }
    }
}

/**
 * 预览EmptyStateView组件
 */
@Preview(showBackground = true)
@Composable
fun EmptyStateViewPreview() {
    ExchangeAppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 物品列表为空
            EmptyItemsState(
                onBrowseAction = {}
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 搜索结果为空
            EmptySearchState(
                searchQuery = "笔记本电脑"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 无更多物品
            NoMoreItemsState()
        }
    }
}

/**
 * 预览ErrorView组件
 */
@Preview(showBackground = true)
@Composable
fun ErrorViewPreview() {
    ExchangeAppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 网络错误
            NetworkErrorView(
                onRetryClick = {},
                onCheckNetwork = {}
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 数据加载错误
            DataLoadErrorView(
                errorMessage = "无法加载物品列表，请检查网络连接。",
                onRetryClick = {}
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 内联错误
            InlineErrorView(
                message = "表单验证失败，请检查输入内容。"
            )
        }
    }
}

/**
 * 文本预览区域标题
 */
@Composable
private fun TextPreviewSection(title: String) {
    androidx.compose.material3.Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}