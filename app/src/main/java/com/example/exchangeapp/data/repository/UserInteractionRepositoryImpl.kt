package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.local.dao.UserInteractionDao
import com.example.exchangeapp.data.local.entity.UserInteractionEntity
import com.example.exchangeapp.domain.model.UserInteraction
import com.example.exchangeapp.domain.model.UserInteractions
import com.example.exchangeapp.domain.repository.UserInteractionRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户交互记录仓库实现类。
 *
 * 基于 Room 的 [UserInteractionDao] 完成用户交互记录的本地持久化，并负责
 * [UserInteractionEntity] 与领域模型 [UserInteraction] 之间的相互转换。
 *
 * 说明：[incrementClickCount] 与 [setFavorite] 以物品维度暴露，但底层数据按
 * (userId, itemId) 维度存储，因此通过 [CurrentUserProvider] 解析当前登录用户；
 * 当不存在当前用户时，这两个操作将安全地忽略。
 *
 * Requirements: 3.5（点击权重）、3.6（收藏权重）、10.4（数据本地持久化与读写）。
 */
@Singleton
class UserInteractionRepositoryImpl @Inject constructor(
    private val userInteractionDao: UserInteractionDao,
    private val currentUserProvider: CurrentUserProvider
) : UserInteractionRepository {

    override suspend fun getUserInteractions(userId: String): UserInteractions {
        val interactions = userInteractionDao.getUserInteractions(userId).map { it.toModel() }
        return UserInteractions(interactions)
    }

    override suspend fun getInteraction(userId: String, itemId: String): UserInteraction? {
        return userInteractionDao.getInteraction(userId, itemId)?.toModel()
    }

    override suspend fun insertOrUpdateInteraction(interaction: UserInteraction) {
        userInteractionDao.insertOrUpdateInteraction(interaction.toEntity())
    }

    override suspend fun incrementClickCount(itemId: String) {
        val userId = currentUserProvider.getCurrentUserId() ?: return
        val now = System.currentTimeMillis()
        val existing = userInteractionDao.getInteraction(userId, itemId)
        if (existing == null) {
            // 尚无记录时，DAO 的 UPDATE 语句不会生效，因此先创建首条记录（点击次数为 1）。
            userInteractionDao.insertOrUpdateInteraction(
                UserInteractionEntity(
                    userId = userId,
                    itemId = itemId,
                    clickCount = 1,
                    isFavorite = false,
                    lastInteractionTime = now
                )
            )
        } else {
            userInteractionDao.incrementClickCount(userId, itemId, now)
        }
    }

    override suspend fun setFavorite(itemId: String, isFavorite: Boolean) {
        val userId = currentUserProvider.getCurrentUserId() ?: return
        val now = System.currentTimeMillis()
        val existing = userInteractionDao.getInteraction(userId, itemId)
        val updated = existing?.copy(
            isFavorite = isFavorite,
            lastInteractionTime = now
        ) ?: UserInteractionEntity(
            userId = userId,
            itemId = itemId,
            clickCount = 0,
            isFavorite = isFavorite,
            lastInteractionTime = now
        )
        userInteractionDao.insertOrUpdateInteraction(updated)
    }

    // region 转换逻辑（Entity <-> Model）

    /**
     * 将领域模型 [UserInteraction] 转换为持久化实体 [UserInteractionEntity]。
     */
    private fun UserInteraction.toEntity(): UserInteractionEntity {
        return UserInteractionEntity(
            userId = userId,
            itemId = itemId,
            clickCount = clickCount,
            isFavorite = isFavorite,
            lastInteractionTime = lastInteractionTime
        )
    }

    /**
     * 将持久化实体 [UserInteractionEntity] 转换为领域模型 [UserInteraction]。
     */
    private fun UserInteractionEntity.toModel(): UserInteraction {
        return UserInteraction(
            userId = userId,
            itemId = itemId,
            clickCount = clickCount,
            isFavorite = isFavorite,
            lastInteractionTime = lastInteractionTime
        )
    }

    // endregion
}
