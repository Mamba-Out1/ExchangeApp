package com.example.exchangeapp.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.exchangeapp.R
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.User
import com.example.exchangeapp.ui.component.ErrorView
import com.example.exchangeapp.ui.component.ItemCard
import com.example.exchangeapp.ui.component.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onItemClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onOrderClick: (String) -> Unit,
    onEditItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
            uiState.isLoading && uiState.user == null -> {
                LoadingView(modifier = contentModifier)
            }

            uiState.error != null && uiState.user == null -> {
                ErrorView(
                    title = "加载失败",
                    message = uiState.error,
                    onRetryClick = onRetry,
                    modifier = contentModifier
                )
            }

            else -> {
                ProfileContent(
                    uiState = uiState,
                    onItemClick = onItemClick,
                    onFavoriteClick = onFavoriteClick,
                    onOrderClick = onOrderClick,
                    onEditItem = onEditItem,
                    onDeleteItem = onDeleteItem,
                    onLogout = onLogout,
                    modifier = contentModifier
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onItemClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onOrderClick: (String) -> Unit,
    onEditItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var itemPendingDeletion by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = modifier) {
        item(key = "profile_header") {
            ProfileHeader(
                user = uiState.user,
                exchangeCount = uiState.exchangeCount,
                onLogout = onLogout,
                modifier = Modifier.padding(16.dp)
            )
        }

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

        item(key = "history_header") {
            SectionHeader(
                title = "历史交换记录",
                count = uiState.completedOrders.size,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (uiState.completedOrders.isEmpty()) {
            item(key = "history_empty") {
                EmptySectionHint(
                    text = "完成交换后，记录会显示在这里",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            items(
                items = uiState.completedOrders,
                key = { "history_${it.id}" }
            ) { order ->
                ExchangeHistoryCard(
                    order = order,
                    onClick = { onOrderClick(order.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

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

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

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

@Composable
private fun ProfileHeader(
    user: User?,
    exchangeCount: Int,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.nickname ?: "未登录",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user?.phone ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!user?.campusLocation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user?.campusLocation ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

@Composable
private fun PublishedItemCard(
    item: Item,
    onItemClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box {
            ItemCard(
                item = item,
                isFavorite = false,
                distance = null,
                onItemClick = onItemClick,
                onFavoriteClick = onItemClick,
                modifier = Modifier.fillMaxWidth()
            )

            if (item.status == ItemStatus.EXCHANGED) {
                StatusBadge(
                    text = "已完成交换",
                    backgroundColor = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF2E7D32),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (item.status == ItemStatus.EXCHANGED) {
            Text(
                text = "该物品已完成交换，已从主页下架。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
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
}

@Composable
private fun ExchangeHistoryCard(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "交换记录 ${order.id.take(8)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(
                    text = "已完成",
                    backgroundColor = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF2E7D32)
                )
            }
            Text(
                text = "完成时间: ${formatTimestamp(order.completedAt ?: order.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击查看交换物品和对方用户信息",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
