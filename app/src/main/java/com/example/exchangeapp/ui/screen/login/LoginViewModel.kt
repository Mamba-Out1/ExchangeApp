package com.example.exchangeapp.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.User
import com.example.exchangeapp.domain.repository.UserRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录界面的ViewModel
 *
 * 处理用户登录逻辑，管理登录状态(StateFlow)，调用UserRepository验证凭证
 *
 * **Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7**
 *
 * Requirements:
 * - 11.1: 提供登录界面
 * - 11.2: 支持手机号码和密码登录
 * - 11.3: 验证凭证
 * - 11.4: 登录成功保存Login_State到Storage_Module
 * - 11.5: 登录失败显示错误提示信息
 * - 11.6: 在3秒内完成登录验证
 * - 11.7: Login_State有效时自动登录User
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    // 登录状态
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // 表单数据
    private val _phoneNumber = MutableStateFlow("")
    private val _password = MutableStateFlow("")

    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()
    val password: StateFlow<String> = _password.asStateFlow()

    /**
     * 更新手机号
     */
    fun updatePhoneNumber(newPhone: String) {
        _phoneNumber.value = newPhone
    }

    /**
     * 更新密码
     */
    fun updatePassword(newPassword: String) {
        _password.value = newPassword
    }

    /**
     * 执行登录操作
     *
     * 验证手机号和密码，调用UserRepository验证凭证
     * 遵循要求11.6: 在3秒内完成登录验证
     */
    fun login() {
        // 验证输入
        val validationResult = validateInput()
        if (!validationResult.isValid) {
            _loginState.value = LoginState.Error(validationResult.errorMessage)
            return
        }

        // 设置加载状态
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                // 使用withTimeout确保3秒内完成验证(要求11.6)
                val result = kotlinx.coroutines.withTimeout(3000L) {
                    performLogin(_phoneNumber.value, _password.value)
                }

                when (result) {
                    is LoginResult.Success -> {
                        // 登录成功，保存登录状态(要求11.4)
                        currentUserProvider.setCurrentUserId(result.userId)
                        
                        // 更新状态为成功
                        _loginState.value = LoginState.Success
                    }
                    is LoginResult.Error -> {
                        // 登录失败，显示错误提示(要求11.5)
                        _loginState.value = LoginState.Error(result.message)
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // 超时处理
                _loginState.value = LoginState.Error("登录验证超时，请稍后重试")
            } catch (e: Exception) {
                // 其他异常处理
                _loginState.value = LoginState.Error("登录失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 验证用户输入
     *
     * 验证手机号和密码格式
     */
    private fun validateInput(): ValidationResult {
        val phone = _phoneNumber.value.trim()
        val pwd = _password.value.trim()

        if (phone.isEmpty()) {
            return ValidationResult(false, "请输入手机号")
        }

        if (!isValidPhoneNumber(phone)) {
            return ValidationResult(false, "请输入有效的手机号")
        }

        if (pwd.isEmpty()) {
            return ValidationResult(false, "请输入密码")
        }

        if (pwd.length < 6) {
            return ValidationResult(false, "密码长度至少6位")
        }

        return ValidationResult(true, "")
    }

    /**
     * 检查手机号格式
     *
     * 简单的手机号格式验证，支持11位手机号
     */
    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
    }

    /**
     * 执行实际的登录验证
     *
     * 调用UserRepository验证用户凭证
     * 
     * 注意：在实际应用中应该使用安全的密码哈希算法（如BCrypt）
     * 这里使用简单验证用于演示目的
     */
    private suspend fun performLogin(phone: String, password: String): LoginResult {
        // 根据手机号查找用户
        val user = userRepository.getUserByPhone(phone)

        return if (user != null) {
            // 检查密码
            // 注意：这里使用简单密码验证，实际应用中应使用密码哈希
            if (isPasswordValid(user, password)) {
                LoginResult.Success(user.id)
            } else {
                LoginResult.Error("密码错误")
            }
        } else {
            // 用户不存在，显示错误信息
            LoginResult.Error("用户不存在")
        }
    }
    
    /**
     * 验证密码
     * 
     * 注意：这里使用简单验证，实际应用中应使用安全的密码哈希验证
     * 
     * @param user 用户对象
     * @param password 输入的密码
     * @return true如果密码有效，false否则
     */
    private fun isPasswordValid(user: User, password: String): Boolean {
        // 如果用户没有设置密码哈希（新用户或测试用户），允许任何密码
        if (user.passwordHash == null) {
            return true
        }
        
        // 简单密码验证：在实际应用中，这里应该比较密码哈希
        // 例如：BCrypt.verify(password, user.passwordHash)
        // 这里使用简单验证用于演示
        return user.passwordHash == simpleHash(password)
    }
    
    /**
     * 简单的密码哈希函数（仅用于演示）
     * 
     * 在实际应用中，应使用安全的密码哈希算法如BCrypt
     * 
     * @param password 明文密码
     * @return 哈希值（演示用）
     */
    private fun simpleHash(password: String): String {
        // 简单演示：实际应用中应使用安全的哈希算法
        return password.hashCode().toString()
    }

    /**
     * 重置登录状态
     */
    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    /**
     * 检查是否已有有效的登录状态
     *
     * 遵循要求11.7: Login_State有效时自动登录User
     */
    fun checkAutoLogin(): Boolean {
        return currentUserProvider.hasValidLogin()
    }
}

/**
 * 登录状态密封类
 *
 * 表示登录过程中的不同状态
 */
sealed class LoginState {
    /**
     * 空闲状态，初始状态
     */
    object Idle : LoginState()

    /**
     * 加载中状态，表示正在验证凭证
     */
    object Loading : LoginState()

    /**
     * 登录成功状态
     */
    object Success : LoginState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : LoginState()
}

/**
 * 登录结果密封类
 *
 * 表示登录操作的结果
 */
sealed class LoginResult {
    /**
     * 登录成功
     *
     * @param userId 用户ID
     */
    data class Success(val userId: String) : LoginResult()

    /**
     * 登录失败
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : LoginResult()
}

/**
 * 验证结果数据类
 *
 * @param isValid 是否有效
 * @param errorMessage 错误信息，当isValid为false时有效
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String
)