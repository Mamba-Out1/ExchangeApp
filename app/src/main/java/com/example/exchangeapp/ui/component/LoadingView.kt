package com.example.exchangeapp.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.ui.theme.LocalCustomColors

/**
 * LoadingView组件 - 加载动画组件
 *
 * 功能:
 * 1. 全屏加载动画
 * 2. 内联加载动画（在列表项中）
 * 3. 进度条加载动画
 * 4. 带文字的加载动画
 * 5. 骨架屏加载动画
 *
 * 使用场景:
 * - 页面初始化加载
 * - 下拉刷新加载
 * - 上滑加载更多
 * - 数据提交加载
 * - 图片上传加载
 *
 * @param message 加载提示文字（可选）
 * @param modifier Modifier修饰符
 */
@Composable
fun LoadingView(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = LocalCustomColors.current.loadingBackground.copy(alpha = 0.9f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            message?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 内联加载组件（用于列表项中）
 *
 * @param modifier Modifier修饰符
 */
@Composable
fun InlineLoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 进度条加载组件
 *
 * @param progress 进度值（0.0-1.0）
 * @param message 加载提示文字（可选）
 * @param modifier Modifier修饰符
 */
@Composable
fun ProgressLoadingView(
    progress: Float,
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        LinearProgressIndicator(
            progress = progress,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )

        message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 显示百分比
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 骨架屏加载组件（用于列表加载）
 *
 * @param count 骨架屏项数量
 * @param modifier Modifier修饰符
 */
@Composable
fun SkeletonLoadingView(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        repeat(count) {
            SkeletonItem()
        }
    }
}

/**
 * 骨架屏列表项
 */
@Composable
private fun SkeletonItem() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Color.LightGray.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Box(
            modifier = Modifier.padding(12.dp)
        ) {
            // 左侧图片占位
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(96.dp, 96.dp)
            ) {}

            // 右侧内容占位
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(start = 112.dp)
                    .fillMaxSize()
            ) {
                // 标题占位
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                ) {}

                // 描述占位
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                ) {}

                // 价格占位
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(14.dp)
                ) {}
            }
        }
    }
}

/**
 * 小型加载指示器（用于按钮等小元素）
 *
 * @param size 指示器大小
 * @param color 指示器颜色
 * @param modifier Modifier修饰符
 */
@Composable
fun SmallLoadingIndicator(
    size: Int = 20,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size.dp)
    ) {
        CircularProgressIndicator(
            color = color,
            strokeWidth = 2.dp,
            modifier = Modifier.size((size * 0.8).dp)
        )
    }
}

/**
 * 特定场景的加载组件
 */

/**
 * 列表加载更多组件
 *
 * 用于上滑加载更多时的底部加载组件
 */
@Composable
fun LoadMoreLoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "正在加载更多...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 下拉刷新加载组件
 */
@Composable
fun RefreshLoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * 图片上传加载组件
 *
 * @param progress 上传进度（0.0-1.0）
 */
@Composable
fun ImageUploadLoadingView(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                progress = progress,
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "正在上传图片...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

