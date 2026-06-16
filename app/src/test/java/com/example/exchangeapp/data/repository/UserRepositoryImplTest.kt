package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.repository.fake.FakeUserDao
import com.example.exchangeapp.domain.model.User
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * [UserRepositoryImpl] 单元测试。
 *
 * 使用内存版假 DAO（[FakeUserDao]）验证 Entity <-> Model 转换的往返正确性，
 * 以及数据读取失败时的错误处理路径。
 *
 * **验证需求: Requirements 2.8**
 */
class UserRepositoryImplTest {

    private lateinit var dao: FakeUserDao
    private lateinit var repository: UserRepositoryImpl

    private fun sampleUser(
        id: String = "user-1",
        phone: String = "13800000000",
        avatar: String? = "avatar.png",
        passwordHash: String? = "password123".hashCode().toString()
    ) = User(
        id = id,
        phone = phone,
        nickname = "小明",
        passwordHash = passwordHash,
        avatar = avatar,
        campusLocation = "校区A",
        createdAt = 1234L
    )

    @BeforeEach
    fun setup() {
        dao = FakeUserDao()
        repository = UserRepositoryImpl(dao)
    }

    @Test
    fun `insert then get returns equal model - round trip conversion`() = runTest {
        val user = sampleUser()

        repository.insertUser(user)
        val loaded = repository.getUserById(user.id)

        assertEquals(user, loaded)
    }

    @Test
    fun `round trip preserves null avatar`() = runTest {
        val user = sampleUser(avatar = null)

        repository.insertUser(user)
        val loaded = repository.getUserById(user.id)

        assertEquals(user, loaded)
    }

    @Test
    fun `getUserByPhone returns matching user`() = runTest {
        val user = sampleUser(phone = "13900000001")
        repository.insertUser(user)

        val loaded = repository.getUserByPhone("13900000001")

        assertEquals(user, loaded)
    }

    @Test
    fun `getUserById returns null when user is missing`() = runTest {
        assertNull(repository.getUserById("missing"))
    }

    @Test
    fun `updateUser overwrites existing user`() = runTest {
        val user = sampleUser()
        repository.insertUser(user)

        val updated = user.copy(nickname = "小红")
        repository.updateUser(updated)

        assertEquals(updated, repository.getUserById(user.id))
    }

    @Test
    fun `read failure propagates as exception`() = runTest {
        dao.failReads = true

        assertFailsWith<RuntimeException> {
            repository.getUserById("user-1")
        }
    }
}
