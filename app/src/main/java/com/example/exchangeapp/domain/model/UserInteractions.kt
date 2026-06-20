package com.example.exchangeapp.domain.model

/**
 * 某个用户全部交互记录的聚合视图。
 *
 * 将一组 [UserInteraction] 按 itemId 建立索引，方便推荐引擎在计算
 * Recommendation_Score 时快速查询某个物品的点击次数与收藏状态。
 *
 * 当某个物品没有任何交互记录时，[getClickCount] 返回 0，[isFavorite] 返回 false。
 */
data class UserInteractions(
    val interactions: List<UserInteraction>
) {
    private val byItemId: Map<String, UserInteraction> =
        interactions.associateBy { it.itemId }

    /**
     * 返回指定物品的点击次数，没有记录时返回 0。
     */
    fun getClickCount(itemId: String): Int = byItemId[itemId]?.clickCount ?: 0

    /**
     * 返回指定物品是否被收藏，没有记录时返回 false。
     */
    fun isFavorite(itemId: String): Boolean = byItemId[itemId]?.isFavorite ?: false

    companion object {
        /** 无任何交互记录的空集合。 */
        val EMPTY = UserInteractions(emptyList())
    }
}
