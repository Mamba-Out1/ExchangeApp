package com.example.exchangeapp.ui.screen.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.exchangeapp.R

/**
 * 注册屏幕
 *
 * 提供用户注册界面，支持手机号、密码、确认密码和昵称输入，
 * 并基于 [RegisterViewModel] 的字段级错误集合显示针对性校验提示。
 *
 * **Validates: Requirements 11.1, 11.2, 11.5**
 *
 * Requirements:
 * - 11.1: 提供注册界面
 * - 11.2: 支持手机号码和密码注册
 * - 11.5: 注册失败显示错误提示信息
 *
 * @param registerState 当前注册状态，由ViewModel的StateFlow提供
 * @param fieldErrors 验证失败的字段名集合（"phone"、"password"、"confirmPassword"、"nickname"）
 * @param onRegister 触发注册操作的回调，传入表单四个字段
 * @param onRegisterSuccess 注册成功后的导航回调（跳转主界面，具体路由由导航层处理）
 * @param onResetState 重置注册状态的回调，用于在展示错误后恢复表单可用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    registerState: RegisterState,
    fieldErrors: Set<String>,
    onRegister: (phone: String, password: String, confirmPassword: String, nickname: String) -> Unit,
    onRegisterSuccess: () -> Unit,
    onResetState: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 处理注册成功：触发导航回调跳转主界面
    LaunchedEffect(registerState) {
        if (registerState is RegisterState.Success) {
            onRegisterSuccess()
        }
    }

    // 处理错误状态：通过Snackbar展示错误信息后重置状态，
    // 保留用户已填写的表单并允许重新尝试注册(要求11.5)
    LaunchedEffect(registerState) {
        if (registerState is RegisterState.Error) {
            snackbarHostState.showSnackbar(registerState.message)
            onResetState()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 始终展示注册表单，避免在加载/错误状态下丢失已填写内容
            RegisterContent(
                isLoading = registerState is RegisterState.Loading,
                fieldErrors = fieldErrors,
                onRegister = onRegister,
                modifier = Modifier.fillMaxSize()
            )

            // 加载状态时在表单之上展示遮罩与进度指示
            if (registerState is RegisterState.Loading) {
                LoadingOverlay()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterContent(
    isLoading: Boolean,
    fieldErrors: Set<String>,
    onRegister: (phone: String, password: String, confirmPassword: String, nickname: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    // 字段级错误：来源于ViewModel上一次验证结果(要求11.5)
    val phoneError = fieldErrors.contains("phone")
    val passwordError = fieldErrors.contains("password")
    val confirmPasswordError = fieldErrors.contains("confirmPassword")
    val nicknameError = fieldErrors.contains("nickname")

    // 仅在四个字段均非空且非加载状态时允许提交，详细校验由ViewModel处理
    val canSubmit = phone.isNotBlank() &&
        password.isNotBlank() &&
        confirmPassword.isNotBlank() &&
        nickname.isNotBlank() &&
        !isLoading

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.screen_title_register),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 手机号
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("手机号") },
            placeholder = { Text("请输入11位手机号") },
            singleLine = true,
            enabled = !isLoading,
            isError = phoneError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            supportingText = {
                if (phoneError) {
                    Text("请输入有效的11位手机号")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 密码
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            placeholder = { Text("至少6位，需包含字母和数字") },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = passwordError,
            supportingText = {
                if (passwordError) {
                    Text("密码至少6位，且需同时包含字母和数字")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 确认密码
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("确认密码") },
            placeholder = { Text("请再次输入密码") },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = confirmPasswordError,
            supportingText = {
                if (confirmPasswordError) {
                    Text("两次输入的密码不一致")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 昵称
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("昵称") },
            placeholder = { Text("请输入昵称") },
            singleLine = true,
            enabled = !isLoading,
            isError = nicknameError,
            supportingText = {
                if (nicknameError) {
                    Text("请输入昵称")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onRegister(phone, password, confirmPassword, nickname) },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        ) {
            Text(text = "注册")
        }
    }
}

/**
 * 加载状态遮罩
 */
@Composable
private fun LoadingOverlay(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.message_loading))
            }
        }
    }
}
