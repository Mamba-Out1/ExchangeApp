package com.example.exchangeapp.domain.model

enum class OrderStatus {
    PENDING,      // 待确认
    IN_PROGRESS,  // 进行中
    COMPLETED,    // 已完成
    CANCELLED     // 已取消
}
