package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.repository.ItemRepository
import javax.inject.Inject

/**
 * 删除物品的 Use Case。
 *
 * 封装单一业务逻辑：根据物品ID从 [ItemRepository] 中删除对应物品。本 Use Case 以
 * [Result] 形式向上层（如 ViewModel）传播成功结果或失败信息。
 *
 * 设计说明：
 * - 调用方（如已发布物品管理界面的 ViewModel）只需提供物品ID即可删除该物品。
 * - 当删除失败时（如数据写入异常），失败信息通过 [Result.failure] 向上传播，
 *   调用方可据此提示用户重试。
 *
 * @property itemRepository 物品仓库，负责物品的读取与持久化。
 *
 * **验证需求: Requirements 7.5**
 */
class DeleteItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository
) {

    /**
     * 根据物品ID删除物品。
     *
     * @param itemId 待删除的物品ID。
     * @return 成功时返回 [Result.success]；删除失败时返回 [Result.failure]。
     */
    suspend operator fun invoke(itemId: String): Result<Unit> = runCatching {
        itemRepository.deleteItem(itemId)
    }
}
