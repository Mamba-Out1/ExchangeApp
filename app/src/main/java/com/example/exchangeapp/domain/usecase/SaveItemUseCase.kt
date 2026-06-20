package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.repository.ItemRepository
import javax.inject.Inject

/**
 * 保存（发布或更新）物品的 Use Case。
 *
 * 封装单一业务逻辑：将一个 [Item] 持久化到 [ItemRepository]。当仓库中已存在相同ID的
 * 物品时执行更新（编辑），否则执行新增（发布）。本 Use Case 以 [Result] 形式向上层
 * （如 ViewModel）传播成功结果或失败信息。
 *
 * 设计说明：
 * - 调用方（如物品发布 / 编辑界面的 ViewModel）只需构造好 [Item] 并调用本 Use Case，
 *   无需关心“新增还是更新”的判定与持久化细节。
 * - 是否为更新由物品ID是否已存在决定：通过 [ItemRepository.getItemById] 查询，
 *   存在则调用 [ItemRepository.updateItem]，否则调用 [ItemRepository.insertItem]。
 * - 当持久化失败时（如数据写入异常），失败信息通过 [Result.failure] 向上传播，
 *   调用方可据此提示用户重试。
 *
 * @property itemRepository 物品仓库，负责物品的读取与持久化。
 *
 * **验证需求: Requirements 6.1, 7.5**
 */
class SaveItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository
) {

    /**
     * 保存（发布或更新）一个物品。
     *
     * @param item 待保存的物品。当其ID在仓库中已存在时执行更新，否则执行新增。
     * @return 成功时返回 [Result.success] 包装的已保存 [Item]；持久化失败时返回 [Result.failure]。
     */
    suspend operator fun invoke(item: Item): Result<Item> = runCatching {
        val existingItem = itemRepository.getItemById(item.id)
        if (existingItem != null) {
            itemRepository.updateItem(item)
        } else {
            itemRepository.insertItem(item)
        }
        item
    }
}
