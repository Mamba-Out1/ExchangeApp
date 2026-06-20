package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.model.User
import com.example.exchangeapp.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * RegisterUserUseCase 单元测试。
 *
 * 验证：
 * - 注册成功时返回仓库创建的新用户，并将表单输入正确透传给仓库（Requirements 11.4, 2.1）
 * - 手机号已注册等业务错误被向上传播为失败结果
 * - 持久化异常被向上传播
 *
 * **验证需求: Requirements 11.4, 2.1**
 */
class RegisterUserUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var useCase: RegisterUserUseCase

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        useCase = RegisterUserUseCase(userRepository)
    }

    @Test
    fun `invoke should create user and return new user on success`() = runTest {
        // Given
        val phone = "13800138000"
        val nickname = "小明"
        val passwordHash = "hashed-pwd"
        val campusLocation = "校区A"
        val createdUser = User(
            id = "generated-id",
            phone = phone,
            nickname = nickname,
            passwordHash = passwordHash,
            avatar = null,
            campusLocation = campusLocation,
            createdAt = 1_000L
        )
        coEvery {
            userRepository.createUser(phone, nickname, passwordHash, null, campusLocation)
        } returns Result.success(createdUser)

        // When
        val result = useCase(phone, nickname, passwordHash, campusLocation)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(createdUser, result.getOrNull())
        coVerify(exactly = 1) {
            userRepository.createUser(phone, nickname, passwordHash, null, campusLocation)
        }
    }

    @Test
    fun `invoke should propagate descriptive error when phone already registered`() = runTest {
        // Given
        val expectedError = IllegalStateException("手机号已注册")
        coEvery {
            userRepository.createUser(any(), any(), any(), any(), any())
        } returns Result.failure(expectedError)

        // When
        val result = useCase(
            phone = "13800138000",
            nickname = "小明",
            passwordHash = "hashed-pwd",
            campusLocation = "校区A"
        )

        // Then
        assertTrue(result.isFailure)
        assertSame(expectedError, result.exceptionOrNull())
        assertEquals("手机号已注册", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should pass avatar through to repository`() = runTest {
        // Given
        val avatar = "avatar.png"
        coEvery {
            userRepository.createUser(any(), any(), any(), avatar, any())
        } returns Result.success(
            User(
                id = "id",
                phone = "13800138000",
                nickname = "小明",
                passwordHash = null,
                avatar = avatar,
                campusLocation = "校区A",
                createdAt = 0L
            )
        )

        // When
        val result = useCase(
            phone = "13800138000",
            nickname = "小明",
            passwordHash = null,
            campusLocation = "校区A",
            avatar = avatar
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(avatar, result.getOrNull()?.avatar)
        coVerify(exactly = 1) {
            userRepository.createUser("13800138000", "小明", null, avatar, "校区A")
        }
    }
}
