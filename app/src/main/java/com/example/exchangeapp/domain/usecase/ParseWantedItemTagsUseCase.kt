package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.data.repository.AIRepository
import javax.inject.Inject

class ParseWantedItemTagsUseCase @Inject constructor(
    private val aiRepository: AIRepository
) {
    suspend operator fun invoke(itemName: String): Result<List<String>> {
        return aiRepository.parseWantedItemTags(itemName)
    }
}
