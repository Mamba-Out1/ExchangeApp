package com.example.exchangeapp.ui.screen.itemdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.ui.component.EmptyStateView
import com.example.exchangeapp.ui.component.ErrorView
import com.example.exchangeapp.ui.component.ItemCard
import com.example.exchangeapp.ui.component.ItemImageCarousel
import com.example.exchangeapp.ui.component.LoadingView

/**
 * 物品详情屏幕
 *
 * 显示物品详细信息，支持收藏和联系卖家功能
 *
 * **Validates: Requirements 4.5, 5.3, 10.3, 15.4, 15.5**
 *
 * Requirements:
 * - 4.5: 在物品详情界面显示匹配物品推荐
 * - 5.3: 提供物品详情界面
 * - 10.3: 物品详情界面提供Favorite按钮
 * - 15.4: 显示物品距离
 * - 15.5: 在物品卡片上显示距离
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    uiState: ItemDetailUiState,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onContactSeller: () -> Unit,
    onMatchedItemClick: (String) -> Unit,
    onInitiateExchange: (String) -> Unit,
    onExchangeMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 在已有数据的情况下出现错误时，通过Snackbar提示，避免覆盖详情内容
    LaunchedEffect(uiState.error) {
        if (uiState.error != null && uiState.item != null) {
            snackbarHostState.showSnackbar(uiState.error)
        }
    }

    // 发起交换的一次性反馈消息，通过Snackbar展示后回调清除
    LaunchedEffect(uiState.exchangeMessage) {
        uiState.exchangeMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onExchangeMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "物品详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 收藏按钮 - 反映当前收藏状态 (Requirement 10.3)
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) "已收藏" else "收藏",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when {
            // 初始加载状态（尚无数据）
            uiState.isLoading && uiState.item == null -> {
                LoadingView(modifier = contentModifier)
            }
            // 错误状态且无数据，提供返回入口
            uiState.error != null && uiState.item == null -> {
                ErrorView(
                    title = "加载失败",
                    message = uiState.error,
                    retryText = "返回",
                    onRetryClick = onBackClick,
                    modifier = contentModifier
                )
            }
            // 空状态
            uiState.item == null -> {
                EmptyStateView(
                    title = "物品不存在",
                    description = "该物品可能已被删除",
                    modifier = contentModifier
                )
            }
            // 物品详情内容
            else -> {
                ItemDetailContent(
                    uiState = uiState,
                    onContactSeller = onContactSeller,
                    onMatchedItemClick = onMatchedItemClick,
                    onInitiateExchange = onInitiateExchange,
                    modifier = contentModifier
                )
            }
        }
    }
}

/**
 * 物品详情内容
 */
@Composable
private fun ItemDetailContent(
    uiState: ItemDetailUiState,
    onContactSeller: () -> Unit,
    onMatchedItemClick: (String) -> Unit,
    onInitiateExchange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val item = uiState.item ?: return

    // 发起交换物品选择对话框的本地显示状态
    var showExchangeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // 图片轮播 (Requirement 5.3)
        if (item.images.isNotEmpty()) {
            ItemImageCarousel(
                images = item.images,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 物品基本信息
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // 名称和价格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = String.format("¥%.2f", item.estimatedPrice),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 标签
            if (item.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    item.tags.forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 距离信息 (Requirements 15.4, 15.5)
            if (uiState.distance != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "距离",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.distance,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 描述标题
            Text(
                text = "物品描述",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 描述内容 (Requirement 5.3)
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 联系卖家按钮
            Button(
                onClick = onContactSeller,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "联系卖家",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "联系卖家")
            }

            // 发起交换按钮（仅对非本人物品显示）
            if (!uiState.isOwnItem) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showExchangeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "发起交换")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // 匹配物品推荐 (Requirement 4.5)
        if (uiState.matchedItems.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "相似物品推荐",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.height(240.dp)
                ) {
                    items(
                        items = uiState.matchedItems,
                        key = { it.id }
                    ) { matchedItem ->
                        ItemCard(
                            item = matchedItem,
                            isFavorite = false,
                            distance = null,
                            onItemClick = { onMatchedItemClick(matchedItem.id) },
                            onFavoriteClick = { onMatchedItemClick(matchedItem.id) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // 发起交换：选择用于交换的本人物品对话框
    if (showExchangeDialog) {
        ExchangeItemSelectionDialog(
            myItems = uiState.myItems,
            onItemSelected = { selectedItemId ->
                showExchangeDialog = false
                onInitiateExchange(selectedItemId)
            },
            onDismiss = { showExchangeDialog = false }
        )
    }
}

/**
 * 发起交换物品选择对话框
 *
 * 列出当前用户可用于交换的物品，点击某项即选定并发起交换。
 * 当没有可交换物品时，提示用户先发布物品。
 */
@Composable
private fun ExchangeItemSelectionDialog(
    myItems: List<Item>,
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "选择用于交换的物品") },
        text = {
            if (myItems.isEmpty()) {
                Text(text = "你还没有可交换的物品，请先发布物品")
            } else {
                LazyColumn(
                    modifier = Modifier.height(280.dp)
                ) {
                    items(
                        items = myItems,
                        key = { it.id }
                    ) { myItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemSelected(myItem.id) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = myItem.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("¥%.2f", myItem.estimatedPrice),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

/**
 * ItemDetailScreen的UI状态类
 */
data class ItemDetailUiState(
    val item: Item? = null,
    val matchedItems: List<Item> = emptyList(),
    val isFavorite: Boolean = false,
    val distance: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOwnItem: Boolean = false,
    val myItems: List<Item> = emptyList(),
    val exchangeMessage: String? = null
)
