package com.example.exchangeapp.data.repository.fake

import com.example.exchangeapp.data.local.dao.ChatDao
import com.example.exchangeapp.data.local.dao.ItemDao
import com.example.exchangeapp.data.local.dao.OrderDao
import com.example.exchangeapp.data.local.dao.UserDao
import com.example.exchangeapp.data.local.dao.UserInteractionDao
import com.example.exchangeapp.data.local.entity.ChatMessageEntity
import com.example.exchangeapp.data.local.entity.ItemEntity
import com.example.exchangeapp.data.local.entity.OrderEntity
import com.example.exchangeapp.data.local.entity.UserEntity
import com.example.exchangeapp.data.local.entity.UserInteractionEntity
import com.example.exchangeapp.domain.service.CurrentUserProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * 内存版 [ItemDao] 假实现，用于在不依赖 Room / Android 运行环境的情况下对
 * Repository 层进行单元测试。
 *
 * 通过 [seed] 可直接写入原始实体（包括非法 JSON / 未知枚举字符串），
 * 用于验证转换逻辑的默认值回退；通过 [failReads] 可模拟数据读取失败。
 */
class FakeItemDao : ItemDao {
    private val store = LinkedHashMap<String, ItemEntity>()

    /** 当为 true 时，所有读取操作抛出异常，模拟「数据读取失败」(Requirement 2.8)。 */
    var failReads: Boolean = false

    fun seed(entity: ItemEntity) {
        store[entity.id] = entity
    }

    private fun guardRead() {
        if (failReads) throw RuntimeException("simulated read failure")
    }

    override suspend fun getAllAvailableItems(): List<ItemEntity> {
        guardRead()
        return store.values
            .filter { it.status == "AVAILABLE" }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun getItemById(itemId: String): ItemEntity? {
        guardRead()
        return store[itemId]
    }

    override suspend fun getItemsByUserId(userId: String): List<ItemEntity> {
        guardRead()
        return store.values.filter { it.userId == userId }
    }

    override suspend fun insertItem(item: ItemEntity) {
        store[item.id] = item
    }

    override suspend fun updateItem(item: ItemEntity) {
        store[item.id] = item
    }

    override suspend fun deleteItem(itemId: String) {
        store.remove(itemId)
    }

    override suspend fun getItemsByTag(tag: String): List<ItemEntity> {
        guardRead()
        return store.values.filter { it.status == "AVAILABLE" && it.tags.contains(tag) }
    }
}

/** 内存版 [UserDao] 假实现。 */
class FakeUserDao : UserDao {
    private val store = LinkedHashMap<String, UserEntity>()

    var failReads: Boolean = false

    fun seed(entity: UserEntity) {
        store[entity.id] = entity
    }

    private fun guardRead() {
        if (failReads) throw RuntimeException("simulated read failure")
    }

    override suspend fun getUserById(userId: String): UserEntity? {
        guardRead()
        return store[userId]
    }

    override suspend fun getUserByPhone(phone: String): UserEntity? {
        guardRead()
        return store.values.firstOrNull { it.phone == phone }
    }

    override suspend fun insertUser(user: UserEntity) {
        store[user.id] = user
    }

    override suspend fun updateUser(user: UserEntity) {
        store[user.id] = user
    }
}

/** 内存版 [ChatDao] 假实现。 */
class FakeChatDao : ChatDao {
    private val store = LinkedHashMap<String, ChatMessageEntity>()
    private val changes = MutableStateFlow(0)

    var failReads: Boolean = false
    var failInserts: Boolean = false

    fun seed(entity: ChatMessageEntity) {
        store[entity.id] = entity
        changes.value++
    }

    private fun guardRead() {
        if (failReads) throw RuntimeException("simulated read failure")
    }

    override suspend fun getMessagesByConversationId(conversationId: String): List<ChatMessageEntity> {
        guardRead()
        return store.values
            .filter { it.conversationId == conversationId }
            .sortedBy { it.timestamp }
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>> {
        return changes.map {
            store.values
                .filter { it.conversationId == conversationId }
                .sortedBy { it.timestamp }
        }
    }

    override suspend fun insertMessage(message: ChatMessageEntity) {
        if (failInserts) throw RuntimeException("simulated insert failure")
        store[message.id] = message
        changes.value++
    }

    override suspend fun markMessagesAsRead(conversationId: String) {
        store.values
            .filter { it.conversationId == conversationId }
            .forEach { store[it.id] = it.copy(isRead = true) }
        changes.value++
    }

    override fun getUnreadCount(userId: String): Flow<Int> {
        return changes.map {
            store.values.count { it.receiverId == userId && !it.isRead }
        }
    }
}

/** 内存版 [OrderDao] 假实现。 */
class FakeOrderDao : OrderDao {
    private val store = LinkedHashMap<String, OrderEntity>()

    var failReads: Boolean = false

    fun seed(entity: OrderEntity) {
        store[entity.id] = entity
    }

    private fun guardRead() {
        if (failReads) throw RuntimeException("simulated read failure")
    }

    override suspend fun getOrdersByUserId(userId: String): List<OrderEntity> {
        guardRead()
        return store.values
            .filter { it.user1Id == userId || it.user2Id == userId }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun getOrderById(orderId: String): OrderEntity? {
        guardRead()
        return store[orderId]
    }

    override suspend fun insertOrder(order: OrderEntity) {
        store[order.id] = order
    }

    override suspend fun updateOrder(order: OrderEntity) {
        store[order.id] = order
    }
}

/** 内存版 [UserInteractionDao] 假实现。 */
class FakeUserInteractionDao : UserInteractionDao {
    private val store = LinkedHashMap<Pair<String, String>, UserInteractionEntity>()

    var failReads: Boolean = false

    fun seed(entity: UserInteractionEntity) {
        store[entity.userId to entity.itemId] = entity
    }

    private fun guardRead() {
        if (failReads) throw RuntimeException("simulated read failure")
    }

    override suspend fun getUserInteractions(userId: String): List<UserInteractionEntity> {
        guardRead()
        return store.values.filter { it.userId == userId }
    }

    override suspend fun getInteraction(userId: String, itemId: String): UserInteractionEntity? {
        guardRead()
        return store[userId to itemId]
    }

    override suspend fun insertOrUpdateInteraction(interaction: UserInteractionEntity) {
        store[interaction.userId to interaction.itemId] = interaction
    }

    override suspend fun incrementClickCount(userId: String, itemId: String, timestamp: Long) {
        val key = userId to itemId
        val existing = store[key] ?: return
        store[key] = existing.copy(
            clickCount = existing.clickCount + 1,
            lastInteractionTime = timestamp
        )
    }
}

/** 可配置的 [CurrentUserProvider] 假实现。 */
class FakeCurrentUserProvider(var userId: String?) : CurrentUserProvider {
    override fun getCurrentUserId(): String? = userId
    
    override fun setCurrentUserId(userId: String?) {
        this.userId = userId
    }
    
    override fun clearCurrentUser() {
        this.userId = null
    }
    
    override fun hasValidLogin(): Boolean {
        return userId != null
    }
}
