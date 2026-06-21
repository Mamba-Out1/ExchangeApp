package com.example.exchangeapp.ui.screen.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import com.example.exchangeapp.ui.component.EmptyStateView
import com.example.exchangeapp.ui.component.ErrorView
import com.example.exchangeapp.ui.component.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    uiState: OrderListUiState,
    onOrderClick: (String) -> Unit,
    onConfirmOrder: (String) -> Unit,
    onCancelOrder: (String) -> Unit,
    onRateOrder: (String, Int) -> Unit,
    onCompleteOrder: (String) -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.operationMessage) {
        uiState.operationMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when {
            uiState.isLoading && uiState.orders.isEmpty() -> {
                LoadingView(modifier = contentModifier)
            }

            uiState.error != null && uiState.orders.isEmpty() -> {
                ErrorView(
                    title = "加载失败",
                    message = uiState.error,
                    onRetryClick = onRefresh,
                    modifier = contentModifier
                )
            }

            uiState.orders.isEmpty() -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = contentModifier
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            EmptyStateView(
                                title = "暂无交换记录",
                                description = "发起或收到以物易物请求后，会在这里显示",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = contentModifier
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = uiState.orders,
                            key = { it.id }
                        ) { order ->
                            OrderCard(
                                order = order,
                                currentUserId = uiState.currentUserId,
                                onOrderClick = { onOrderClick(order.id) },
                                onConfirmOrder = { onConfirmOrder(order.id) },
                                onCancelOrder = { onCancelOrder(order.id) },
                                onRateOrder = { rating -> onRateOrder(order.id, rating) },
                                onCompleteOrder = { onCompleteOrder(order.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    currentUserId: String?,
    onOrderClick: () -> Unit,
    onConfirmOrder: () -> Unit,
    onCancelOrder: () -> Unit,
    onRateOrder: (Int) -> Unit,
    onCompleteOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOrderClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "交换记录 ${order.id.take(8)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OrderStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "创建时间: ${formatTimestamp(order.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (order.status == OrderStatus.COMPLETED && order.completedAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "完成时间: ${formatTimestamp(order.completedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (order.status) {
                OrderStatus.PENDING -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (order.user2Id == currentUserId) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancelOrder,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "拒绝")
                            }
                            Button(
                                onClick = onConfirmOrder,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "确认交换")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "等待对方确认",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(onClick = onCancelOrder) {
                                Text(text = "取消请求")
                            }
                        }
                    }
                }

                OrderStatus.COMPLETED -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onRateOrder(DEFAULT_RATING) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "评价")
                    }
                }

                OrderStatus.IN_PROGRESS -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCompleteOrder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "完成交换")
                    }
                }

                OrderStatus.CANCELLED -> Unit
            }
        }
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

private const val DEFAULT_RATING = 5
