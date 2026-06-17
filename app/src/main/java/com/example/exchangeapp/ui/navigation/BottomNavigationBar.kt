package com.example.exchangeapp.ui.navigation

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.exchangeapp.R

/**
 * 应用底部导航栏
 * 
 * 提供在主屏幕之间的导航功能
 * 
 * **Requirements: 所有界面**
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        // 定义底部导航栏项目
        val navItems = listOf(
            BottomNavItem(
                route = Routes.HOME,
                label = stringResource(R.string.bottom_nav_home),
                iconName = "home"
            ),
            BottomNavItem(
                route = Routes.POST_ITEM,
                label = stringResource(R.string.bottom_nav_post),
                iconName = "add"
            ),
            BottomNavItem(
                route = Routes.CHAT,
                label = stringResource(R.string.bottom_nav_chat),
                iconName = "chat"
            ),
            BottomNavItem(
                route = Routes.ORDERS,
                label = stringResource(R.string.bottom_nav_orders),
                iconName = "shopping_cart"
            ),
            BottomNavItem(
                route = Routes.PROFILE,
                label = stringResource(R.string.bottom_nav_profile),
                iconName = "person"
            ),
            BottomNavItem(
                route = Routes.FAVORITES,
                label = stringResource(R.string.bottom_nav_favorites),
                iconName = "favorite"
            )
        )
        
        // 渲染每个导航项
        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    // 导航到目标路由
                    navController.navigate(item.route) {
                        // 弹出到起始目标，防止堆积返回栈
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // 避免同一目标多次入栈
                        launchSingleTop = true
                        // 恢复状态
                        restoreState = true
                    }
                },
                icon = {
                    // 简化版：使用文本代替图标，避免图标依赖问题
                    Text(text = item.iconName.take(1).uppercase())
                },
                label = {
                    Text(text = item.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}

/**
 * 底部导航栏项目数据类
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val iconName: String
)

/**
 * 底部应用栏（带浮动操作按钮版本，可选）
 */
@Composable
fun BottomAppNavigationBar(
    navController: NavController,
    currentRoute: String?,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        actions = {
            // 定义除了发布按钮之外的其他导航项
            val navItems = listOf(
                BottomNavItem(
                    route = Routes.HOME,
                    label = stringResource(R.string.bottom_nav_home),
                    iconName = "home"
                ),
                BottomNavItem(
                    route = Routes.CHAT,
                    label = stringResource(R.string.bottom_nav_chat),
                    iconName = "chat"
                ),
                BottomNavItem(
                    route = Routes.ORDERS,
                    label = stringResource(R.string.bottom_nav_orders),
                    iconName = "shopping_cart"
                ),
                BottomNavItem(
                    route = Routes.PROFILE,
                    label = stringResource(R.string.bottom_nav_profile),
                    iconName = "person"
                ),
                BottomNavItem(
                    route = Routes.FAVORITES,
                    label = stringResource(R.string.bottom_nav_favorites),
                    iconName = "favorite"
                )
            )
            
            navItems.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        // 简化版：使用文本代替图标
                        Text(text = item.iconName.take(1).uppercase())
                    },
                    label = {
                        Text(text = item.label)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        floatingActionButton = {
            // 浮动操作按钮用于发布物品
            androidx.compose.material3.FloatingActionButton(
                onClick = onFabClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                // 简化版：使用文本代替图标
                Text(text = "+")
            }
        }
    )
}