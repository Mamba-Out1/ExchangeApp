package com.example.exchangeapp.ui.screen.order

import com.example.exchangeapp.domain.model.Order
import com.example.exchangeapp.domain.model.OrderStatus
import com.example.exchangeapp.domain.repository.OrderRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var viewModel: OrderListViewModel
    private lateinit var mockOrderRepository: OrderRepository
    private lateinit var mockCurrentUserProvider: CurrentUserProvider

    private val testUserId = "test-user-1"
    private val testOrderId = "test-order-1"
    private val testCounterpartUserId = "test-user-2"
    
    private val testOrders = listOf(
        Order(
            id = "order-1",
            item1Id = "item-1",
            item2Id = "item-2",
            user1Id = testUserId,
            user2Id = testCounterpartUserId,
            status = OrderStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            completedAt = null
        ),
        Order(
            id = "order-2",
            item1Id = "item-3",
            item2Id = "item-4",
            user1Id = testCounterpartUserId,
            user2Id = testUserId,
            status = OrderStatus.IN_PROGRESS,
            createdAt = System.currentTimeMillis() - 1000,
            updatedAt = System.currentTimeMillis() - 500,
            completedAt = null
        ),
        Order(
            id = "order-3",
            item1Id = "item-5",
            item2Id = "item-6",
            user1Id = testUserId,
            user2Id = "test-user-3",
            status = OrderStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - 2000,
            updatedAt = System.currentTimeMillis() - 1000,
            completedAt = System.currentTimeMillis() - 800
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockOrderRepository = mockk()
        mockCurrentUserProvider = mockk()
        
        // 配置默认的用户登录状态
        every { mockCurrentUserProvider.getCurrentUserId() } returns testUserId
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始化时应加载用户订单`() = testScope.runTest {
        // Given: mock成功获取用户订单
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns testOrders
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // Then: 应处于成功状态
        val state = viewModel.ordersState.value
        assertTrue("初始状态应该是成功", state is OrdersState.Success)
        
        val successState = state as OrdersState.Success
        assertEquals("应该返回所有测试订单", testOrders.size, successState.orders.size)
        
        // Then: 订单应该按更新时间降序排序
        assertTrue("订单应按更新时间降序排序", 
            successState.orders[0].updatedAt >= successState.orders[1].updatedAt &&
            successState.orders[1].updatedAt >= successState.orders[2].updatedAt
        )
        
        // Then: 刷新状态应为空闲
        assertEquals("刷新状态应为空闲", RefreshState.Idle, viewModel.refreshState.value)
        
        // Then: 操作状态应为空闲
        assertEquals("操作状态应为空闲", OperationState.Idle, viewModel.operationState.value)
    }

    @Test
    fun `用户未登录时应显示错误`() = testScope.runTest {
        // Given: 用户未登录
        every { mockCurrentUserProvider.getCurrentUserId() } returns null
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // Then: 应处于错误状态
        val state = viewModel.ordersState.value
        assertTrue("用户未登录时应显示错误", state is OrdersState.Error)
        
        val errorState = state as OrdersState.Error
        assertTrue("错误消息应包含登录提示", errorState.message.contains("登录"))
    }

    @Test
    fun `refresh应重新加载订单`() = testScope.runTest {
        // Given: mock成功获取用户订单
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns testOrders
        
        // When: 创建ViewModel并调用refresh
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // When: 调用refresh
        viewModel.refresh()
        
        // 等待刷新完成
        advanceUntilIdle()
        
        // Then: 刷新状态应为成功
        assertEquals("刷新状态应为成功", RefreshState.Success, viewModel.refreshState.value)
        
        // Then: 订单状态仍应为成功
        assertTrue("刷新后订单状态应为成功", viewModel.ordersState.value is OrdersState.Success)
    }

    @Test
    fun `确认订单应更新订单状态`() = testScope.runTest {
        // Given: mock成功获取用户订单
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns testOrders
        
        // Given: mock更新订单成功
        coEvery { mockOrderRepository.updateOrder(any()) } returns Unit
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // When: 确认待确认的订单
        viewModel.confirmOrder("order-1")
        
        // 等待操作完成
        advanceUntilIdle()
        
        // Then: 操作状态应为成功
        val operationState = viewModel.operationState.value
        assertTrue("确认订单后操作状态应为成功", operationState is OperationState.Success)
        
        val successState = operationState as OperationState.Success
        assertTrue("成功消息应包含确认信息", successState.message.contains("确认"))
    }

    @Test
    fun `取消订单应更新订单状态`() = testScope.runTest {
        // Given: mock成功获取用户订单
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns testOrders
        
        // Given: mock更新订单成功
        coEvery { mockOrderRepository.updateOrder(any()) } returns Unit
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // When: 取消待确认的订单
        viewModel.cancelOrder("order-1")
        
        // 等待操作完成
        advanceUntilIdle()
        
        // Then: 操作状态应为成功
        val operationState = viewModel.operationState.value
        assertTrue("取消订单后操作状态应为成功", operationState is OperationState.Success)
        
        val successState = operationState as OperationState.Success
        assertTrue("成功消息应包含取消信息", successState.message.contains("取消"))
    }

    @Test
    fun `getOrderById应返回正确的订单`() = testScope.runTest {
        // Given: mock成功获取用户订单
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns testOrders
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // When: 获取订单详情
        val order = viewModel.getOrderById("order-1")
        
        // Then: 应返回正确的订单
        assertNotNull("getOrderById应返回订单", order)
        assertEquals("应返回正确的订单ID", "order-1", order?.id)
        assertEquals("订单状态应为PENDING", OrderStatus.PENDING, order?.status)
    }

    @Test
    fun `getCounterpartContactInfo应为进行中订单返回联系方式`() = testScope.runTest {
        // Given: mock成功获取用户订单
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns testOrders
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // When: 获取进行中订单的联系方式
        val contactInfo = viewModel.getCounterpartContactInfo("order-2")
        
        // Then: 应返回联系方式（占位值）
        assertNotNull("进行中订单应返回联系方式", contactInfo)
        assertEquals("应返回占位联系方式", "138****1234", contactInfo)
    }

    @Test
    fun `getCounterpartContactInfo对于非进行中订单应返回null`() = testScope.runTest {
        // Given: mock成功获取用户订单
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns testOrders
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // When: 获取非进行中订单的联系方式
        val contactInfo = viewModel.getCounterpartContactInfo("order-1") // PENDING状态
        
        // Then: 应返回null
        assertEquals("非进行中订单应返回null", null, contactInfo)
    }

    @Test
    fun `用户无权确认订单时应显示错误`() = testScope.runTest {
        // 创建一个用户无权限的订单
        val unauthorizedOrder = Order(
            id = "order-unauthorized",
            item1Id = "item-1",
            item2Id = "item-2",
            user1Id = "other-user-1", // 不是当前用户
            user2Id = "other-user-2", // 也不是当前用户
            status = OrderStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            completedAt = null
        )
        
        // Given: mock获取包含无权限订单的列表
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns listOf(unauthorizedOrder)
        
        // Given: mock更新订单（应不会被调用）
        coEvery { mockOrderRepository.updateOrder(any()) } returns Unit
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // When: 尝试确认无权限的订单
        viewModel.confirmOrder("order-unauthorized")
        
        // 等待操作完成
        advanceUntilIdle()
        
        // Then: 操作状态应为错误
        val operationState = viewModel.operationState.value
        assertTrue("无权限确认订单时应显示错误", operationState is OperationState.Error)
        
        val errorState = operationState as OperationState.Error
        assertTrue("错误消息应包含权限提示", errorState.message.contains("无权"))
    }

    @Test
    fun `确认非待确认订单时应显示错误`() = testScope.runTest {
        // Given: mock成功获取用户订单
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns testOrders
        
        // Given: mock更新订单（应不会被调用）
        coEvery { mockOrderRepository.updateOrder(any()) } returns Unit
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // When: 尝试确认非待确认的订单（进行中订单）
        viewModel.confirmOrder("order-2") // IN_PROGRESS状态
        
        // 等待操作完成
        advanceUntilIdle()
        
        // Then: 操作状态应为错误
        val operationState = viewModel.operationState.value
        assertTrue("确认非待确认订单时应显示错误", operationState is OperationState.Error)
        
        val errorState = operationState as OperationState.Error
        assertTrue("错误消息应包含无法确认提示", errorState.message.contains("无法确认"))
    }

    @Test
    fun `空订单列表时应显示Empty状态`() = testScope.runTest {
        // Given: mock返回空列表
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns emptyList()
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // Then: 应处于空状态
        val state = viewModel.ordersState.value
        assertTrue("空订单列表时应显示Empty状态", state is OrdersState.Empty)
    }

    @Test
    fun `重置状态应工作正常`() = testScope.runTest {
        // Given: mock成功获取用户订单
        coEvery { mockOrderRepository.getOrdersByUserId(testUserId) } returns testOrders
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // 等待初始加载完成
        advanceUntilIdle()
        
        // When: 重置状态
        viewModel.resetRefreshState()
        viewModel.resetOperationState()
        
        // Then: 刷新状态应为空闲
        assertEquals("重置后刷新状态应为空闲", RefreshState.Idle, viewModel.refreshState.value)
        
        // Then: 操作状态应为空闲
        assertEquals("重置后操作状态应为空闲", OperationState.Idle, viewModel.operationState.value)
    }

    @Test
    fun `isUserLoggedIn应返回正确状态`() {
        // Given: mock用户登录状态
        every { mockCurrentUserProvider.getCurrentUserId() } returns testUserId
        
        // When: 创建ViewModel
        viewModel = OrderListViewModel(
            mockOrderRepository,
            mockCurrentUserProvider
        )
        
        // Then: 用户应显示为已登录
        assertTrue("用户应显示为已登录", viewModel.isUserLoggedIn())
    }
}
