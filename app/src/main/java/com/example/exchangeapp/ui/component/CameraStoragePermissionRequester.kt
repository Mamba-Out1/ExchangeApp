package com.example.exchangeapp.ui.component

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 相机与存储（媒体）权限请求处理器（可复用 Composable）
 *
 * 用户在 [PostItemScreen][com.example.exchangeapp.ui.screen.postitem.PostItemScreen] 中点击
 * “添加图片”时，需要相机权限拍照以及读取媒体/存储权限从相册选择 (Requirement 6.2)。
 * 该组件按需触发权限请求：
 * - 当 [request] 变为 `true` 时开始本次权限流程。
 * - 若权限已授予，直接回调 `onPermissionResult(true)`。
 * - 若未授予，先展示权限说明对话框 ([PermissionRationaleDialog])，用户确认后再向系统发起请求。
 * - 用户拒绝（关闭对话框或系统层拒绝）时回调 `onPermissionResult(false)`，
 *   调用方据此优雅地处理拒绝场景（如提示用户并禁用图片选择）。
 *
 * 该组件遵循 [LocationPermissionRequester] 的实现模式，复用 [PermissionRationaleDialog]，
 * 并兼容不同 Android 版本的存储权限差异：
 * - API 33+ (TIRAMISU) 使用 [Manifest.permission.READ_MEDIA_IMAGES]
 * - 更低版本使用 [Manifest.permission.READ_EXTERNAL_STORAGE]
 *
 * **Validates: Requirements 6.2**
 *
 * @param request 是否发起一次权限请求；调用方应在 [onPermissionResult] 中将其重置为 `false`
 * @param onPermissionResult 权限请求结束后的回调，参数为是否已授予所需权限
 * @param rationaleTitle 权限说明对话框标题
 * @param rationaleMessage 权限说明对话框内容
 */
@Composable
fun CameraStoragePermissionRequester(
    request: Boolean,
    onPermissionResult: (granted: Boolean) -> Unit,
    rationaleTitle: String = "需要相机和存储权限",
    rationaleMessage: String =
        "为了拍摄或从相册选择物品图片，需要获取相机和读取图片的权限。" +
            "若不授权，将无法添加图片。"
) {
    val context = LocalContext.current

    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = isCameraStoragePermissionGranted(context) ||
            result.values.all { it }
        onPermissionResult(granted)
    }

    // 当调用方发起请求时，检查权限并按需弹出说明对话框 (Requirement 6.2)
    LaunchedEffect(request) {
        if (!request) return@LaunchedEffect
        if (isCameraStoragePermissionGranted(context)) {
            onPermissionResult(true)
        } else {
            // 先展示权限说明，再请求系统权限
            showRationale = true
        }
    }

    if (showRationale) {
        PermissionRationaleDialog(
            title = rationaleTitle,
            message = rationaleMessage,
            onConfirm = {
                showRationale = false
                permissionLauncher.launch(requiredCameraStoragePermissions())
            },
            onDismiss = {
                // 用户拒绝授权，交由调用方优雅处理拒绝场景
                showRationale = false
                onPermissionResult(false)
            }
        )
    }
}

/**
 * 返回当前 Android 版本所需的相机与存储/媒体权限集合。
 *
 * - API 33+ (TIRAMISU)：[Manifest.permission.READ_MEDIA_IMAGES]
 * - 更低版本：[Manifest.permission.READ_EXTERNAL_STORAGE]
 */
private fun requiredCameraStoragePermissions(): Array<String> {
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return arrayOf(Manifest.permission.CAMERA, storagePermission)
}

/**
 * 判断当前是否已授予相机以及对应版本的存储/媒体读取权限。
 */
private fun isCameraStoragePermissionGranted(context: Context): Boolean {
    return requiredCameraStoragePermissions().all { permission ->
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
