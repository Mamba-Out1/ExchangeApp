package com.example.exchangeapp.ui.screen.chat

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exchangeapp.ui.component.EmptyStateView
import com.example.exchangeapp.ui.component.ErrorView
import com.example.exchangeapp.ui.component.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 会话列表屏幕
 *
 * 展示当前登录用户的所有会话，点击某个会话进入与对方的聊天界面。
 * 通过[ChatListViewModel]加载会话数据。
 *
 * **Validates: Requirements 9.1, 9.3, 9.8**
 *
 * Requirements:
 * - 9.1: 提供聊天界面
 * - 9.3: 显示聊天历史记录
 * - 9.8: 显示未读消息数量标记
 *
 * @param onConversationClick 点击会话回调，参数为对方用户ID
 * @param modifier Modifier修饰符
 * @param viewModel 通过Hilt注入的[ChatListViewModel]
 */
@Composable
fun ChatListScreen(
    onConversationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatListContent(
        uiState = uiState,
        onConversationClick = onConversationClick,
        onRetry = viewModel::refresh,
        modifier = modifier
    )
}

/**
 * 会话列表界面的无状态内容，便于预览与测试。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListContent(
    uiState: ChatListState,
    onConversationClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "消息") }
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (uiState) {
            // 加载中 (Requirement 9.1)
            is ChatListState.Loading -> {
                LoadingView(message = "加载会话中...", modifier = contentModifier)
            }
            // 空状态：暂无会话 (Requirement 9.3)
            is ChatListState.Empty -> {
                EmptyStateView(
                    title = "暂无会话",
                    description = "去物品详情页联系卖家开始聊天吧",
                    modifier = contentModifier
                )
            }
            // 错误状态，提供重试入口
            is ChatListState.Error -> {
                ErrorView(
                    title = "加载失败",
                    message = uiState.message,
                    onRetryClick = onRetry,
                    modifier = contentModifier
                )
            }
            // 成功状态 - 展示会话列表
            is ChatListState.Success -> {
                LazyColumn(modifier = contentModifier) {
                    items(
                        items = uiState.conversations,
                        key = { it.otherUserId }
                    ) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation.otherUserId) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个会话行
 *
 * 展示对方昵称、最后一条消息预览(单行省略)、时间，以及未读数量徽章。
 * 整张卡片可点击进入聊天界面 (Requirement 9.8)。
 */
@Composable
private fun ConversationRow(
    conversation: ConversationSummary,
    onClick: () -> Unit,
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
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 顶部：对方昵称 + 时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.otherNickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(conversation.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 最后一条消息预览（单行省略）
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 未读消息数量标记 (Requirement 9.8)
            if (conversation.unreadCount > 0) {
                Spacer(modifier = Modifier.width(12.dp))
                Badge {
                    Text(
                        text = if (conversation.unreadCount > 99) {
                            "99+"
                        } else {
                            conversation.unreadCount.toString()
                        }
                    )
                }
            }
        }
    }
}

/**
 * 格式化消息时间戳为可读时间字符串。
 */
private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
