package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.model.User
import com.example.exchangeapp.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 用户注册 Use Case。
 *
 * 封装单一业务逻辑：根据注册表单输入创建新的用户账户。
 *
 * 业务流程：
 * - 调用方（如 RegisterViewModel）在完成表单验证后，提供手机号、昵称、密码哈希、
 *   默认校区等注册信息并调用本 Use Case。
 * - 本 Use Case 委托 [UserRepository.createUser] 完成业务处理：仓库会先校验手机号
 *   唯一性，若手机号已被注册则返回描述性错误（“手机号已注册”），否则生成新的用户ID
 *   并将新用户持久化到本地存储。
 * - 注册成功时以 [Result.success] 返回新建的 [User]；手机号已存在或持久化失败时以
 *   [Result.failure] 返回携带描述性错误的失败结果，调用方可据此提示用户。
 *
 * 设计说明：
 * - 本 Use Case 不直接接触持久化细节（ID 生成、唯一性校验、存储写入均由仓库负责），
 *   仅作为领域层入口封装“注册”这一业务动作，保持调用方与数据层解耦。
 *
 * @property userRepository 用户仓库，负责用户的唯一性校验与持久化。
 *
 * **验证需求: Requirements 11.4, 2.1**
 */
class RegisterUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    /**
     * 注册（创建）一个新用户账户。
     *
     * @param phone 手机号，作为登录与唯一性校验的依据。
     * @param nickname 用户昵称。
     * @param passwordHash 密码哈希值，null 表示未设置密码。
     * @param campusLocation 默认校区。
     * @param avatar 头像地址，可为 null。
     * @return 注册成功时返回 [Result.success] 包装的新建 [User]；手机号已注册或持久化
     *         失败时返回携带描述性错误的 [Result.failure]。
     */
    suspend operator fun invoke(
        phone: String,
        nickname: String,
        passwordHash: String?,
        campusLocation: String,
        avatar: String? = null
    ): Result<User> {
        return userRepository.createUser(
            phone = phone,
            nickname = nickname,
            passwordHash = passwordHash,
            avatar = avatar,
            campusLocation = campusLocation
        )
    }
}
