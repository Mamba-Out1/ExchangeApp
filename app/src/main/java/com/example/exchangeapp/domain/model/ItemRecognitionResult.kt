package com.example.exchangeapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ItemRecognitionResult(
    val name: String,
    val description: String,
    val estimatedPrice: Double,
    val tags: List<String>
)
