package com.example.exchangeapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 自定义颜色主题
 */
@Immutable
data class CustomColors(
    val cardImageBackground: Color,
    val emptyStateBackground: Color,
    val loadingBackground: Color,
    val errorBackground: Color,
    val carouselIndicatorActive: Color,
    val carouselIndicatorInactive: Color
)

/**
 * 默认的自定义颜色
 */
val customColors = CustomColors(
    cardImageBackground = Color(0xFFF5F5F5),
    emptyStateBackground = Color(0xFFF8F9FA),
    loadingBackground = Color(0xFFF5F5F5),
    errorBackground = Color(0xFFFDEDED),
    carouselIndicatorActive = Color(0xFF2196F3),
    carouselIndicatorInactive = Color(0xFFE0E0E0)
)

/**
 * 暗色模式的自定义颜色
 */
val darkCustomColors = CustomColors(
    cardImageBackground = Color(0xFF2D2D2D),
    emptyStateBackground = Color(0xFF1E1E1E),
    loadingBackground = Color(0xFF2D2D2D),
    errorBackground = Color(0xFF3D2D2D),
    carouselIndicatorActive = Color(0xFF64B5F6),
    carouselIndicatorInactive = Color(0xFF424242)
)

/**
 * 自定义颜色的CompositionLocal
 */
val LocalCustomColors = staticCompositionLocalOf { customColors }

/**
 * 获取当前主题的自定义颜色
 */
@Composable
fun getCustomColors(): CustomColors {
    return LocalCustomColors.current
}