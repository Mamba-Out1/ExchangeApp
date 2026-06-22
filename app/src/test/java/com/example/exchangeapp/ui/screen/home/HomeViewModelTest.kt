package com.example.exchangeapp.ui.screen.home

import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.recommendation.RecommendationEngine
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.service.LocationService
import com.example.exchangeapp.domain.usecase.GetRecommendedItemsUseCase
import com.example.exchangeapp.domain.usecase.ToggleFavoriteResult
import com.example.exchangeapp.domain.usecase.ToggleFavoriteUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var getRecommendedItemsUseCase: GetRecommendedItemsUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var recommendationEngine: RecommendationEngine
    private lateinit var itemRepository: ItemRepository
    private lateinit var currentUserProvider: CurrentUserProvider
    private lateinit var locationService: LocationService

    private val userId = "test-user"
    private val location = Location(39.9042, 116.4074, "Beijing")
    private val items = listOf(
        item("item-1", "mouse", listOf("mouse", "computer")),
        item("item-2", "calculator", listOf("calculator", "study")),
        item("item-3", "book", listOf("book", "study"))
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getRecommendedItemsUseCase = mockk()
        toggleFavoriteUseCase = mockk()
        recommendationEngine = mockk(relaxed = true)
        itemRepository = mockk()
        currentUserProvider = mockk()
        locationService = mockk()

        every { currentUserProvider.getCurrentUserId() } returns userId
        coEvery { locationService.getCurrentLocation() } returns location
        coEvery { itemRepository.getAllItems() } returns items
        coEvery { getRecommendedItemsUseCase(any(), any(), any()) } returns Result.success(items.take(2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load publishes all items and recommendations`() = testScope.runTest {
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.itemsState.value
        assertTrue(state is ItemsState.Success)
        state as ItemsState.Success
        assertEquals(items, state.items)
        assertEquals(items.take(2), state.recommendedItems)
        assertEquals(LoadMoreState.Idle, viewModel.loadMoreState.value)
        assertEquals(RefreshState.Idle, viewModel.refreshState.value)
    }

    @Test
    fun `not logged in shows error`() = testScope.runTest {
        every { currentUserProvider.getCurrentUserId() } returns null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.itemsState.value is ItemsState.Error)
    }

    @Test
    fun `refresh reloads items and recommendations`() = testScope.runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(RefreshState.Success, viewModel.refreshState.value)
        assertTrue(viewModel.itemsState.value is ItemsState.Success)
    }

    @Test
    fun `empty all items and empty recommendations shows empty state`() = testScope.runTest {
        coEvery { itemRepository.getAllItems() } returns emptyList()
        coEvery { getRecommendedItemsUseCase(any(), any(), any()) } returns Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.itemsState.value is ItemsState.Empty)
    }

    @Test
    fun `toggle favorite refreshes recommendations`() = testScope.runTest {
        coEvery { toggleFavoriteUseCase("item-1") } returns Result.success(
            ToggleFavoriteResult(
                itemId = "item-1",
                isFavorite = true,
                wasToggled = true,
                previousState = false
            )
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite("item-1")
        advanceUntilIdle()

        assertTrue(viewModel.itemsState.value is ItemsState.Success)
    }

    @Test
    fun `login state delegates to current user provider`() {
        val viewModel = createViewModel()

        assertTrue(viewModel.isUserLoggedIn())
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getRecommendedItemsUseCase = getRecommendedItemsUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            recommendationEngine = recommendationEngine,
            itemRepository = itemRepository,
            currentUserProvider = currentUserProvider,
            locationService = locationService
        )
    }

    private fun item(id: String, name: String, tags: List<String>): Item {
        return Item(
            id = id,
            userId = "owner-$id",
            name = name,
            description = "$name description",
            estimatedPrice = 10.0,
            images = emptyList(),
            tags = tags,
            location = null,
            status = ItemStatus.AVAILABLE,
            createdAt = 0L,
            updatedAt = 0L
        )
    }
}
