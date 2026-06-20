package com.example.exchangeapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WantedItemTagsResult(
    val tags: List<String>
)
