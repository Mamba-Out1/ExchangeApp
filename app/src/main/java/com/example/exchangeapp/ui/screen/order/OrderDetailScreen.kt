package com.example.exchangeapp.ui.screen.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import com.example.exchangeapp.domain.model.User
import com.example.exchangeapp.ui.component.ErrorView
import com.example.exchangeapp.ui.component.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    uiState: OrderDetailUiState,
    onBack: () -> Unit,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "交换记录详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
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
            uiState.isLoading -> LoadingView(modifier = contentModifier)

            uiState.error != null && uiState.order == null -> {
                ErrorView(
                    title = "加载失败",
                    message = uiState.error,
                    onRetryClick = onBack,
                    modifier = contentModifier
                )
            }

            uiState.order != null -> {
                OrderDetailContent(
                    order = uiState.order,
                    item1 = uiState.item1,
                    item2 = uiState.item2,
                    counterpartUser = uiState.counterpartUser,
                    onRate = onRate,
                    modifier = contentModifier
                )
            }
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: Order,
    item1: Item?,
    item2: Item?,
    counterpartUser: User?,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "记录 ${order.id.take(8)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OrderStatusBadge(status = order.status)
        }

        DetailCard(title = "交换物品") {
            ExchangeItemRow(label = "发起方物品", item = item1, fallbackId = order.item1Id)
            Spacer(modifier = Modifier.height(10.dp))
            ExchangeItemRow(label = "接收方物品", item = item2, fallbackId = order.item2Id)
        }

        DetailCard(title = "对方用户") {
            DetailRow(label = "昵称", value = counterpartUser?.nickname ?: "未知用户")
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "手机号", value = counterpartUser?.phone ?: "暂无联系方式")
            if (!counterpartUser?.campusLocation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = "校区", value = counterpartUser?.campusLocation ?: "")
            }
        }

        DetailCard(title = "时间信息") {
            DetailRow(label = "创建时间", value = formatTimestamp(order.createdAt))
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "更新时间", value = formatTimestamp(order.updatedAt))
            if (order.completedAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = "完成时间", value = formatTimestamp(order.completedAt))
            }
        }

        if (order.status == OrderStatus.COMPLETED) {
            DetailCard(title = "评价") {
                RatingBar(
                    rating = order.rating ?: 0,
                    onRate = onRate
                )
                if (order.rating != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "已评价 ${order.rating} 星",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExchangeItemRow(
    label: String,
    item: Item?,
    fallbackId: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = item?.name ?: fallbackId,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (item != null) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.tags.isNotEmpty()) {
                Text(
                    text = item.tags.take(5).joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RatingBar(
    rating: Int,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (star in 1..5) {
            IconButton(onClick = { onRate(star) }) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "评分 $star 星",
                    tint = if (star <= rating) Color(0xFFFFB300)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OrderStatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val (label, backgroundColor, contentColor) = when (status) {
        OrderStatus.PENDING -> Triple("待确认", Color(0xFFFFF3E0), Color(0xFFE65100))
        OrderStatus.IN_PROGRESS -> Triple("进行中", Color(0xFFE3F2FD), Color(0xFF1565C0))
        OrderStatus.COMPLETED -> Triple("已完成", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        OrderStatus.CANCELLED -> Triple("已取消", Color(0xFFF5F5F5), Color(0xFF757575))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Text(
            text = label,
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
