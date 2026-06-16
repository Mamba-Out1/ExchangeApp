package com.example.exchangeapp.domain.service

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 当前登录用户提供者的持久化实现
 * 
 * 使用SharedPreferences存储登录状态，支持应用重启后自动登录
 * 
 * **Validates: Requirements 11.4, 11.7**
 * 
 * Requirements:
 * - 11.4: 登录成功保存Login_State到Storage_Module
 * - 11.7: Login_State有效时自动登录User
 */
@Singleton
class CurrentUserProviderImpl @Inject constructor(
    private val context: Context
) : CurrentUserProvider {
    
    companion object {
        private const val PREFS_NAME = "exchange_app_prefs"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
        private const val KEY_LOGIN_EXPIRY_HOURS = "login_expiry_hours"
        
        // 默认登录有效期为7天
        private const val DEFAULT_LOGIN_EXPIRY_HOURS = 168L // 7 * 24小时
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 获取当前用户ID
     * 
     * 检查登录状态是否有效，如果过期则返回null
     * 
     * @return 当前用户ID，如果没有登录用户或登录状态过期则返回null
     */
    override fun getCurrentUserId(): String? {
        val userId = prefs.getString(KEY_CURRENT_USER_ID, null)
        val loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
        
        if (userId == null || loginTimestamp == 0L) {
            return null
        }
        
        // 检查登录状态是否过期
        if (isLoginExpired(loginTimestamp)) {
            // 登录状态已过期，清除登录状态
            clearCurrentUser()
            return null
        }
        
        return userId
    }
    
    /**
     * 设置当前用户ID并保存登录状态
     * 
     * @param userId 要设置为当前用户的ID
     */
    override fun setCurrentUserId(userId: String?) {
        val editor = prefs.edit()
        
        if (userId != null) {
            // 保存用户ID和登录时间戳
            editor.putString(KEY_CURRENT_USER_ID, userId)
            editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
            editor.putLong(KEY_LOGIN_EXPIRY_HOURS, DEFAULT_LOGIN_EXPIRY_HOURS)
        } else {
            // 清除登录状态
            editor.remove(KEY_CURRENT_USER_ID)
            editor.remove(KEY_LOGIN_TIMESTAMP)
            editor.remove(KEY_LOGIN_EXPIRY_HOURS)
        }
        
        editor.apply()
    }
    
    /**
     * 清除当前用户（用于登出操作）
     */
    override fun clearCurrentUser() {
        val editor = prefs.edit()
        editor.remove(KEY_CURRENT_USER_ID)
        editor.remove(KEY_LOGIN_TIMESTAMP)
        editor.remove(KEY_LOGIN_EXPIRY_HOURS)
        editor.apply()
    }
    
    /**
     * 检查登录状态是否过期
     * 
     * @param loginTimestamp 登录时间戳
     * @return true如果登录状态已过期，false如果仍然有效
     */
    private fun isLoginExpired(loginTimestamp: Long): Boolean {
        val expiryHours = prefs.getLong(KEY_LOGIN_EXPIRY_HOURS, DEFAULT_LOGIN_EXPIRY_HOURS)
        val expiryMillis = expiryHours * 60 * 60 * 1000 // 转换为毫秒
        val currentTime = System.currentTimeMillis()
        
        return (currentTime - loginTimestamp) > expiryMillis
    }
    
    /**
     * 设置登录过期时间（用于测试或特殊场景）
     * 
     * @param expiryHours 过期时间（小时）
     */
    fun setLoginExpiryHours(expiryHours: Long) {
        prefs.edit()
            .putLong(KEY_LOGIN_EXPIRY_HOURS, expiryHours)
            .apply()
    }
    
    /**
     * 获取登录时间戳
     * 
     * @return 登录时间戳，如果没有登录则返回0
     */
    fun getLoginTimestamp(): Long {
        return prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
    }
    
    /**
     * 检查是否有有效的登录状态
     * 
     * @return true如果存在有效的登录状态，false否则
     */
    override fun hasValidLogin(): Boolean {
        return getCurrentUserId() != null
    }
}