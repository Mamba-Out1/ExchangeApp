package com.example.exchangeapp.domain.model

data class Order(
    val id: String,
    val item1Id: String,
    val item2Id: String,
    val user1Id: String,
    val user2Id: String,
    val status: OrderStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val rating: Int? = null
)
