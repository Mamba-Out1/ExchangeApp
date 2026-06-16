package com.example.exchangeapp.domain.service

/**
 * 当前登录用户的提供者。
 *
 * 部分仓库接口（如 [com.example.exchangeapp.domain.repository.UserInteractionRepository]
 * 的 incrementClickCount / setFavorite）以物品维度暴露操作，但底层持久化按
 * (userId, itemId) 维度存储，因此需要在数据层解析「当前用户」。
 *
 * 该抽象将「当前用户是谁」与具体的会话 / 登录实现解耦，方便后续接入真正的登录态。
 *
 * **Validates: Requirements 11.4, 11.7**
 *
 * Requirements:
 * - 11.4: 登录成功保存Login_State到Storage_Module
 * - 11.7: Login_State有效时自动登录User
 */
interface CurrentUserProvider {
    /**
     * 返回当前登录用户的 ID；尚未登录时返回 null。
     */
    fun getCurrentUserId(): String?

    /**
     * 设置当前用户ID并保存登录状态。
     *
     * @param userId 要设置为当前用户的ID，null表示登出
     */
    fun setCurrentUserId(userId: String?)

    /**
     * 清除当前用户登录状态（用于登出操作）。
     */
    fun clearCurrentUser()

    /**
     * 检查是否有有效的登录状态。
     *
     * @return true如果存在有效的登录状态，false否则
     */
    fun hasValidLogin(): Boolean
}