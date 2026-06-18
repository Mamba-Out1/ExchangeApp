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
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import com.example.exchangeapp.ui.component.ErrorView
import com.example.exchangeapp.ui.component.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 订单详情屏幕
 *
 * 展示单个订单的完整信息。无状态(stateless)组件，通过[OrderDetailUiState]
 * 与回调驱动，由导航层连接[OrderDetailViewModel]。
 *
 * **Validates: Requirements 8.4, 8.6, 8.7**
 *
 * Requirements:
 * - 8.4: 显示订单详情(订单号、状态、时间、交换物品)
 * - 8.6: 进行中订单显示对方User的联系方式
 * - 8.7: 完成订单允许User评价(5星)
 *
 * @param uiState 订单详情的UI状态
 * @param onBack 返回回调
 * @param onRate 评价回调，参数为评分(1-5) (Requirement 8.7)
 */
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
                title = { Text(text = "订单详情") },
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
            uiState.isLoading -> {
                LoadingView(modifier = contentModifier)
            }
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
                    item1Name = uiState.item1Name,
                    item2Name = uiState.item2Name,
                    counterpartContact = uiState.counterpartContact,
                    onRate = onRate,
                    modifier = contentModifier
                )
            }
        }
    }
}

/**
 * 订单详情的内容区域（无状态）
 */
@Composable
private fun OrderDetailContent(
    order: Order,
    item1Name: String?,
    item2Name: String?,
    counterpartContact: String?,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // 订单号 + 状态徽章 (Requirement 8.3, 8.4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "订单号: ${order.id.take(8)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OrderStatusBadge(status = order.status)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 交换物品信息
        DetailCard(title = "交换物品") {
            DetailRow(label = "物品一", value = item1Name ?: order.item1Id)
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "物品二", value = item2Name ?: order.item2Id)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 时间信息
        DetailCard(title = "时间信息") {
            DetailRow(label = "创建时间", value = formatTimestamp(order.createdAt))
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "更新时间", value = formatTimestamp(order.updatedAt))
            if (order.completedAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = "完成时间", value = formatTimestamp(order.completedAt))
            }
        }

        // 对方联系方式：仅进行中订单展示 (Requirement 8.6)
        if (order.status == OrderStatus.IN_PROGRESS) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailCard(title = "对方联系方式") {
                DetailRow(
                    label = "手机号",
                    value = counterpartContact ?: "暂无联系方式"
                )
            }
        }

        // 评价区域：仅已完成订单展示 (Requirement 8.7)
        if (order.status == OrderStatus.COMPLETED) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailCard(title = "评价") {
                RatingBar(
                    rating = order.rating ?: 0,
                    onRate = onRate
                )
                if (order.rating != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "已评价: ${order.rating}星",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 评价星级组件：5颗可点击的星，已评分的部分显示实心星。
 *
 * **Validates: Requirement 8.7**
 */
@Composable
private fun RatingBar(
    rating: Int,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (star in 1..5) {
            IconButton(onClick = { onRate(star) }) {
                // 核心图标集仅提供实心 Star，未评分的星以灰色实心星表示，
                // 避免引入 material-icons-extended 依赖。
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

/**
 * 详情信息卡片容器。
 */
@Composable
private fun DetailCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
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

/**
 * 标签-值的单行展示。
 */
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

/**
 * 订单状态徽章
 *
 * 以带颜色的标签展示订单状态（待确认、进行中、已完成、已取消）。
 * 与[OrderListScreen]保持一致的视觉风格，此处独立实现以保持组件自包含。
 *
 * **Validates: Requirement 8.3**
 */
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

/**
 * 将时间戳格式化为可读的日期时间字符串
 */
private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
