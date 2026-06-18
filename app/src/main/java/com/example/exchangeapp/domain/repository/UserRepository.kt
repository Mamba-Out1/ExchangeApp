package com.example.exchangeapp.domain.repository

import com.example.exchangeapp.domain.model.User

/**
 * 用户仓库接口。
 *
 * 负责用户数据的读取与持久化。
 *
 * Requirements: 7.3（用户信息管理）、10.1（数据本地持久化）、11.4 / 2.1（用户注册与本地存储）。
 */
interface UserRepository {
    /**
     * 创建（注册）新用户。
     *
     * 先通过 [getUserByPhone] 校验手机号唯一性；若该手机号已被注册，返回包含描述性
     * 错误的失败 [Result]。否则生成新的用户ID并通过 [insertUser] 将新用户保存到本地
     * 存储，返回包含新建 [User] 的成功 [Result]。
     *
     * Requirements: 11.4（注册创建新用户账户）、2.1（保存数据到本地存储）。
     *
     * @param phone 手机号，作为登录与唯一性校验的依据。
     * @param nickname 用户昵称。
     * @param passwordHash 密码哈希值，null 表示未设置密码。
     * @param avatar 头像地址，可为 null。
     * @param campusLocation 默认校区。
     * @return 成功时返回新建的 [User]；手机号已存在或持久化失败时返回失败结果。
     */
    suspend fun createUser(
        phone: String,
        nickname: String,
        passwordHash: String?,
        avatar: String?,
        campusLocation: String
    ): Result<User>

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
