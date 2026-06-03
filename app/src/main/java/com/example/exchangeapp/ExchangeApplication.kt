package com.example.exchangeapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Campus Exchange App.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class ExchangeApplication : Application()
