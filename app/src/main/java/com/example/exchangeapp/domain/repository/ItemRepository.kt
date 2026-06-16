package com.example.exchangeapp.domain.repository

import com.example.exchangeapp.domain.model.Item

/**
 * 物品仓库接口。
 *
 * 负责物品数据的读取与持久化，提供物品的 CRUD、搜索与按标签过滤能力。
 *
 * Requirements: 5.1（物品发布与管理）、10.1（数据本地持久化）。
 */
interface ItemRepository {
    /**
     * 获取全部可推荐 / 可交换的物品列表（状态为可用）。
     */
    suspend fun getAllItems(): List<Item>

    /**
     * 根据物品ID获取单个物品，不存在时返回 null。
     */
    suspend fun getItemById(itemId: String): Item?

    /**
     * 获取指定用户发布的全部物品。
     */
    suspend fun getItemsByUserId(userId: String): List<Item>

    /**
     * 新增物品；若已存在相同ID则替换。
     */
    suspend fun insertItem(item: Item)

    /**
     * 更新已存在的物品信息。
     */
    suspend fun updateItem(item: Item)

    /**
     * 根据物品ID删除物品。
     */
    suspend fun deleteItem(itemId: String)

    /**
     * 按标签搜索可用物品。
     */
    suspend fun getItemsByTag(tag: String): List<Item>
}
