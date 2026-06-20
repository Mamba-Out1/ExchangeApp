package com.example.exchangeapp.domain.usecase

import android.util.Base64
import com.example.exchangeapp.data.repository.AIRepository
import com.example.exchangeapp.domain.model.ItemRecognitionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * RecognizeItemImageUseCase 单元测试。
 *
 * 验证：
 * - 图片字节被 Base64 编码后传递给 AIRepository（Requirement 1.1）
 * - 仓库成功结果被原样返回
 * - 仓库失败结果被向上传播（Requirement 1.6，支持降级到手动输入）
 * - 空图片输入返回失败且不调用仓库
 *
 * **验证需求: Requirements 1.1, 1.6**
 */
class RecognizeItemImageUseCaseTest {

    private lateinit var aiRepository: AIRepository
    private lateinit var useCase: RecognizeItemImageUseCase

    @BeforeEach
    fun setup() {
        aiRepository = mockk()
        useCase = RecognizeItemImageUseCase(aiRepository)

        // android.util.Base64 在 JVM 单元测试中不可用，使用 java.util.Base64 模拟其行为
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            val bytes = firstArg<ByteArray>()
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `invoke should base64 encode image bytes and pass to repository`() = runTest {
        // Given
        val imageBytes = "hello-image".toByteArray()
        val expectedBase64 = java.util.Base64.getEncoder().encodeToString(imageBytes)
        val recognition = ItemRecognitionResult(
            name = "iPhone 13",
            description = "一部功能完好的手机",
            estimatedPrice = 4500.0,
            tags = listOf("电子产品")
        )
        val base64Slot = slot<String>()
        coEvery { aiRepository.recognizeItem(capture(base64Slot)) } returns Result.success(recognition)

        // When
        val result = useCase(imageBytes)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(recognition, result.getOrNull())
        assertEquals(expectedBase64, base64Slot.captured)
        coVerify(exactly = 1) { aiRepository.recognizeItem(expectedBase64) }
    }

    @Test
    fun `invoke should propagate repository failure for fallback handling`() = runTest {
        // Given
        val imageBytes = "some-bytes".toByteArray()
        val expectedError = IllegalStateException("AI识别失败")
        coEvery { aiRepository.recognizeItem(any()) } returns Result.failure(expectedError)

        // When
        val result = useCase(imageBytes)

        // Then
        assertTrue(result.isFailure)
        assertSame(expectedError, result.exceptionOrNull())
    }

    @Test
    fun `invoke should return failure and skip repository when image bytes are empty`() = runTest {
        // Given
        val emptyBytes = ByteArray(0)

        // When
        val result = useCase(emptyBytes)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { aiRepository.recognizeItem(any()) }
    }
}
