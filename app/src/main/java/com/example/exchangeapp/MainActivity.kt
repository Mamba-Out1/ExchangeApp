package com.example.exchangeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.exchangeapp.ui.navigation.BottomNavigationBar
import com.example.exchangeapp.ui.navigation.ExchangeNavHost
import com.example.exchangeapp.ui.navigation.Routes
import com.example.exchangeapp.ui.screen.login.LoginViewModel
import com.example.exchangeapp.ui.theme.ExchangeAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExchangeAppTheme {
                CampusExchangeApp()
            }
        }
    }
}

@Composable
fun CampusExchangeApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 检查当前是否在主标签页中
    val isMainTab = currentRoute?.let { route ->
        Routes.mainTabs.contains(route) || route.startsWith(Routes.ITEM_DETAIL) || 
        route.startsWith(Routes.CHAT)
    } ?: false
    
    // 简单的登录状态检查（实际应用中应该从ViewModel或SharedPreferences获取）
    val isLoggedIn = remember { false } // 临时值，实际应该从持久化存储获取
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // 仅当用户已登录且在主要标签页时显示底部导航栏
            if (isMainTab && isLoggedIn) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        ExchangeNavHost(
            navController = navController,
            modifier = Modifier.fillMaxSize(),
            startDestination = Routes.LOGIN
        )
    }
}