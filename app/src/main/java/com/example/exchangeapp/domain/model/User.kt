package com.example.exchangeapp.domain.model

data class User(
    val id: String,
    val phone: String,
    val nickname: String,
    val avatar: String?,
    val campusLocation: String, // 默认校区
    val createdAt: Long
)
