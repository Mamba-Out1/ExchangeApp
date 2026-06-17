package com.example.exchangeapp.ui.screen.postitem

import com.example.exchangeapp.domain.exception.ValidationException
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.ItemRecognitionResult
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.service.LocationService
import com.example.exchangeapp.domain.usecase.RecognizeItemImageUseCase
import com.example.exchangeapp.domain.usecase.SaveItemUseCase
import com.example.exchangeapp.domain.validation.ItemFormValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PostItemViewModelTest {

    private lateinit var viewModel: PostItemViewModel
    private lateinit var mockRecognizeItemImageUseCase: RecognizeItemImageUseCase
    private lateinit var mockSaveItemUseCase: SaveItemUseCase
    private lateinit var mockItemFormValidator: ItemFormValidator
    private lateinit var mockCurrentUserProvider: CurrentUserProvider
    private lateinit var mockLocationService: LocationService
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        mockRecognizeItemImageUseCase = mockk()
        mockSaveItemUseCase = mockk()
        mockItemFormValidator = mockk()
        mockCurrentUserProvider = mockk()
        mockLocationService = mockk()
        
        viewModel = PostItemViewModel(
            recognizeItemImageUseCase = mockRecognizeItemImageUseCase,
            saveItemUseCase = mockSaveItemUseCase,
            itemFormValidator = mockItemFormValidator,
            currentUserProvider = mockCurrentUserProvider,
            locationService = mockLocationService
        )
    }

    @Test
    fun `初始状态应为空闲`() {
        // When: ViewModel创建
        // Then: 初始状态应为空闲
        assertEquals(RecognitionState.Idle, viewModel.recognitionState.value)
        assertEquals(SaveState.Idle, viewModel.saveState.value)
        assertTrue(viewModel.formErrors.value.isEmpty())
    }

    @Test
    fun `更新表单字段应更新状态`() = runTest {
        viewModel.updateName("测试物品")
        viewModel.name.test {
            assertEquals("测试物品", awaitItem())
        }

        viewModel.updateDescription("测试描述")
        viewModel.description.test {
            assertEquals("测试描述", awaitItem())
        }

        viewModel.updatePrice(100.0)
        viewModel.price.test {
            assertEquals(100.0, awaitItem())
        }

        viewModel.updateTags(listOf("电子产品", "二手"))
        viewModel.tags.test {
            assertEquals(listOf("电子产品", "二手"), awaitItem())
        }
    }

    @Test
    fun `添加图片应更新图片列表`() = runTest {
        val imageBase64 = "base64image1"
        
        viewModel.addImage(imageBase64)
        viewModel.images.test {
            assertEquals(listOf(imageBase64), awaitItem())
        }

        assertTrue(viewModel.hasEnoughImages())
        assertFalse(viewModel.hasReachedImageLimit())
    }

    @Test
    fun `删除图片应更新图片列表`() = runTest {
        val imageBase64 = "base64image1"
        
        viewModel.addImage(imageBase64)
        viewModel.removeImage(0)
        
        viewModel.images.test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `当未上传图片时识别应返回错误`() = runTest {
        viewModel.recognizeItemImage()
        
        viewModel.recognitionState.test {
            val state = awaitItem()
            assertTrue(state is RecognitionState.Error)
            val errorState = state as RecognitionState.Error
            assertEquals("请先上传图片", errorState.message)
        }
    }

    @Test
    fun `当AI识别成功时应填充表单`() = runTest {
        // 设置mock
        every { mockCurrentUserProvider.getCurrentUserId() } returns "user123"
        val recognitionResult = ItemRecognitionResult(
            name = "AI识别物品",
            description = "AI识别描述",
            estimatedPrice = 500.0,
            tags = listOf("电子产品", "数码")
        )
        coEvery { mockRecognizeItemImageUseCase(any()) } returns Result.success(recognitionResult)
        
        // 添加图片
        viewModel.addImage("base64image")
        
        viewModel.recognizeItemImage()
        
        // 验证识别状态变化
        viewModel.recognitionState.test {
            assertEquals(RecognitionState.Loading, awaitItem())
            assertEquals(RecognitionState.Success, awaitItem())
        }
        
        // 验证表单被填充
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.name.test {
            assertEquals("AI识别物品", awaitItem())
        }
        viewModel.description.test {
            assertEquals("AI识别描述", awaitItem())
        }
        viewModel.price.test {
            assertEquals(500.0, awaitItem())
        }
        viewModel.tags.test {
            assertEquals(listOf("电子产品", "数码"), awaitItem())
        }
        
        assertTrue(viewModel.isAiFilled())
        
        // 验证UseCase被调用
        coVerify { mockRecognizeItemImageUseCase(any()) }
    }

    @Test
    fun `当AI识别失败时应显示错误`() = runTest {
        every { mockCurrentUserProvider.getCurrentUserId() } returns "user123"
        coEvery { mockRecognizeItemImageUseCase(any()) } returns Result.failure(Exception("网络错误"))
        
        viewModel.addImage("base64image")
        viewModel.recognizeItemImage()
        
        viewModel.recognitionState.test {
            assertEquals(RecognitionState.Loading, awaitItem())
            val errorState = awaitItem() as RecognitionState.Error
            assertTrue(errorState.message.contains("网络错误"))
        }
    }

    @Test
    fun `当表单验证失败时应设置错误状态`() = runTest {
        every { mockCurrentUserProvider.getCurrentUserId() } returns "user123"
        every { mockItemFormValidator.validate(any()) } returns Result.failure(
            ValidationException(listOf("name", "description"))
        )
        
        viewModel.postItem()
        
        viewModel.saveState.test {
            val errorState = awaitItem() as SaveState.Error
            assertEquals("请填写所有必填字段", errorState.message)
        }
        
        viewModel.formErrors.test {
            val errors = awaitItem()
            assertEquals(listOf("name", "description"), errors)
        }
        
        verify { mockItemFormValidator.validate(any()) }
    }

    @Test
    fun `当用户未登录时发布应返回错误`() = runTest {
        every { mockCurrentUserProvider.getCurrentUserId() } returns null
        
        viewModel.postItem()
        
        viewModel.saveState.test {
            val errorState = awaitItem() as SaveState.Error
            assertEquals("请先登录", errorState.message)
        }
    }

    @Test
    fun `成功发布物品应返回成功状态`() = runTest {
        val userId = "user123"
        val itemId = UUID.randomUUID().toString()
        
        every { mockCurrentUserProvider.getCurrentUserId() } returns userId
        every { mockItemFormValidator.validate(any()) } returns Result.success(Unit)
        every { mockLocationService.getCurrentLocation() } returns Location(39.9042, 116.4074, "北京")
        coEvery { mockSaveItemUseCase(any()) } returns Result.success(
            Item(
                id = itemId,
                userId = userId,
                name = "测试物品",
                description = "测试描述",
                estimatedPrice = 100.0,
                images = listOf("base64image"),
                tags = listOf("电子产品"),
                location = Location(39.9042, 116.4074, "北京"),
                status = ItemStatus.AVAILABLE,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        
        // 设置表单数据
        viewModel.updateName("测试物品")
        viewModel.updateDescription("测试描述")
        viewModel.updatePrice(100.0)
        viewModel.addImage("base64image")
        viewModel.updateTags(listOf("电子产品"))
        
        viewModel.postItem()
        
        viewModel.saveState.test {
            assertEquals(SaveState.Loading, awaitItem())
            val successState = awaitItem() as SaveState.Success
            assertEquals(itemId, successState.itemId)
        }
        
        coVerify { mockSaveItemUseCase(any()) }
    }

    @Test
    fun `重置表单应清除所有数据`() = runTest {
        // 设置表单数据
        viewModel.updateName("测试物品")
        viewModel.updateDescription("测试描述")
        viewModel.updatePrice(100.0)
        viewModel.addImage("base64image")
        viewModel.updateTags(listOf("电子产品"))
        
        // 重置表单
        viewModel.resetForm()
        
        viewModel.name.test {
            assertEquals("", awaitItem())
        }
        viewModel.description.test {
            assertEquals("", awaitItem())
        }
        viewModel.price.test {
            assertEquals(0.0, awaitItem())
        }
        viewModel.images.test {
            assertTrue(awaitItem().isEmpty())
        }
        viewModel.tags.test {
            assertTrue(awaitItem().isEmpty())
        }
        viewModel.formErrors.test {
            assertTrue(awaitItem().isEmpty())
        }
        
        assertFalse(viewModel.isAiFilled())
    }

    @Test
    fun `hasEnoughImages应正确判断图片数量`() {
        assertFalse(viewModel.hasEnoughImages())
        
        viewModel.addImage("base64image1")
        assertTrue(viewModel.hasEnoughImages())
    }

    @Test
    fun `hasReachedImageLimit应正确判断图片上限`() {
        assertFalse(viewModel.hasReachedImageLimit())
        
        // 添加9张图片
        repeat(9) { index ->
            viewModel.addImage("base64image$index")
        }
        
        assertTrue(viewModel.hasReachedImageLimit())
        
        // 尝试添加第10张图片（应该被阻止）
        viewModel.addImage("base64image10")
        // 图片数量应该仍然是9
        assertEquals(9, viewModel.images.value.size)
    }

    @Test
    fun `清除指定字段错误应正确工作`() = runTest {
        // 设置错误
        viewModel.updateName("测试")
        viewModel.formErrors.test {
            skipItems(1) // 跳过初始空列表
        }
        
        // 直接测试内部状态变化
        // 注意：由于clearFormErrors是private方法，我们通过updateName间接测试
        // updateName会调用clearFormErrors("name")
        viewModel.updateName("新名称")
        
        // 表单错误应该被清除
        viewModel.formErrors.test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `取消识别应重置状态`() = runTest {
        every { mockCurrentUserProvider.getCurrentUserId() } returns "user123"
        
        // 设置长时间运行的识别
        coEvery { mockRecognizeItemImageUseCase(any()) } coAnswers {
            delay(5000) // 模拟长时间运行
            Result.success(ItemRecognitionResult("test", "test", 100.0, emptyList()))
        }
        
        viewModel.addImage("base64image")
        viewModel.recognizeItemImage()
        
        // 立即取消
        viewModel.cancelRecognition()
        
        viewModel.recognitionState.test {
            assertEquals(RecognitionState.Loading, awaitItem())
            assertEquals(RecognitionState.Idle, awaitItem())
        }
    }
}