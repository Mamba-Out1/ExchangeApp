package com.example.exchangeapp.ui.screen.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.exception.ValidationException
import com.example.exchangeapp.domain.model.RegisterFormData
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.usecase.RegisterUserUseCase
import com.example.exchangeapp.domain.validation.RegisterFormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 注册界面的ViewModel
 *
 * 管理注册表单状态(手机号、密码、确认密码、昵称)，调用 [RegisterFormValidator]
 * 验证表单并暴露字段级错误，调用 [RegisterUserUseCase] 创建新用户账户，
 * 注册成功后通过 [CurrentUserProvider] 保存 Login_State 到 Storage_Module。
 *
 * **Validates: Requirements 11.1, 11.2, 11.4, 2.1, 2.4**
 *
 * Requirements:
 * - 11.1: 提供注册界面
 * - 11.2: 支持手机号码和密码注册
 * - 11.4: 注册/登录成功保存Login_State到Storage_Module
 * - 2.1: 创建新用户账户
 * - 2.4: 注册成功后自动登录(保存登录状态)
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase,
    private val registerFormValidator: RegisterFormValidator,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    companion object {
        /** 默认校区，新注册用户的初始校区位置 */
        const val DEFAULT_CAMPUS_LOCATION = "默认校区"
    }

    // 注册状态
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    // 表单字段
    private val _phone = MutableStateFlow("")
    private val _password = MutableStateFlow("")
    private val _confirmPassword = MutableStateFlow("")
    private val _nickname = MutableStateFlow("")

    val phone: StateFlow<String> = _phone.asStateFlow()
    val password: StateFlow<String> = _password.asStateFlow()
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    // 字段级验证错误（包含验证失败的字段名："phone"、"password"、"confirmPassword"、"nickname"）
    private val _fieldErrors = MutableStateFlow<Set<String>>(emptySet())
    val fieldErrors: StateFlow<Set<String>> = _fieldErrors.asStateFlow()

    /**
     * 更新手机号
     */
    fun updatePhone(newPhone: String) {
        _phone.value = newPhone
    }

    /**
     * 更新密码
     */
    fun updatePassword(newPassword: String) {
        _password.value = newPassword
    }

    /**
     * 更新确认密码
     */
    fun updateConfirmPassword(newConfirmPassword: String) {
        _confirmPassword.value = newConfirmPassword
    }

    /**
     * 更新昵称
     */
    fun updateNickname(newNickname: String) {
        _nickname.value = newNickname
    }

    /**
     * 执行注册操作
     *
     * 流程：
     * 1. 构建 [RegisterFormData] 并通过 [RegisterFormValidator] 验证；
     * 2. 验证失败时暴露字段级错误并设置 Error 状态；
     * 3. 验证成功时调用 [RegisterUserUseCase] 创建用户；
     * 4. 创建成功时通过 [CurrentUserProvider.setCurrentUserId] 保存登录状态(要求11.4, 2.4)
     *    并设置 Success 状态；
     * 5. 创建失败时(如手机号已存在)设置 Error 状态并携带描述性错误信息。
     */
    fun register() {
        val formData = RegisterFormData(
            phone = _phone.value.trim(),
            password = _password.value,
            confirmPassword = _confirmPassword.value,
            nickname = _nickname.value.trim()
        )

        // 验证表单输入
        val validationResult = registerFormValidator.validate(formData)
        if (validationResult.isFailure) {
            val exception = validationResult.exceptionOrNull()
            val invalidFields = (exception as? ValidationException)?.missingFields ?: emptyList()
            _fieldErrors.value = invalidFields.toSet()
            _registerState.value = RegisterState.Error(
                exception?.message ?: "表单验证失败，请检查输入"
            )
            return
        }

        // 验证通过，清空字段级错误
        _fieldErrors.value = emptySet()
        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            try {
                val result = registerUserUseCase(
                    phone = formData.phone,
                    nickname = formData.nickname,
                    passwordHash = hashPassword(formData.password),
                    campusLocation = DEFAULT_CAMPUS_LOCATION
                )

                result.fold(
                    onSuccess = { user ->
                        // 注册成功，保存Login_State到Storage_Module(要求11.4, 2.4)
                        currentUserProvider.setCurrentUserId(user.id)
                        _registerState.value = RegisterState.Success
                    },
                    onFailure = { error ->
                        // 注册失败（如手机号已注册），显示描述性错误信息
                        _registerState.value = RegisterState.Error(
                            error.message ?: "注册失败，请稍后重试"
                        )
                    }
                )
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(
                    "注册失败: ${e.message ?: "未知错误"}"
                )
            }
        }
    }

    /**
     * 重置注册状态与字段级错误
     */
    fun resetState() {
        _registerState.value = RegisterState.Idle
        _fieldErrors.value = emptySet()
    }

    /**
     * 简单的密码哈希函数（仅用于演示）
     *
     * 与 LoginViewModel 保持一致，在实际应用中应使用安全的哈希算法如 BCrypt。
     *
     * @param password 明文密码
     * @return 哈希值（演示用）
     */
    private fun hashPassword(password: String): String {
        return password.hashCode().toString()
    }
}

/**
 * 注册状态密封类
 *
 * 表示注册过程中的不同状态
 */
sealed class RegisterState {
    /**
     * 空闲状态，初始状态
     */
    object Idle : RegisterState()

    /**
     * 加载中状态，表示正在创建用户账户
     */
    object Loading : RegisterState()

    /**
     * 注册成功状态
     */
    object Success : RegisterState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : RegisterState()
}
