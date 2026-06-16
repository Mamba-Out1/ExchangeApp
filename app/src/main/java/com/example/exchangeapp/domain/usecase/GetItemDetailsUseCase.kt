package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.repository.ItemRepository
import javax.inject.Inject

/**
 * 获取物品详情的 Use Case。
 *
 * 封装单一业务逻辑：根据物品ID从 [ItemRepository] 读取单个物品的详细信息。本 Use Case
 * 以 [Result] 形式向上层（如物品详情界面的 ViewModel）传播成功结果或失败信息。
 *
 * 设计说明：
 * - 调用方只需提供物品ID即可获取该物品的完整信息，用于详情页面展示。
 * - 当物品不存在时返回 [Result.failure] 包装的 [NoSuchElementException]，
 *   由调用方决定如何处理（如提示“物品不存在或已被删除”）。
 * - 读取过程中发生异常时，失败信息通过 [Result.failure] 向上传播。
 *
 * @property itemRepository 物品仓库，负责物品的读取与持久化。
 *
 * **验证需求: Requirements 5.3**
 */
class GetItemDetailsUseCase @Inject constructor(
    private val itemRepository: ItemRepository
) {

    /**
     * 根据物品ID获取物品详情。
     *
     * @param itemId 目标物品ID。
     * @return 成功时返回 [Result.success] 包装的 [Item]；物品不存在或读取失败时返回 [Result.failure]。
     */
    suspend operator fun invoke(itemId: String): Result<Item> = runCatching {
        itemRepository.getItemById(itemId)
            ?: throw NoSuchElementException("未找到ID为 $itemId 的物品")
    }
}
