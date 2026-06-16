package com.example.exchangeapp.domain.repository

import com.example.exchangeapp.domain.model.User

/**
 * 用户仓库接口。
 *
 * 负责用户数据的读取与持久化。
 *
 * Requirements: 7.3（用户信息管理）、10.1（数据本地持久化）。
 */
interface UserRepository {
    /**
     * 根据用户ID获取用户，不存在时返回 null。
     */
    suspend fun getUserById(userId: String): User?

    /**
     * 根据手机号获取用户，不存在时返回 null。
     */
    suspend fun getUserByPhone(phone: String): User?

    /**
     * 新增用户；若已存在相同ID则替换。
     */
    suspend fun insertUser(user: User)

    /**
     * 更新已存在的用户信息。
     */
    suspend fun updateUser(user: User)
}
