package com.example.exchangeapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.exchangeapp.data.local.SeedDataInitializer
import com.example.exchangeapp.util.CoilImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ExchangeApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var seedDataInitializer: SeedDataInitializer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seedDataInitializer.seedBarterItemsIfNeeded()
        }
    }

    override fun newImageLoader(): ImageLoader = CoilImageLoaderFactory.build(this)
}
