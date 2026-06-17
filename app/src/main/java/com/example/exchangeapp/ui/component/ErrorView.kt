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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.ui.theme.LocalCustomColors

/**
 * ErrorView组件 - 错误提示组件
 *
 * 功能:
 * 1. 显示错误图标和错误信息
 * 2. 支持不同的错误类型
 * 3. 提供重试操作按钮
 * 4. 支持自定义错误处理
 *
 * 使用场景:
 * - 网络请求失败
 * - 数据加载失败
 * - 权限获取失败
 * - 操作执行失败
 *
 * @param iconResId 错误图标资源ID
 * @param title 错误标题
 * @param message 错误信息
 * @param retryText 重试按钮文本（可选，如果为空则不显示重试按钮）
 * @param secondaryText 次要按钮文本（可选）
 * @param onRetryClick 重试按钮点击回调（可选）
 * @param onSecondaryClick 次要按钮点击回调（可选）
 * @param modifier Modifier修饰符
 */
@Composable
fun ErrorView(
    iconResId: Int = android.R.drawable.stat_notify_error,
    title: String,
    message: String,
    retryText: String? = "重试",
    secondaryText: String? = null,
    onRetryClick: (() -> Unit)? = null,
    onSecondaryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = LocalCustomColors.current.errorBackground
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // 错误图标
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 错误标题
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 错误信息
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // 按钮区域
            if (retryText != null || secondaryText != null) {
                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 重试按钮
                    retryText?.let {
                        Button(
                            onClick = { onRetryClick?.invoke() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 次要按钮
                    secondaryText?.let {
                        OutlinedButton(
                            onClick = { onSecondaryClick?.invoke() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 网络错误组件
 *
 * @param onRetryClick 重试按钮点击回调
 * @param onCheckNetwork 检查网络按钮点击回调（可选）
 */
@Composable
fun NetworkErrorView(
    onRetryClick: () -> Unit,
    onCheckNetwork: (() -> Unit)? = null
) {
    ErrorView(
        iconResId = android.R.drawable.ic_dialog_alert,
        title = "网络连接失败",
        message = "无法连接到服务器，请检查您的网络连接后重试。",
        retryText = "重试",
        secondaryText = if (onCheckNetwork != null) "检查网络" else null,
        onRetryClick = onRetryClick,
        onSecondaryClick = onCheckNetwork
    )
}

/**
 * 数据加载错误组件
 *
 * @param errorMessage 错误信息
 * @param onRetryClick 重试按钮点击回调
 */
@Composable
fun DataLoadErrorView(
    errorMessage: String,
    onRetryClick: () -> Unit
) {
    ErrorView(
        iconResId = android.R.drawable.stat_notify_error,
        title = "数据加载失败",
        message = errorMessage,
        retryText = "重新加载",
        onRetryClick = onRetryClick
    )
}

/**
 * 权限错误组件
 *
 * @param permissionName 权限名称
 * @param onGoToSettings 前往设置按钮点击回调
 */
@Composable
fun PermissionErrorView(
    permissionName: String,
    onGoToSettings: () -> Unit
) {
    ErrorView(
        iconResId = android.R.drawable.ic_lock_lock,
        title = "权限被拒绝",
        message = "需要${permissionName}权限才能继续使用该功能，请在设置中授予权限。",
        retryText = "前往设置",
        onRetryClick = onGoToSettings
    )
}

/**
 * 位置服务错误组件
 *
 * @param onEnableLocation 启���定位按钮点击回调
 * @param onUseDefaultLocation 使用默认位置按钮点击回调（可选）
 */
@Composable
fun LocationServiceErrorView(
    onEnableLocation: () -> Unit,
    onUseDefaultLocation: (() -> Unit)? = null
) {
    ErrorView(
        iconResId = android.R.drawable.ic_menu_mylocation,
        title = "定位服务未开启",
        message = "需要开启定位服务才能获取准确的位置信息。",
        retryText = "开启定位",
        secondaryText = if (onUseDefaultLocation != null) "使用默认位置" else null,
        onRetryClick = onEnableLocation,
        onSecondaryClick = onUseDefaultLocation
    )
}

/**
 * 服务器错误组件
 *
 * @param errorCode 错误代码
 * @param onRetryClick 重试按钮点击回调
 * @param onReportIssue 报告问题按钮点击回调（可选）
 */
@Composable
fun ServerErrorView(
    errorCode: Int = 500,
    onRetryClick: () -> Unit,
    onReportIssue: (() -> Unit)? = null
) {
    val errorTitle = when (errorCode) {
        404 -> "资源未找到"
        500 -> "服务器内部错误"
        503 -> "服务暂时不可用"
        else -> "服务器错误 ($errorCode)"
    }

    val errorMessage = when (errorCode) {
        404 -> "请求的资源不存在，请稍后再试。"
        500 -> "服务器遇到问题，请稍后重试。"
        503 -> "服务器正在维护，请稍后访问。"
        else -> "服务器返回错误代码 $errorCode，请稍后重试。"
    }

    ErrorView(
        iconResId = android.R.drawable.stat_notify_error,
        title = errorTitle,
        message = errorMessage,
        retryText = "重试",
        secondaryText = if (onReportIssue != null) "报告问题" else null,
        onRetryClick = onRetryClick,
        onSecondaryClick = onReportIssue
    )
}

/**
 * 无网络连接组件
 *
 * @param onCheckNetwork 检查网络按钮点击回调
 */
@Composable
fun NoNetworkView(
    onCheckNetwork: () -> Unit
) {
    ErrorView(
        iconResId = android.R.drawable.ic_menu_set_as,
        title = "无网络连接",
        message = "当前设备未连接到网络，请检查网络设置。",
        retryText = "检查网络",
        onRetryClick = onCheckNetwork
    )
}

/**
 * 操作失败组件
 *
 * @param operationName 操作名称（如"发布物品"、"上传图片"等）
 * @param errorMessage 错误信息
 * @param onRetryClick 重试按钮点击回调
 * @param onCancelClick 取消按钮点击回调（可选）
 */
@Composable
fun OperationErrorView(
    operationName: String,
    errorMessage: String,
    onRetryClick: () -> Unit,
    onCancelClick: (() -> Unit)? = null
) {
    ErrorView(
        iconResId = android.R.drawable.ic_dialog_info,
        title = "${operationName}失败",
        message = errorMessage,
        retryText = "重试",
        secondaryText = if (onCancelClick != null) "取消" else null,
        onRetryClick = onRetryClick,
        onSecondaryClick = onCancelClick
    )
}

/**
 * 内联错误提示（用于表单验证等场景）
 *
 * @param message 错误信息
 * @param modifier Modifier修饰符
 */
@Composable
fun InlineErrorView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Icon(
            painter = painterResource(android.R.drawable.ic_dialog_alert),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 简化的错误提示（用于Toast或Snackbar替代）
 *
 * @param message 错误信息
 * @param onRetryClick 重试按钮点击回调（可选）
 */
@Composable
fun SimpleErrorView(
    message: String,
    onRetryClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        onRetryClick?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = it,
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "重试",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}