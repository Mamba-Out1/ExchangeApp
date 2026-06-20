package com.example.exchangeapp.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.exchangeapp.R
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.User
import com.example.exchangeapp.ui.component.ErrorView
import com.example.exchangeapp.ui.component.ItemCard
import com.example.exchangeapp.ui.component.LoadingView

/**
 * 个人中心屏幕
 *
 * 显示用户信息、已发布物品列表和收藏列表
 *
 * **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 10.5**
 *
 * Requirements:
 * - 7.1: 显示个人中心界面
 * - 7.2: 显示头像、昵称、联系方式
 * - 7.3: 显示用户发布的所有物品列表
 * - 7.4: 显示收藏列表
 * - 7.5: 点击已发布物品允许编辑或删除
 * - 7.6: 点击收藏物品跳转到详情页
 * - 7.7: 显示历史交换记录数量
 * - 10.5: 在个人中心显示完整的Favorite_List
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onItemClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onEditItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 在已有数据的情况下出现错误时，通过Snackbar提示，避免覆盖内容
    LaunchedEffect(uiState.error) {
        if (uiState.error != null && uiState.user != null) {
            snackbarHostState.showSnackbar(uiState.error)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_title_profile),
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
            // 初始加载状态（尚无任何数据）
            uiState.isLoading && uiState.user == null -> {
                LoadingView(modifier = contentModifier)
            }
            // 错误状态且无数据，提供重试入口
            uiState.error != null && uiState.user == null -> {
                ErrorView(
                    title = "加载失败",
                    message = uiState.error,
                    onRetryClick = onRetry,
                    modifier = contentModifier
                )
            }
            // 内容状态
            else -> {
                ProfileContent(
                    uiState = uiState,
                    onItemClick = onItemClick,
                    onFavoriteClick = onFavoriteClick,
                    onEditItem = onEditItem,
                    onDeleteItem = onDeleteItem,
                    onLogout = onLogout,
                    modifier = contentModifier
                )
            }
        }
    }
}

/**
 * 个人中心内容区
 *
 * 使用LazyColumn渲染个人信息头部、已发布物品列表和收藏列表
 */
@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onItemClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onEditItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 删除确认对话框状态：保存待删除的物品ID
    var itemPendingDeletion by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
    ) {
        // 个人信息头部 (Requirement 7.2, 7.7)
        item(key = "profile_header") {
            ProfileHeader(
                user = uiState.user,
                exchangeCount = uiState.exchangeCount,
                onLogout = onLogout,
                modifier = Modifier.padding(16.dp)
            )
        }

        // 已发布物品标题 (Requirement 7.3)
        item(key = "published_header") {
            SectionHeader(
                title = "我发布的物品",
                count = uiState.postedItems.size,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (uiState.postedItems.isEmpty()) {
            item(key = "published_empty") {
                EmptySectionHint(
                    text = "你还没有发布任何物品",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            // 已发布物品列表 (Requirement 7.3, 7.5)
            items(
                items = uiState.postedItems,
                key = { "published_${it.id}" }
            ) { item ->
                PublishedItemCard(
                    item = item,
                    onItemClick = { onItemClick(item.id) },
                    onEditClick = { onEditItem(item.id) },
                    onDeleteClick = { itemPendingDeletion = item.id },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // 收藏列表标题 (Requirement 7.4, 10.5)
        item(key = "favorites_header") {
            SectionHeader(
                title = "我的收藏",
                count = uiState.favoriteItems.size,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (uiState.favoriteItems.isEmpty()) {
            item(key = "favorites_empty") {
                EmptySectionHint(
                    text = "你还没有收藏任何物品",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            // 收藏物品列表 (Requirement 7.4, 7.6, 10.5)
            items(
                items = uiState.favoriteItems,
                key = { "favorite_${it.id}" }
            ) { item ->
                ItemCard(
                    item = item,
                    isFavorite = true,
                    distance = null,
                    onItemClick = { onFavoriteClick(item.id) },
                    onFavoriteClick = { onFavoriteClick(item.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // 底部留白
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 删除确认对话框 (Requirement 7.5)
    itemPendingDeletion?.let { pendingId ->
        AlertDialog(
            onDismissRequest = { itemPendingDeletion = null },
            title = { Text(text = "删除物品") },
            text = { Text(text = "确定要删除这个物品吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteItem(pendingId)
                        itemPendingDeletion = null
                    }
                ) {
                    Text(text = "删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingDeletion = null }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

/**
 * 个人信息头部
 *
 * 显示用户头像、昵称、联系方式、校区以及历史交换记录数量
 *
 * Requirement 7.2: 显示头像、昵称、联系方式
 * Requirement 7.7: 显示历史交换记录数量
 */
@Composable
private fun ProfileHeader(
    user: User?,
    exchangeCount: Int,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val avatar = user?.avatar
                    if (!avatar.isNullOrBlank()) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = "用户头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_myplaces),
                            contentDescription = "用户头像",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 昵称和联系方式
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user?.nickname ?: "未登录",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 联系方式（手机号）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_call),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = user?.phone ?: "-",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!user?.campusLocation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_mylocation),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = user?.campusLocation ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 历史交换记录数量 (Requirement 7.7)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = exchangeCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "历史交换记录",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = onLogout) {
                    Text(text = "退出登录")
                }
            }
        }
    }
}

/**
 * 区块标题，带数量统计
 */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 区块空状态提示
 */
@Composable
private fun EmptySectionHint(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * 已发布物品卡片
 *
 * 在ItemCard基础上增加编辑和删除操作按钮
 *
 * Requirement 7.5: 点击已发布物品允许编辑或删除
 */
@Composable
private fun PublishedItemCard(
    item: Item,
    onItemClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ItemCard(
            item = item,
            isFavorite = false,
            distance = null,
            onItemClick = onItemClick,
            onFavoriteClick = onItemClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 编辑 / 删除 操作行
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onEditClick) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_edit),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "编辑")
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "删除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
