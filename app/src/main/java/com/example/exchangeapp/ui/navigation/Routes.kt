package com.example.exchangeapp.ui.navigation

/**
 * 应用路由常量定义
 */
object Routes {
    // 登录相关路由
    const val LOGIN = "login"
    const val REGISTER = "register"
    
    // 主界面路由（底部导航栏标签）
    const val HOME = "home"
    const val POST_ITEM = "postitem"
    const val BARTER = "barter"
    const val PROFILE = "profile" 
    const val ORDERS = "order"
    const val CHAT = "chat"
    const val FAVORITES = "favorites"
    
    // 详情和子页面路由
    const val ITEM_DETAIL = "itemdetail"
    const val ORDER_DETAIL = "orderdetail"
    
    // 带参数的路由模式
    const val ITEM_DETAIL_WITH_ID = "$ITEM_DETAIL/{itemId}"
    const val ORDER_DETAIL_WITH_ID = "$ORDER_DETAIL/{orderId}"
    const val CHAT_WITH_USER = "$CHAT/{userId}"
    
    // 路由分组
    val mainTabs = listOf(HOME, POST_ITEM, BARTER, PROFILE, ORDERS, CHAT, FAVORITES)
    
    // 构建带参数的路径
    fun itemDetail(itemId: String): String {
        return "$ITEM_DETAIL/$itemId"
    }
    
    fun orderDetail(orderId: String): String {
        return "$ORDER_DETAIL/$orderId"
    }
    
    fun chatWithUser(userId: String): String {
        return "$CHAT/$userId"
    }
}
