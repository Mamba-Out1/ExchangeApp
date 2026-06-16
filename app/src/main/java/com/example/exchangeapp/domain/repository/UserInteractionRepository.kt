package com.example.exchangeapp.domain.repository

import com.example.exchangeapp.domain.model.UserInteraction
import com.example.exchangeapp.domain.model.UserInteractions

/**
 * 用户交互记录仓库接口。
 *
 * 负责读取用户的交互记录以及在用户点击 / 收藏物品时更新对应权重，
 * 供推荐引擎计算 Recommendation_Score 使用。
 *
 * Requirements: 9.2（基于交互行为的个性化推荐）、10.1（数据本地持久化）。
 */
interface UserInteractionRepository {
    /**
     * 获取指定用户的全部交互记录聚合视图。
     */
    suspend fun getUserInteractions(userId: String): UserInteractions

    /**
     * 获取指定用户对某个物品的单条交互记录，不存在时返回 null。
     */
    suspend fun getInteraction(userId: String, itemId: String): UserInteraction?

    /**
     * 新增或更新一条交互记录。
     */
    suspend fun insertOrUpdateInteraction(interaction: UserInteraction)

    /**
     * 当用户点击某个物品时，增加该物品的点击次数（点击权重）。
     */
    suspend fun incrementClickCount(itemId: String)

    /**
     * 当用户收藏 / 取消收藏某个物品时，更新该物品的收藏状态（收藏权重）。
     */
    suspend fun setFavorite(itemId: String, isFavorite: Boolean)
}
