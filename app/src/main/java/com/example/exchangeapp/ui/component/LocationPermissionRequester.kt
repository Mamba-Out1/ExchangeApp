package com.example.exchangeapp.ui.component

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 位置权限请求处理器（可复用 Composable）
 *
 * 在进入界面时自动检查并请求位置权限 (Requirement 15.1)：
 * - 若权限已授予，直接回调 `onPermissionResult(true)`。
 * - 若未授予，先展示权限说明对话框 ([PermissionRationaleDialog])，用户确认后再向系统发起请求。
 * - 用户拒绝（关闭对话框或系统层拒绝）时回调 `onPermissionResult(false)`，
 *   调用方据此回退到默认校区位置 (Requirement 15.6)。
 *
 * 该组件在 [HomeScreen][com.example.exchangeapp.ui.screen.home.HomeScreen] 和
 * [PostItemScreen][com.example.exchangeapp.ui.screen.postitem.PostItemScreen] 中复用。
 *
 * **Validates: Requirements 15.1, 15.6**
 *
 * @param onPermissionResult 权限请求结束后的回调，参数为是否已授予位置权限
 * @param rationaleTitle 权限说明对话框标题
 * @param rationaleMessage 权限说明对话框内容
 */
@Composable
fun LocationPermissionRequester(
    onPermissionResult: (granted: Boolean) -> Unit = {},
    rationaleTitle: String = "需要位置权限",
    rationaleMessage: String =
        "为了向你推荐附近的物品并显示距离，需要获取你的位置。" +
            "若不授权，将使用默认校区位置。"
) {
    val context = LocalContext.current

    // 跨配置变更（如旋转屏幕）记录是否已经处理过本次权限流程，避免重复弹窗
    var handled by rememberSaveable { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        handled = true
        onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        if (handled) return@LaunchedEffect
        if (isLocationPermissionGranted(context)) {
            handled = true
            onPermissionResult(true)
        } else {
            // 先展示权限说明，再请求系统权限 (Requirement 15.1)
            showRationale = true
        }
    }

    if (showRationale) {
        PermissionRationaleDialog(
            title = rationaleTitle,
            message = rationaleMessage,
            onConfirm = {
                showRationale = false
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = {
                // 用户拒绝授权，回退到默认校区位置 (Requirement 15.6)
                showRationale = false
                handled = true
                onPermissionResult(false)
            }
        )
    }
}

/**
 * 判断当前是否已授予精确或粗略位置权限。
 */
private fun isLocationPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
