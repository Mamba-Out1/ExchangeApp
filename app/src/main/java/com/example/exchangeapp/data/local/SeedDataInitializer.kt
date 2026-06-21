package com.example.exchangeapp.data.local

import com.example.exchangeapp.data.local.database.AppDatabase
import com.example.exchangeapp.data.local.entity.ItemEntity
import com.example.exchangeapp.data.local.entity.UserEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedDataInitializer @Inject constructor(
    private val database: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedBarterItemsIfNeeded() {
        val itemDao = database.itemDao()
        if (itemDao.getItemById(SEED_ITEM_PREFIX + "airpods") != null) return

        val userDao = database.userDao()
        seedUsers().forEach { userDao.insertUser(it) }
        seedItems().forEach { itemDao.insertItem(it) }
    }

    private fun seedUsers(): List<UserEntity> {
        val baseTime = 1_718_000_000_000L
        return listOf(
            UserEntity("seed-user-a", "13800000001", "林同学", null, null, "东区宿舍", baseTime),
            UserEntity("seed-user-b", "13800000002", "周同学", null, null, "图书馆", baseTime + 1_000),
            UserEntity("seed-user-c", "13800000003", "陈同学", null, null, "教学楼A", baseTime + 2_000),
            UserEntity("seed-user-d", "13800000004", "许同学", null, null, "西区操场", baseTime + 3_000),
            UserEntity("seed-user-e", "13800000005", "王同学", null, null, "实验楼", baseTime + 4_000),
            UserEntity("seed-user-f", "13800000006", "赵同学", null, null, "南区食堂", baseTime + 5_000)
        )
    }

    private fun seedItems(): List<ItemEntity> {
        val now = 1_718_100_000_000L
        return listOf(
            barterItem(
                id = "airpods",
                userId = "seed-user-a",
                name = "AirPods 二代蓝牙耳机",
                description = "正常使用，充电盒有轻微划痕，适合通勤和自习使用。",
                price = 260.0,
                tags = listOf("airpods", "earphones", "bluetooth", "apple", "audio"),
                wantedItemName = "计算器",
                wantedTags = listOf("calculator", "electronics", "study", "math", "stationery"),
                createdAt = now
            ),
            barterItem(
                id = "calculator",
                userId = "seed-user-b",
                name = "卡西欧科学计算器",
                description = "函数计算器，按键灵敏，适合高数、物理和工程课程。",
                price = 80.0,
                tags = listOf("calculator", "casio", "electronics", "study", "math"),
                wantedItemName = "蓝牙耳机",
                wantedTags = listOf("earphones", "headphones", "bluetooth", "audio"),
                createdAt = now - 10_000
            ),
            barterItem(
                id = "textbook",
                userId = "seed-user-c",
                name = "计算机网络教材",
                description = "教材保存较好，有少量笔记，适合计科和软工课程复习。",
                price = 35.0,
                tags = listOf("book", "textbook", "computer", "network", "study"),
                wantedItemName = "自行车",
                wantedTags = listOf("bicycle", "transport", "sports", "campus"),
                createdAt = now - 20_000
            ),
            barterItem(
                id = "bike",
                userId = "seed-user-d",
                name = "校园代步自行车",
                description = "车况稳定，刹车正常，适合宿舍到教学楼日常通勤。",
                price = 180.0,
                tags = listOf("bicycle", "transport", "sports", "campus"),
                wantedItemName = "教材",
                wantedTags = listOf("book", "textbook", "study", "education"),
                createdAt = now - 30_000
            ),
            barterItem(
                id = "keyboard",
                userId = "seed-user-e",
                name = "罗技无线键盘",
                description = "轻薄无线键盘，连接稳定，适合宿舍桌面和课堂记录。",
                price = 95.0,
                tags = listOf("keyboard", "logitech", "wireless", "computer", "peripheral"),
                wantedItemName = "无线鼠标",
                wantedTags = listOf("mouse", "wireless", "computer", "peripheral"),
                createdAt = now - 40_000
            ),
            barterItem(
                id = "mouse",
                userId = "seed-user-f",
                name = "无线静音鼠标",
                description = "静音按键，适合图书馆和宿舍夜间使用，带接收器。",
                price = 45.0,
                tags = listOf("mouse", "wireless", "computer", "peripheral"),
                wantedItemName = "键盘",
                wantedTags = listOf("keyboard", "wireless", "computer", "peripheral"),
                createdAt = now - 50_000
            )
        )
    }

    private fun barterItem(
        id: String,
        userId: String,
        name: String,
        description: String,
        price: Double,
        tags: List<String>,
        wantedItemName: String,
        wantedTags: List<String>,
        createdAt: Long
    ): ItemEntity {
        return ItemEntity(
            id = SEED_ITEM_PREFIX + id,
            userId = userId,
            name = name,
            description = description,
            estimatedPrice = price,
            images = json.encodeToString(emptyList<String>()),
            tags = json.encodeToString(tags),
            wantedItemName = wantedItemName,
            wantedTags = json.encodeToString(wantedTags),
            latitude = null,
            longitude = null,
            address = "示例校区",
            status = "AVAILABLE",
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }

    private companion object {
        const val SEED_ITEM_PREFIX = "seed-barter-"
    }
}
