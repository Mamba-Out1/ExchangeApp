package com.example.exchangeapp.ui.screen.profile

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import com.example.exchangeapp.domain.model.User
import com.example.exchangeapp.domain.model.UserInteraction
import com.example.exchangeapp.domain.model.UserInteractions
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.repository.OrderRepository
import com.example.exchangeapp.domain.repository.UserInteractionRepository
import com.example.exchangeapp.domain.repository.UserRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.usecase.DeleteItemUseCase
import com.example.exchangeapp.domain.usecase.GetItemDetailsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ProfileViewModel单元测试
 *
 * **验证需求: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7**
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var mockCurrentUserProvider: CurrentUserProvider
    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockItemRepository: ItemRepository
    private lateinit var mockUserInteractionRepository: UserInteractionRepository
    private lateinit var mockOrderRepository: OrderRepository
    private lateinit var mockDeleteItemUseCase: DeleteItemUseCase
    private lateinit var mockGetItemDetailsUseCase: GetItemDetailsUseCase
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockCurrentUserProvider = mockk()
        mockUserRepository = mockk()
        mockItemRepository = mockk()
        mockUserInteractionRepository = mockk()
        mockOrderRepository = mockk()
        mockDeleteItemUseCase = mockk()
        mockGetItemDetailsUseCase = mockk()
        // 注意：ViewModel在每个测试中按需创建。
        // ProfileViewModel的init{}会立即调用loadProfileData()，因此必须在配置好mock桩之后再构造，
        // 否则init阶段会命中未打桩的mock。
    }

    /**
     * 在配置好mock桩之后创建ViewModel。
     * 由于ProfileViewModel在init{}中触发数据加载，必须在每个测试设置好桩之后调用。
     */
    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            mockCurrentUserProvider,
            mockUserRepository,
            mockItemRepository,
            mockUserInteractionRepository,
            mockOrderRepository,
            mockDeleteItemUseCase,
            mockGetItemDetailsUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `未登录时应显示错误状态`() = runTest {
        // Given: 用户未登录
        every { mockCurrentUserProvider.getCurrentUserId() } returns null

        // When: ViewModel初始化（自动调用loadProfileData）
        viewModel = createViewModel()

        // Then: 所有状态都应为错误状态
        assertTrue(viewModel.userState.value is UserState.Error)
        assertTrue(viewModel.publishedItemsState.value is PublishedItemsState.Error)
        assertTrue(viewModel.favoriteItemsState.value is FavoriteItemsState.Error)
        assertTrue(viewModel.orderCountState.value is OrderCountState.Error)
    }

    @Test
    fun `已登录时应加载用户数据`() = runTest {
        // Given: 用户已登录，有用户数据
        val userId = "user-123"
        val testUser = User(
            id = userId,
            phone = "13800138000",
            nickname = "测试用户",
            passwordHash = null,
            avatar = "avatar_url",
            campusLocation = "默认校区",
            createdAt = 1000L
        )
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns userId
        coEvery { mockUserRepository.getUserById(userId) } returns testUser
        coEvery { mockItemRepository.getItemsByUserId(userId) } returns emptyList()
        coEvery { mockUserInteractionRepository.getUserInteractions(userId) } returns UserInteractions.EMPTY
        coEvery { mockOrderRepository.getOrdersByUserId(userId) } returns emptyList()

        // When: ViewModel初始化（自动调用loadProfileData）
        viewModel = createViewModel()

        // Then: 用户状态应为成功状态
        assertTrue(viewModel.userState.value is UserState.Success)
        val successState = viewModel.userState.value as UserState.Success
        assertEquals(testUser, successState.user)
    }

    @Test
    fun `应加载用户发布的物品列表`() = runTest {
        // Given: 用户已登录，有发布物品
        val userId = "user-123"
        val testItems = listOf(
            Item(
                id = "item-1",
                userId = userId,
                name = "测试物品1",
                description = "测试描述1",
                estimatedPrice = 100.0,
                images = listOf("url1"),
                tags = listOf("电子产品"),
                status = com.example.exchangeapp.domain.model.ItemStatus.AVAILABLE,
                location = com.example.exchangeapp.domain.model.Location(31.23, 121.47, null),
                createdAt = 1000L,
                updatedAt = 1000L
            ),
            Item(
                id = "item-2",
                userId = userId,
                name = "测试物品2",
                description = "测试描述2",
                estimatedPrice = 50.0,
                images = listOf("url2"),
                tags = listOf("书籍"),
                status = com.example.exchangeapp.domain.model.ItemStatus.AVAILABLE,
                location = com.example.exchangeapp.domain.model.Location(31.24, 121.48, null),
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns userId
        coEvery { mockUserRepository.getUserById(userId) } returns mockk()
        coEvery { mockItemRepository.getItemsByUserId(userId) } returns testItems
        coEvery { mockUserInteractionRepository.getUserInteractions(userId) } returns UserInteractions.EMPTY
        coEvery { mockOrderRepository.getOrdersByUserId(userId) } returns emptyList()

        // When: ViewModel初始化
        viewModel = createViewModel()

        // Then: 发布物品状态应为成功状态
        assertTrue(viewModel.publishedItemsState.value is PublishedItemsState.Success)
        val successState = viewModel.publishedItemsState.value as PublishedItemsState.Success
        assertEquals(testItems, successState.items)
    }

    @Test
    fun `应加载用户收藏的物品列表`() = runTest {
        // Given: 用户已登录，有收藏物品
        val userId = "user-123"
        val favoriteItemId = "item-fav-1"
        val favoriteItem = Item(
            id = favoriteItemId,
            userId = "other-user",
            name = "收藏物品",
            description = "收藏描述",
            estimatedPrice = 200.0,
            images = listOf("fav-url"),
            tags = listOf("电子产品"),
            status = com.example.exchangeapp.domain.model.ItemStatus.AVAILABLE,
            location = com.example.exchangeapp.domain.model.Location(31.25, 121.49, null),
            createdAt = 3000L,
            updatedAt = 3000L
        )
        
        val userInteractions = UserInteractions(
            listOf(
                UserInteraction(
                    userId = userId,
                    itemId = favoriteItemId,
                    clickCount = 1,
                    isFavorite = true,
                    lastInteractionTime = 4000L
                )
            )
        )
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns userId
        coEvery { mockUserRepository.getUserById(userId) } returns mockk()
        coEvery { mockItemRepository.getItemsByUserId(userId) } returns emptyList()
        coEvery { mockUserInteractionRepository.getUserInteractions(userId) } returns userInteractions
        coEvery { mockGetItemDetailsUseCase(favoriteItemId) } returns Result.success(favoriteItem)
        coEvery { mockOrderRepository.getOrdersByUserId(userId) } returns emptyList()

        // When: ViewModel初始化
        viewModel = createViewModel()

        // Then: 收藏物品状态应为成功状态
        assertTrue(viewModel.favoriteItemsState.value is FavoriteItemsState.Success)
        val successState = viewModel.favoriteItemsState.value as FavoriteItemsState.Success
        assertEquals(listOf(favoriteItem), successState.items)
    }

    @Test
    fun `应计算已完成订单数量`() = runTest {
        // Given: 用户已登录，有订单记录
        val userId = "user-123"
        val testOrders = listOf(
            Order(
                id = "order-1",
                item1Id = "item-1",
                item2Id = "item-2",
                user1Id = userId,
                user2Id = "user-2",
                status = OrderStatus.COMPLETED,
                createdAt = 1000L,
                updatedAt = 2000L,
                completedAt = 2000L
            ),
            Order(
                id = "order-2",
                item1Id = "item-3",
                item2Id = "item-4",
                user1Id = "user-3",
                user2Id = userId,
                status = OrderStatus.PENDING,
                createdAt = 3000L,
                updatedAt = 3000L,
                completedAt = null
            ),
            Order(
                id = "order-3",
                item1Id = "item-5",
                item2Id = "item-6",
                user1Id = userId,
                user2Id = "user-4",
                status = OrderStatus.COMPLETED,
                createdAt = 4000L,
                updatedAt = 5000L,
                completedAt = 5000L
            )
        )
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns userId
        coEvery { mockUserRepository.getUserById(userId) } returns mockk()
        coEvery { mockItemRepository.getItemsByUserId(userId) } returns emptyList()
        coEvery { mockUserInteractionRepository.getUserInteractions(userId) } returns UserInteractions.EMPTY
        coEvery { mockOrderRepository.getOrdersByUserId(userId) } returns testOrders

        // When: ViewModel初始化
        viewModel = createViewModel()

        // Then: 订单数量状态应为成功状态，且数量为2（两个COMPLETED订单）
        assertTrue(viewModel.orderCountState.value is OrderCountState.Success)
        val successState = viewModel.orderCountState.value as OrderCountState.Success
        assertEquals(2, successState.count)
    }

    @Test
    fun `删除物品应调用DeleteItemUseCase`() = runTest {
        // Given: 用户已登录，要删除物品
        val userId = "user-123"
        val itemId = "item-to-delete"
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns userId
        coEvery { mockDeleteItemUseCase(itemId) } returns Result.success(Unit)
        coEvery { mockItemRepository.getItemsByUserId(userId) } returns emptyList()

        // When: 调用deleteItem
        viewModel = createViewModel()
        viewModel.deleteItem(itemId)

        // Then: 应调用DeleteItemUseCase
        coVerify { mockDeleteItemUseCase(itemId) }
    }

    @Test
    fun `编辑物品应触发导航状态`() = runTest {
        // Given: 要编辑物品
        val itemId = "item-to-edit"
        // init会调用loadProfileData，提供默认桩使其走未登录分支（不触发仓库调用）
        every { mockCurrentUserProvider.getCurrentUserId() } returns null

        // When: 调用editItem
        viewModel = createViewModel()
        viewModel.editItem(itemId)

        // Then: 操作状态应为NavigateToEdit
        assertTrue(viewModel.actionState.value is ActionState.NavigateToEdit)
        val navigateState = viewModel.actionState.value as ActionState.NavigateToEdit
        assertEquals(itemId, navigateState.itemId)
    }

    @Test
    fun `查看收藏物品应触发导航状态`() = runTest {
        // Given: 要查看收藏物品
        val itemId = "item-to-view"
        // init会调用loadProfileData，提供默认桩使其走未登录分支（不触发仓库调用）
        every { mockCurrentUserProvider.getCurrentUserId() } returns null

        // When: 调用viewFavoriteItem
        viewModel = createViewModel()
        viewModel.viewFavoriteItem(itemId)

        // Then: 操作状态应为NavigateToItemDetail
        assertTrue(viewModel.actionState.value is ActionState.NavigateToItemDetail)
        val navigateState = viewModel.actionState.value as ActionState.NavigateToItemDetail
        assertEquals(itemId, navigateState.itemId)
    }
}