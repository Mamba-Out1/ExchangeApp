package com.example.exchangeapp.domain.service

/**
 * 当前登录用户的提供者。
 *
 * 部分仓库接口（如 [com.example.exchangeapp.domain.repository.UserInteractionRepository]
 * 的 incrementClickCount / setFavorite）以物品维度暴露操作，但底层持久化按
 * (userId, itemId) 维度存储，因此需要在数据层解析「当前用户」。
 *
 * 该抽象将「当前用户是谁」与具体的会话 / 登录实现解耦，方便后续接入真正的登录态。
 */
interface CurrentUserProvider {
    /**
     * 返回当前登录用户的 ID；尚未登录时返回 null。
     */
    fun getCurrentUserId(): String?
}
