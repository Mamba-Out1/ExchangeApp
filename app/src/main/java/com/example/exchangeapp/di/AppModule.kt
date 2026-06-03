package com.example.exchangeapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies.
 * This module provides app-wide singleton instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provides the OpenAI API key from BuildConfig.
     * This is a placeholder to verify Hilt configuration.
     */
    @Provides
    @Singleton
    fun provideApiKey(): String {
        return com.example.exchangeapp.BuildConfig.OPENAI_API_KEY
    }
}
