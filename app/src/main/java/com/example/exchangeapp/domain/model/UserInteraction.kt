package com.example.exchangeapp.domain.model

data class UserInteraction(
    val userId: String,
    val itemId: String,
    val clickCount: Int,
    val isFavorite: Boolean,
    val lastInteractionTime: Long
)
