package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.local.dao.UserDao
import com.example.exchangeapp.data.local.entity.UserEntity
import com.example.exchangeapp.domain.model.User
import com.example.exchangeapp.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户仓库实现类。
 *
 * 基于 Room 的 [UserDao] 完成用户的本地持久化，并负责 [UserEntity] 与领域模型
 * [User] 之间的相互转换。
 *
 * Requirements: 7.2（用户信息管理）、11.3 / 11.4（数据本地持久化与读写）。
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)?.toModel()
    }

    override suspend fun getUserByPhone(phone: String): User? {
        return userDao.getUserByPhone(phone)?.toModel()
    }

    override suspend fun insertUser(user: User) {
        userDao.insertUser(user.toEntity())
    }

    override suspend fun updateUser(user: User) {
        userDao.updateUser(user.toEntity())
    }

    // region 转换逻辑（Entity <-> Model）

    /**
     * 将领域模型 [User] 转换为持久化实体 [UserEntity]。
     */
    private fun User.toEntity(): UserEntity {
        return UserEntity(
            id = id,
            phone = phone,
            nickname = nickname,
            avatar = avatar,
            campusLocation = campusLocation,
            createdAt = createdAt
        )
    }

    /**
     * 将持久化实体 [UserEntity] 转换为领域模型 [User]。
     */
    private fun UserEntity.toModel(): User {
        return User(
            id = id,
            phone = phone,
            nickname = nickname,
            avatar = avatar,
            campusLocation = campusLocation,
            createdAt = createdAt
        )
    }

    // endregion
}
