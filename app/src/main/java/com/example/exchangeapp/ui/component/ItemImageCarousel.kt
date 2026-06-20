package com.example.exchangeapp.ui.component

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.exchangeapp.ui.theme.LocalCustomColors
import kotlinx.coroutines.delay

/**
 * ItemImageCarousel组件 - 图片轮播组件
 *
 * 功能:
 * 1. 支持多张图片轮播显示
 * 2. 支持自动轮播
 * 3. 显示当前图片索引指示器
 * 4. 支持点击切换图片
 *
 * @param images 图片URL列表
 * @param autoPlay 是否自动轮播（默认true）
 * @param autoPlayInterval 自动轮播间隔时间（毫秒，默认3000ms）
 * @param onImageClick 点击图片回调（可选）
 * @param modifier Modifier修饰符
 */
@Composable
fun ItemImageCarousel(
    images: List<String>,
    autoPlay: Boolean = true,
    autoPlayInterval: Long = 3000L,
    onImageClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) {
        EmptyCarouselPlaceholder()
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val totalImages = images.size

    // 自动轮播逻辑
    if (autoPlay && totalImages > 1) {
        LaunchedEffect(currentIndex, autoPlayInterval) {
            delay(autoPlayInterval)
            currentIndex = (currentIndex + 1) % totalImages
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LocalCustomColors.current.cardImageBackground)
            .then(if (onImageClick != null) Modifier.clickable(onClick = onImageClick) else Modifier)
    ) {
        // 显示当前图片
        AsyncImage(
            model = images[currentIndex],
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )

        // 图片数量指示器
        if (totalImages > 1) {
            // 当前图片索引（如 "2/5"）
            ImageIndexIndicator(
                currentIndex = currentIndex + 1,
                totalImages = totalImages,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )

            // 底部圆点指示器
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalImages) { index ->
                    val isActive = index == currentIndex
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (isActive) 
                                    LocalCustomColors.current.carouselIndicatorActive 
                                else 
                                    LocalCustomColors.current.carouselIndicatorInactive
                            )
                    )
                }
            }

            // 左右切换按钮
            ImageNavigationControls(
                hasPrevious = currentIndex > 0,
                hasNext = currentIndex < totalImages - 1,
                onPrevious = { currentIndex = (currentIndex - 1 + totalImages) % totalImages },
                onNext = { currentIndex = (currentIndex + 1) % totalImages },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * 空轮播占位符
 */
@Composable
private fun EmptyCarouselPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LocalCustomColors.current.cardImageBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_gallery),
                contentDescription = "暂无图片",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/**
 * 图片索引指示器（如 "2/5"）
 */
@Composable
private fun ImageIndexIndicator(
    currentIndex: Int,
    totalImages: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "$currentIndex/$totalImages",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}



/**
 * 图片导航控制按钮
 */
@Composable
private fun ImageNavigationControls(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        // 上一个按钮
        if (hasPrevious) {
            NavigationButton(
                isPrevious = true,
                onClick = onPrevious,
                modifier = Modifier.padding(start = 16.dp)
            )
        } else {
            Box(modifier = Modifier.width(48.dp))
        }

        // 下一个按钮
        if (hasNext) {
            NavigationButton(
                isPrevious = false,
                onClick = onNext,
                modifier = Modifier.padding(end = 16.dp)
            )
        } else {
            Box(modifier = Modifier.width(48.dp))
        }
    }
}

/**
 * ���航按钮
 */
@Composable
private fun NavigationButton(
    isPrevious: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                if (isPrevious) android.R.drawable.ic_media_previous 
                else android.R.drawable.ic_media_next
            ),
            contentDescription = if (isPrevious) "上一张" else "下一张",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

