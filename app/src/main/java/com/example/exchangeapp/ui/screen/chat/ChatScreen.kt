package com.example.exchangeapp.ui.screen.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.exchangeapp.domain.model.ChatMessage
import com.example.exchangeapp.domain.model.MessageType
import com.example.exchangeapp.ui.component.EmptyStateView
import com.example.exchangeapp.ui.component.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 聊天屏幕
 *
 * 提供用户间即时通讯功能，连接[ChatViewModel]观察消息流并发送文字/图片消息。
 *
 * **Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8**
 *
 * Requirements:
 * - 9.1: 提供聊天界面
 * - 9.2: 点击联系卖家打开聊天窗口
 * - 9.3: 显示聊天历史记录
 * - 9.4: 允许发送文字消息
 * - 9.5: 允许发送图片消息
 * - 9.6: 1秒内显示消息已发送状态
 * - 9.7: 按时间顺序显示消息
 * - 9.8: 显示未读消息数量标记
 *
 * @param otherUserId 聊天对方的用户ID
 * @param onBack 返回回调
 * @param viewModel 通过Hilt注入的[ChatViewModel]
 */
@Composable
fun ChatScreen(
    otherUserId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    // 进入屏幕时初始化会话 (Requirement 9.2)
    LaunchedEffect(otherUserId) {
        if (otherUserId.isNotEmpty()) {
            viewModel.initializeConversation(otherUserId)
        }
    }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val messageInput by viewModel.messageInput.collectAsStateWithLifecycle()
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    ChatContent(
        otherUserId = otherUserId,
        messages = messages,
        messageInput = messageInput,
        chatState = chatState,
        unreadCount = unreadCount,
        onMessageInputChange = viewModel::updateMessageInput,
        onSendText = viewModel::sendTextMessage,
        onSendImage = viewModel::sendImageMessage,
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * 聊天界面的无状态内容，便于预览与测试。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(
    otherUserId: String,
    messages: List<ChatMessage>,
    messageInput: String,
    chatState: ChatState,
    unreadCount: Int,
    onMessageInputChange: (String) -> Unit,
    onSendText: () -> Unit,
    onSendImage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // 错误状态通过Snackbar提示
    LaunchedEffect(chatState) {
        if (chatState is ChatState.Error) {
            snackbarHostState.showSnackbar(chatState.message)
        }
    }

    // 新消息到达时自动滚动到底部 (Requirement 9.7)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 图片选择器：选取图片后作为图片消息发送 (Requirement 9.5)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onSendImage(it.toString()) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "聊天",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // 未读消息数量标记 (Requirement 9.8)
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge {
                                Text(text = if (unreadCount > 99) "99+" else unreadCount.toString())
                            }
                        }
                    }
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    // 初始加载且尚无消息
                    chatState is ChatState.Loading && messages.isEmpty() -> {
                        LoadingView(message = "加载消息中...")
                    }
                    // 无消息空状态 (Requirement 9.3)
                    messages.isEmpty() -> {
                        EmptyStateView(
                            title = "暂无消息",
                            description = "发送第一条消息，开始对话吧！"
                        )
                    }
                    // 消息列表 (Requirements 9.3, 9.7)
                    else -> {
                        MessageList(
                            messages = messages,
                            otherUserId = otherUserId,
                            listState = listState
                        )
                    }
                }
            }

            // 消息输入区 (Requirements 9.4, 9.5)
            MessageInputBar(
                messageInput = messageInput,
                isSending = chatState is ChatState.Sending,
                onMessageInputChange = onMessageInputChange,
                onSendText = onSendText,
                onPickImage = { imagePickerLauncher.launch("image/*") }
            )
        }
    }
}

/**
 * 消息列表，按时间顺序展示消息 (Requirement 9.7)。
 */
@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    otherUserId: String,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            // 对方发送的消息靠左，自己发送的消息靠右
            val isFromMe = message.senderId != otherUserId
            MessageBubble(
                message = message,
                isFromMe = isFromMe
            )
        }
    }
}

/**
 * 单条消息气泡，支持文字与图片消息显示 (Requirements 9.4, 9.5)。
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isFromMe: Boolean
) {
    val bubbleColor = if (isFromMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isFromMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(12.dp)
            ) {
                when (message.messageType) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    MessageType.IMAGE -> {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "图片消息",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 时间戳 + 已发送状态 (Requirements 9.6, 9.7)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (isFromMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (message.isRead) "已读" else "已发送",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * 消息输入栏，包含文字输入框、图片按钮与发送按钮 (Requirements 9.4, 9.5)。
 */
@Composable
private fun MessageInputBar(
    messageInput: String,
    isSending: Boolean,
    onMessageInputChange: (String) -> Unit,
    onSendText: () -> Unit,
    onPickImage: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图片消息按钮 (Requirement 9.5)
            IconButton(onClick = onPickImage) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_gallery),
                    contentDescription = "发送图片",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 文字输入框 (Requirement 9.4)
            OutlinedTextField(
                value = messageInput,
                onValueChange = onMessageInputChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 120.dp),
                placeholder = { Text("输入消息...") },
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 发送按钮 (Requirements 9.4, 9.6)
            IconButton(
                onClick = onSendText,
                enabled = messageInput.isNotBlank() && !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "发送",
                        tint = if (messageInput.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
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
