package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.local.dao.ItemDao
import com.example.exchangeapp.data.local.entity.ItemEntity
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.repository.ItemRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 物品仓库实现类。
 *
 * 基于 Room 的 [ItemDao] 完成物品的本地持久化，并负责 [ItemEntity] 与领域模型
 * [Item] 之间的相互转换。其中：
 * - 图片(images)与标签(tags)列表使用 JSON 字符串持久化；
 * - 物品状态(status)以枚举名称的字符串形式持久化；
 * - 位置(location)拆分为 latitude / longitude / address 三个可空字段持久化。
 *
 * Requirements: 5.1（物品发布）、5.2（物品编辑）、5.3（物品删除）、
 * 14.4 / 14.5（数据本地持久化与读写）。
 */
@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao
) : ItemRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAllItems(): List<Item> {
        return itemDao.getAllAvailableItems().map { it.toModel() }
    }

    override suspend fun getItemById(itemId: String): Item? {
        return itemDao.getItemById(itemId)?.toModel()
    }

    override suspend fun getItemsByUserId(userId: String): List<Item> {
        return itemDao.getItemsByUserId(userId).map { it.toModel() }
    }

    override suspend fun insertItem(item: Item) {
        itemDao.insertItem(item.toEntity())
    }

    override suspend fun updateItem(item: Item) {
        itemDao.updateItem(item.toEntity())
    }

    override suspend fun deleteItem(itemId: String) {
        itemDao.deleteItem(itemId)
    }

    override suspend fun getItemsByTag(tag: String): List<Item> {
        return itemDao.getItemsByTag(tag).map { it.toModel() }
    }

    // region 转换逻辑（Entity <-> Model）

    /**
     * 将领域模型 [Item] 转换为持久化实体 [ItemEntity]。
     */
    private fun Item.toEntity(): ItemEntity {
        return ItemEntity(
            id = id,
            userId = userId,
            name = name,
            description = description,
            estimatedPrice = estimatedPrice,
            images = json.encodeToString(images),
            tags = json.encodeToString(tags),
            latitude = location?.latitude,
            longitude = location?.longitude,
            address = location?.address,
            status = status.name,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * 将持久化实体 [ItemEntity] 转换为领域模型 [Item]。
     */
    private fun ItemEntity.toModel(): Item {
        return Item(
            id = id,
            userId = userId,
            name = name,
            description = description,
            estimatedPrice = estimatedPrice,
            images = decodeStringList(images),
            tags = decodeStringList(tags),
            location = toLocation(),
            status = decodeStatus(status),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * 将实体中的经纬度与地址组合为 [Location]。
     * 当经纬度均缺失时视为无位置信息，返回 null。
     */
    private fun ItemEntity.toLocation(): Location? {
        val lat = latitude
        val lng = longitude
        return if (lat != null && lng != null) {
            Location(latitude = lat, longitude = lng, address = address)
        } else {
            null
        }
    }

    /**
     * 解析 JSON 字符串为字符串列表；解析失败或空值时返回空列表。
     */
    private fun decodeStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 将字符串解析为 [ItemStatus]；无法识别时回退为 [ItemStatus.AVAILABLE]。
     */
    private fun decodeStatus(value: String): ItemStatus {
        return try {
            ItemStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ItemStatus.AVAILABLE
        }
    }

    // endregion
}
