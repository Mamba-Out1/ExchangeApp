package com.example.exchangeapp.di

import android.content.Context
import coil.ImageLoader
import com.example.exchangeapp.util.CoilImageLoaderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 图片加载依赖模块。
 *
 * 提供与应用级[coil.ImageLoaderFactory]一致的Coil [ImageLoader]，
 * 便于在需要显式指定ImageLoader的场景（如预加载、测试）通过依赖注入获取，
 * 复用同一套内存/磁盘缓存策略 (Requirements 6.2, 6.3)。
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    /**
     * 提供全局共享的Coil [ImageLoader]。
     */
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return CoilImageLoaderFactory.build(context)
    }
}
