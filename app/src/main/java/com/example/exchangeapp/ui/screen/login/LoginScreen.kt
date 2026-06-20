package com.example.exchangeapp.ui.screen.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.exchangeapp.R

/**
 * 登录屏幕
 *
 * 提供用户登录界面，支持手机号和密码登录
 *
 * **Validates: Requirements 11.1, 11.2, 11.5, 11.6**
 *
 * Requirements:
 * - 11.1: 提供登录界面
 * - 11.2: 支持手机号码和密码登录
 * - 11.5: 登录失败显示错误提示信息
 * - 11.6: 在3秒内完成登录验证 (由ViewModel处理，界面仅反映Loading状态)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    loginState: LoginState,
    onLogin: (phone: String, password: String) -> Unit,
    onLoginSuccess: () -> Unit,
    onResetState: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 处理登录成功
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onLoginSuccess()
        }
    }

    // 处理错误状态：通过Snackbar展示错误信息后重置状态，
    // 这样用户可以保留已填写的表单并重新尝试登录(要求11.5)
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Error) {
            snackbarHostState.showSnackbar(loginState.message)
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
            // 始终展示登录表单，避免在加载/错误状态下丢失已填写内容
            LoginContent(
                isLoading = loginState is LoginState.Loading,
                onLogin = onLogin,
                onNavigateToRegister = onNavigateToRegister,
                modifier = Modifier.fillMaxSize()
            )

            // 加载状态时在表单之上展示遮罩与进度指示
            if (loginState is LoginState.Loading) {
                LoadingOverlay()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    isLoading: Boolean,
    onLogin: (phone: String, password: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val phoneError = phoneNumber.isNotEmpty() && !isValidPhoneNumber(phoneNumber)
    val passwordError = password.isNotEmpty() && password.length < 6
    val canSubmit = isValidPhoneNumber(phoneNumber) && password.length >= 6 && !isLoading

    Column(
        modifier = modifier
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.screen_title_login),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("手机号") },
            placeholder = { Text("请输入11位手机号") },
            singleLine = true,
            enabled = !isLoading,
            isError = phoneError,
            supportingText = {
                if (phoneError) {
                    Text("请输入有效的11位手机号")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            placeholder = { Text("请输入密码") },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError,
            supportingText = {
                if (passwordError) {
                    Text("密码至少6位")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onLogin(phoneNumber, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        ) {
            Text(text = "登录")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 注册入口：跳转到注册界面 (Requirement 11.1)
        TextButton(
            onClick = onNavigateToRegister,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "还没有账号？注册")
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

/**
 * 验证手机号格式
 */
private fun isValidPhoneNumber(phone: String): Boolean {
    // 简单的手机号验证：11位数字，以1开头
    return phone.matches(Regex("^1[0-9]{10}$"))
}
