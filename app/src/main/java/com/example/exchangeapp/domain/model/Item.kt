package com.example.exchangeapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val estimatedPrice: Double,
    val images: List<String>, // 图片URL列表
    val tags: List<String>,
    val location: Location?,
    val status: ItemStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val wantedItemName: String = "",
    val wantedTags: List<String> = emptyList()
)
