package com.example.exchangeapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.exchangeapp.util.CoilImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Campus Exchange App.
 *
 * - 使用[@HiltAndroidApp]启用Hilt依赖注入。
 * - 实现[ImageLoaderFactory]为Coil提供带内存/磁盘缓存策略的全局[ImageLoader]，
 *   使所有[coil.compose.AsyncImage]默认复用同一套缓存配置 (Requirements 6.2, 6.3)。
 */
@HiltAndroidApp
class ExchangeApplication : Application(), ImageLoaderFactory {

    /**
     * 提供全局Coil [ImageLoader]。Coil在首次加载图片时调用该方法并缓存结果。
     */
    override fun newImageLoader(): ImageLoader = CoilImageLoaderFactory.build(this)
}
