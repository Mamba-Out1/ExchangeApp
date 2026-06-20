package com.example.exchangeapp.ui.component

import android.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.ui.theme.LocalCustomColors

/**
 * EmptyStateView组件 - 空状态视图组件
 *
 * **Validates: Requirements 5.7** - IF 无更多Item可加载, THEN THE App SHALL显示"没有更多物品"提示
 *
 * 功能:
 * 1. 显示空状态图标
 * 2. 显示空状态标题
 * 3. 显示空状态描述
 * 4. 可选的操作按钮
 * 5. 支持自定义内容
 *
 * 使用场景:
 * - 物品列表为空
 * - 搜索结果为空
 * - 收藏列表为空
 * - 网络错误后无数据
 *
 * @param iconResId 图标资源ID（使用Android内置图标或自定义图标）
 * @param title 标题文本
 * @param description 描述文本（可选）
 * @param actionText 操作按钮文本（可选，如果为空则不显示按钮）
 * @param onActionClick 操作按钮点击回调（可选）
 * @param modifier Modifier修饰符
 */
@Composable
fun EmptyStateView(
    iconResId: Int = android.R.drawable.ic_menu_info_details,
    title: String,
    description: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = LocalCustomColors.current.emptyStateBackground
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // 图标
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // 描述（可选）
            description?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 操作按钮（可选）
            actionText?.let {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { onActionClick?.invoke() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(text = it)
                }
            }
        }
    }
}

/**
 * 特定场景的空状态视图
 */

/**
 * 物品列表为空状态
 *
 * @param onBrowseAction 浏览物品按钮点击回调（可选）
 */
@Composable
fun EmptyItemsState(
    onBrowseAction: (() -> Unit)? = null
) {
    EmptyStateView(
        iconResId = android.R.drawable.ic_menu_view,
        title = "暂无物品",
        description = "还没有物品发布，去发布第一个物品吧！",
        actionText = if (onBrowseAction != null) "浏览物品" else null,
        onActionClick = onBrowseAction
    )
}

/**
 * 搜索结果为空状态
 *
 * @param searchQuery 搜索关键词
 */
@Composable
fun EmptySearchState(
    searchQuery: String
) {
    EmptyStateView(
        iconResId = android.R.drawable.ic_menu_search,
        title = "未找到相关物品",
        description = "没有找到与 \"$searchQuery\" 相关的物品，请尝试其他关键词。"
    )
}

/**
 * 收藏列表为空状态
 *
 * @param onBrowseAction 浏览物品按钮点击回调（可选）
 */
@Composable
fun EmptyFavoritesState(
    onBrowseAction: (() -> Unit)? = null
) {
    EmptyStateView(
        iconResId = android.R.drawable.star_big_on,
        title = "暂无收藏",
        description = "您还没有收藏任何物品，快去发现有趣的物品吧！",
        actionText = if (onBrowseAction != null) "浏览物品" else null,
        onActionClick = onBrowseAction
    )
}

/**
 * 无更多物品加载状态（用于分页加载底部）
 *
 * 实现Requirement 5.7: IF 无更多Item可加载, THEN THE App SHALL显示"没有更多物品"提示
 */
@Composable
fun NoMoreItemsState(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Text(
            text = "没有更多物品了",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "已经到底啦",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 网络错误空状态
 *
 * @param onRetryAction 重试按钮点击回调
 */
@Composable
fun NetworkErrorState(
    onRetryAction: () -> Unit
) {
    EmptyStateView(
        iconResId = android.R.drawable.stat_notify_error,
        title = "网络连接错误",
        description = "无法连接到服务器，请检查网络连接后重试。",
        actionText = "重试",
        onActionClick = onRetryAction
    )
}

/**
 * 无网络连接空状态
 *
 * @param onCheckNetwork 检查网络按钮点击回调
 */
@Composable
fun NoNetworkState(
    onCheckNetwork: () -> Unit
) {
    EmptyStateView(
        iconResId = android.R.drawable.ic_menu_set_as,
        title = "无网络连接",
        description = "当前设备未连接到网络，请检查网络设置后重试。",
        actionText = "检查网络",
        onActionClick = onCheckNetwork
    )
}

