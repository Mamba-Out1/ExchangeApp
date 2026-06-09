package com.example.exchangeapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ItemStatus {
    AVAILABLE,    // 可用
    RESERVED,     // 已预订
    EXCHANGED,    // 已交换
    DELETED       // 已删除
}
