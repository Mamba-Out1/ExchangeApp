package com.example.exchangeapp.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 通用的权限说明对话框
 *
 * 在向系统请求运行时权限之前，向用户解释为何需要该权限以及拒绝后的影响，
 * 提升授权率并满足权限请求的可解释性要求。可被位置、相机、存储等各类权限请求复用。
 *
 * **Validates: Requirements 15.1**
 *
 * @param title 对话框标题
 * @param message 权限用途说明文案
 * @param onConfirm 用户点击"授权"按钮时的回调（通常用于触发系统权限请求）
 * @param onDismiss 用户点击"暂不"或关闭对话框时的回调（通常用于回退到默认行为）
 * @param confirmText 确认按钮文案
 * @param dismissText 取消按钮文案
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "授权",
    dismissText: String = "暂不"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        },
        modifier = modifier
    )
}
