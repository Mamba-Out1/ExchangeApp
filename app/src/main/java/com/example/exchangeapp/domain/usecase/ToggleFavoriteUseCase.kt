package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.repository.UserInteractionRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import javax.inject.Inject

/**
 * 切换物品收藏状态的 Use Case。
 *
 * 封装单一业务逻辑：为当前登录用户切换指定物品的收藏状态。
 * 如果用户尚未登录，则操作会安全地失败并返回错误。
 *
 * 需求说明：
 * - 需求 10.1: WHEN User点击收藏按钮, THE App SHALL将Item添加到Favorite_List
 * - 需求 10.2: WHEN Item已在Favorite_List中且User点击收藏按钮, THE App SHALL从Favorite_List中移除该Item
 * - 需求 10.4: WHEN User添加或移除收藏, THE Storage_Module SHALL立即保存Favorite_List
 *
 * 设计说明：
 * - 调用方（如 ViewModel）只需提供物品ID，无需关心当前用户获取和状态切换逻辑。
 * - 本 Use Case 会自动检查当前用户的登录状态，未登录时操作失败。
 * - 会根据当前收藏状态自动切换：如果已收藏则取消，未收藏则添加。
 * - 操作结果会立即持久化到存储模块（通过 UserInteractionRepository）。
 * - 以 [Result] 形式返回操作结果，包含切换后的收藏状态。
 *
 * @property userInteractionRepository 用户交互记录仓库，负责收藏状态的读取和保存。
 * @property currentUserProvider 当前用户提供者，用于获取当前登录用户ID。
 *
 * **验证需求: Requirements 10.1, 10.2, 10.4**
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val userInteractionRepository: UserInteractionRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    /**
     * 切换当前登录用户对指定物品的收藏状态。
     *
     * @param itemId 要切换收藏状态的物品ID。
     * @return 成功时返回 [Result.success] 包装的切换结果 [ToggleFavoriteResult]，
     *         包含切换后的收藏状态；失败时（如用户未登录）返回 [Result.failure]。
     */
    suspend operator fun invoke(itemId: String): Result<ToggleFavoriteResult> = runCatching {
        val userId = currentUserProvider.getCurrentUserId()
            ?: throw IllegalStateException("用户未登录，无法进行收藏操作")

        // 获取当前的收藏状态
        val currentInteraction = userInteractionRepository.getInteraction(userId, itemId)
        val currentIsFavorite = currentInteraction?.isFavorite ?: false

        // 切换收藏状态
        val newIsFavorite = !currentIsFavorite
        userInteractionRepository.setFavorite(itemId, newIsFavorite)

        // 返回切换结果
        ToggleFavoriteResult(
            itemId = itemId,
            isFavorite = newIsFavorite,
            wasToggled = true,
            previousState = currentIsFavorite
        )
    }
}

/**
 * 切换收藏操作的结果。
 *
 * @param itemId 操作的目标物品ID。
 * @param isFavorite 切换后的收藏状态（true = 已收藏，false = 未收藏）。
 * @param wasToggled 是否成功进行了切换操作（总是为true，除非遇到错误）。
 * @param previousState 切换前的收藏状态。
 */
data class ToggleFavoriteResult(
    val itemId: String,
    val isFavorite: Boolean,
    val wasToggled: Boolean,
    val previousState: Boolean
)