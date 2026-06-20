package com.example.exchangeapp.ui.screen.order

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

/**
 * 订单列表屏幕
 *
 * 显示和管理用户的交换订单。无状态(stateless)组件，
 * 通过[OrderListUiState]与回调函数驱动，由导航层连接[OrderListViewModel]。
 *
 * **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7**
 *
 * Requirements:
 * - 8.1: 显示订单管理界面
 * - 8.2: 显示用户的所有交换订单
 * - 8.3: 显示订单状态（待确认、进行中、已完成、已取消）
 * - 8.4: 点击订单显示详情
 * - 8.5: 待确认订单允许确认或取消
 * - 8.6: 进行中订单显示对方联系方式
 * - 8.7: 完成订单允许评价
 *
 * @param uiState 订单列表的UI状态
 * @param onOrderClick 点击订单回调，参数为订单ID (Requirement 8.4)
 * @param onConfirmOrder 确认订单回调，参数为订单ID (Requirement 8.5)
 * @param onCancelOrder 取消订单回调，参数为订单ID (Requirement 8.5)
 * @param onRateOrder 评价订单回调，参数为订单ID和评分 (Requirement 8.7)
 * @param onRefresh 下拉刷新回调
 */
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

    // 订单操作结果(确认/取消/评价)通过Snackbar反馈，避免打断列表浏览
    LaunchedEffect(uiState.operationMessage) {
        uiState.operationMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
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
            // 初始加载状态（尚无任何数据）
            uiState.isLoading && uiState.orders.isEmpty() -> {
                LoadingView(modifier = contentModifier)
            }
            // 错误状态且无数据，提供重试入口
            uiState.error != null && uiState.orders.isEmpty() -> {
                ErrorView(
                    title = "加载失败",
                    message = uiState.error,
                    onRetryClick = onRefresh,
                    modifier = contentModifier
                )
            }
            // 空状态：没有任何订单 (Requirement 8.1)
            uiState.orders.isEmpty() -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = contentModifier
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            EmptyStateView(
                                title = "暂无订单",
                                description = "您还没有任何交换订单",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            // 成功状态 - 显示订单列表 (Requirement 8.2)
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

/**
 * 订单卡片组件
 *
 * 显示单个订单的状态、时间信息和可用操作。
 *
 * - 整张卡片可点击，跳转到订单详情 (Requirement 8.4)
 * - 待确认订单显示"确认"和"取消"按钮 (Requirement 8.5)
 * - 已完成订单显示"评价"按钮 (Requirement 8.7)
 */
@Composable
private fun OrderCard(
    order: Order,
    onOrderClick: () -> Unit,
    onConfirmOrder: () -> Unit,
    onCancelOrder: () -> Unit,
    onRateOrder: (Int) -> Unit,
    onCompleteOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOrderClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 顶部：订单编号 + 状态徽章 (Requirement 8.3)
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

            Spacer(modifier = Modifier.height(8.dp))

            // 创建时间
            Text(
                text = "创建时间: ${formatTimestamp(order.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 完成时间（已完成订单显示）
            if (order.status == OrderStatus.COMPLETED && order.completedAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "完成时间: ${formatTimestamp(order.completedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 操作按钮区域
            when (order.status) {
                // 待确认：确认 / 取消 (Requirement 8.5)
                OrderStatus.PENDING -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelOrder,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "取消")
                        }
                        Button(
                            onClick = onConfirmOrder,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "确认")
                        }
                    }
                }
                // 已完成：评价 (Requirement 8.7)
                OrderStatus.COMPLETED -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onRateOrder(DEFAULT_RATING) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "评价")
                    }
                }
                // 进行中：完成交换 (Requirement 8.7)
                OrderStatus.IN_PROGRESS -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCompleteOrder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "完成交换")
                    }
                }
                // 已取消：无内联操作按钮
                else -> Unit
            }
        }
    }
}

/**
 * 订单状态徽章
 *
 * 以带颜色的标签展示订单状态（待确认、进行中、已完成、已取消）。
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

/** 评价默认分值，实际评分交互由订单详情/评价界面实现 */
private const val DEFAULT_RATING = 5
