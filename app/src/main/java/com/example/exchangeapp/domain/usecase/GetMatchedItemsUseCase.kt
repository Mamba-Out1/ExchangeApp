package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.matching.MatchingSystem
import com.example.exchangeapp.domain.model.MatchedItem
import javax.inject.Inject

/**
 * 获取"以物换物"匹配物品的 Use Case。
 *
 * 封装单一业务逻辑：当用户查看某个 Item 详情时，调用 [MatchingSystem] 计算并返回
 * 与该源物品匹配度最高的其他物品列表。匹配系统内部基于标签相似度与描述关键词相似度
 * 计算每个候选物品的 Matching_Score，并已保证结果按分数降序排列，因此本 Use Case
 * 主要负责对匹配系统调用进行封装，并以 [Result] 形式向上层（如 ViewModel）传播
 * 成功结果或失败信息。
 *
 * 设计说明：
 * - 调用方只需提供源物品ID与可选的返回数量上限，无需关心评分细节。
 * - 当匹配计算失败时（如数据读取异常），失败信息通过 [Result.failure] 向上传播，
 *   调用方可据此进行降级处理（如展示空状态或提示）。
 *
 * @property matchingSystem 匹配系统，负责计算匹配分数并按分数降序返回物品。
 *
 * **验证需求: Requirements 4.5, 4.8**
 */
class GetMatchedItemsUseCase @Inject constructor(
    private val matchingSystem: MatchingSystem
) {

    /**
     * 获取与源物品匹配的物品列表。
     *
     * @param sourceItemId 源物品ID，作为匹配基准（通常为用户当前查看详情的 Item）。
     * @param limit 返回的最大匹配物品数量，默认 5。
     * @return 成功时返回 [Result.success] 包装的、按 Matching_Score 降序排列的匹配物品列表；
     *         计算失败时返回 [Result.failure]，由调用方决定降级处理。
     */
    suspend operator fun invoke(
        sourceItemId: String,
        limit: Int = 5
    ): Result<List<MatchedItem>> = runCatching {
        matchingSystem.getMatchedItems(
            sourceItemId = sourceItemId,
            limit = limit
        )
    }
}
