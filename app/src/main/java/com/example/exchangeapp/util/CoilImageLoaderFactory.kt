package com.example.exchangeapp.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

/**
 * 集中配置Coil [ImageLoader]的工厂函数。
 *
 * 通过应用级[coil.ImageLoaderFactory]和Hilt依赖注入共享同一套缓存策略，
 * 避免重复配置导致内存/磁盘缓存被多次创建。
 *
 * 缓存策略 (Requirements 6.2, 6.3):
 * - 内存缓存: 占用应用可用内存的25%，加速重复展示的物品图片。
 * - 磁盘缓存: 占用缓存目录可用空间的2%（上限受Coil控制），持久化图片避免重复解码/下载。
 * - 启用内存与磁盘双重缓存策略，提升图片列表滚动性能。
 */
object CoilImageLoaderFactory {

    /** 磁盘缓存子目录名称。 */
    private const val DISK_CACHE_DIR = "image_cache"

    /** 内存缓存占用应用可用内存的比例。 */
    private const val MEMORY_CACHE_PERCENT = 0.25

    /** 磁盘缓存占用磁盘可用空间的比例。 */
    private const val DISK_CACHE_PERCENT = 0.02

    /**
     * 构建带有内存缓存与磁盘缓存策略的[ImageLoader]。
     *
     * @param context 应用上下文，用于解析缓存目录与可用内存。
     */
    fun build(context: Context): ImageLoader {
        val appContext = context.applicationContext
        return ImageLoader.Builder(appContext)
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve(DISK_CACHE_DIR))
                    .maxSizePercent(DISK_CACHE_PERCENT)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // 在内存充足时使用RGB_565以外的默认配置，崩溃时优雅降级
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
