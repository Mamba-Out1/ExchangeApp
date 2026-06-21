package com.example.exchangeapp.ui.screen.barter

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.exception.ValidationException
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.ItemFormData
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.domain.model.MatchedItem
import com.example.exchangeapp.domain.repository.ItemRepository
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.service.LocationService
import com.example.exchangeapp.domain.usecase.CreateExchangeOrderUseCase
import com.example.exchangeapp.domain.usecase.GetMatchedItemsUseCase
import com.example.exchangeapp.domain.usecase.ParseWantedItemTagsUseCase
import com.example.exchangeapp.domain.usecase.RecognizeItemImageUseCase
import com.example.exchangeapp.domain.usecase.SaveItemUseCase
import com.example.exchangeapp.domain.validation.ItemFormValidator
import com.example.exchangeapp.util.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BarterViewModel @Inject constructor(
    private val recognizeItemImageUseCase: RecognizeItemImageUseCase,
    private val parseWantedItemTagsUseCase: ParseWantedItemTagsUseCase,
    private val saveItemUseCase: SaveItemUseCase,
    private val getMatchedItemsUseCase: GetMatchedItemsUseCase,
    private val createExchangeOrderUseCase: CreateExchangeOrderUseCase,
    private val itemRepository: ItemRepository,
    private val itemFormValidator: ItemFormValidator,
    private val currentUserProvider: CurrentUserProvider,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarterUiState())
    val uiState: StateFlow<BarterUiState> = _uiState.asStateFlow()

    init {
        loadMyBarterItems()
    }

    fun updateName(value: String) = update { copy(name = value) }
    fun updateDescription(value: String) = update { copy(description = value) }
    fun updatePrice(value: String) = update { copy(price = value.filter { it.isDigit() || it == '.' }) }
    fun updateTags(value: List<String>) = update { copy(tags = cleanTags(value)) }
    fun updateWantedItemName(value: String) = update { copy(wantedItemName = value) }
    fun updateWantedTags(value: List<String>) = update { copy(wantedTags = cleanTags(value)) }

    fun addImageFromBytes(imageBytes: ByteArray) {
        if (imageBytes.isEmpty()) return
        val compressed = ImageCompressor.compress(imageBytes)
        val imageBase64 = Base64.encodeToString(compressed, Base64.NO_WRAP)
        update { copy(images = (images + imageBase64).take(9), message = null) }
        if (_uiState.value.images.size == 1) {
            recognizeUploadedItem()
        }
    }

    fun removeImage(index: Int) {
        update { copy(images = images.filterIndexed { i, _ -> i != index }) }
    }

    fun parseWantedTags() {
        val itemName = _uiState.value.wantedItemName.trim()
        if (itemName.isBlank()) {
            update { copy(message = "请先输入想要的物品") }
            return
        }

        viewModelScope.launch {
            update { copy(isParsingWantedTags = true, message = null) }
            val result = parseWantedItemTagsUseCase(itemName)
            update {
                if (result.isSuccess) {
                    copy(
                        wantedTags = cleanTags(result.getOrThrow()),
                        isParsingWantedTags = false,
                        message = "已生成想要的商品标签"
                    )
                } else {
                    copy(
                        isParsingWantedTags = false,
                        message = result.exceptionOrNull()?.message ?: "标签解析失败"
                    )
                }
            }
        }
    }

    fun postBarterItem() {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            update { copy(message = "请先登录") }
            return
        }

        val state = _uiState.value
        val price = state.price.toDoubleOrNull() ?: 0.0
        val formData = ItemFormData(
            name = state.name,
            description = state.description,
            price = price,
            images = state.images,
            tags = state.tags
        )
        val validation = itemFormValidator.validate(formData)
        if (validation.isFailure) {
            val fields = (validation.exceptionOrNull() as? ValidationException)?.missingFields.orEmpty()
            update { copy(formErrors = fields, message = "请填写商品名称、描述、估价和图片") }
            return
        }
        if (state.wantedItemName.isBlank() || state.wantedTags.isEmpty()) {
            update { copy(formErrors = listOf("wanted"), message = "请填写想要的物品并生成或添加标签") }
            return
        }

        viewModelScope.launch {
            update { copy(isPosting = true, message = null) }
            val now = System.currentTimeMillis()
            val item = Item(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = state.name.trim(),
                description = state.description.trim(),
                estimatedPrice = price,
                images = state.images,
                tags = cleanTags(state.tags),
                location = locationService.getCurrentLocation(),
                status = ItemStatus.AVAILABLE,
                createdAt = now,
                updatedAt = now,
                wantedItemName = state.wantedItemName.trim(),
                wantedTags = cleanTags(state.wantedTags)
            )
            val saveResult = saveItemUseCase(item)
            if (saveResult.isSuccess) {
                update {
                    copy(
                        name = "",
                        description = "",
                        price = "",
                        images = emptyList(),
                        tags = emptyList(),
                        wantedItemName = "",
                        wantedTags = emptyList(),
                        formErrors = emptyList(),
                        isPosting = false,
                        selectedItemId = item.id,
                        isShowingMatchResults = true,
                        message = "易物商品已发布，正在为你匹配"
                    )
                }
                loadMyBarterItems()
                loadMatches(item.id)
            } else {
                update {
                    copy(
                        isPosting = false,
                        message = saveResult.exceptionOrNull()?.message ?: "发布失败"
                    )
                }
            }
        }
    }

    fun loadMyBarterItems() {
        val userId = currentUserProvider.getCurrentUserId() ?: return
        viewModelScope.launch {
            update { copy(isLoadingItems = true) }
            val items = itemRepository.getItemsByUserId(userId)
                .filter { it.status == ItemStatus.AVAILABLE }
                .filter { it.wantedItemName.isNotBlank() || it.wantedTags.isNotEmpty() }
                .sortedByDescending { it.createdAt }
            val selected = _uiState.value.selectedItemId?.takeIf { id -> items.any { it.id == id } }
                ?: items.firstOrNull()?.id
            update { copy(myItems = items, selectedItemId = selected, isLoadingItems = false) }
            if (selected != null && _uiState.value.isShowingMatchResults) {
                loadMatches(selected)
            }
        }
    }

    fun selectItem(itemId: String) {
        update { copy(selectedItemId = itemId, isShowingMatchResults = true) }
        loadMatches(itemId)
    }

    fun showPostForm() {
        update { copy(isShowingMatchResults = false) }
    }

    fun requestExchange(theirItemId: String) {
        val userId = currentUserProvider.getCurrentUserId()
        val myItemId = _uiState.value.selectedItemId
        if (userId == null) {
            update { copy(message = "请先登录") }
            return
        }
        if (myItemId == null) {
            update { copy(message = "请先选择自己的易物商品") }
            return
        }
        if (myItemId == theirItemId) {
            update { copy(message = "不能和自己的同一件商品交换") }
            return
        }

        viewModelScope.launch {
            update { copy(isRequestingExchange = true, message = null) }
            val theirItem = itemRepository.getItemById(theirItemId)
            if (theirItem == null || theirItem.status != ItemStatus.AVAILABLE) {
                update { copy(isRequestingExchange = false, message = "匹配商品不存在或已下架") }
                return@launch
            }
            if (theirItem.userId == userId) {
                update { copy(isRequestingExchange = false, message = "不能向自己的商品发起交换") }
                return@launch
            }

            val result = createExchangeOrderUseCase(
                myItemId = myItemId,
                theirItemId = theirItemId,
                myUserId = userId,
                theirUserId = theirItem.userId
            )
            update {
                copy(
                    isRequestingExchange = false,
                    message = if (result.isSuccess) {
                        "交换请求已发送，等待对方确认"
                    } else {
                        result.exceptionOrNull()?.message ?: "交换请求发送失败"
                    }
                )
            }
        }
    }

    private fun recognizeUploadedItem() {
        val image = _uiState.value.images.firstOrNull() ?: return
        viewModelScope.launch {
            update { copy(isRecognizingItem = true) }
            val result = recognizeItemImageUseCase(decodeBase64Image(image))
            update {
                if (result.isSuccess) {
                    val recognized = result.getOrThrow()
                    copy(
                        name = name.ifBlank { recognized.name },
                        description = description.ifBlank { recognized.description },
                        price = if (price.isBlank()) formatPrice(recognized.estimatedPrice) else price,
                        tags = if (tags.isEmpty()) cleanTags(recognized.tags) else tags,
                        isRecognizingItem = false
                    )
                } else {
                    copy(isRecognizingItem = false, message = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    private fun loadMatches(itemId: String) {
        viewModelScope.launch {
            update { copy(isLoadingMatches = true) }
            val result = getMatchedItemsUseCase(itemId, 20)
            update {
                copy(
                    matches = result.getOrElse { emptyList() },
                    isLoadingMatches = false,
                    message = result.exceptionOrNull()?.message ?: message
                )
            }
        }
    }

    private fun decodeBase64Image(value: String): ByteArray {
        return try {
            Base64.decode(value, Base64.DEFAULT)
        } catch (e: Exception) {
            java.util.Base64.getDecoder().decode(value)
        }
    }

    private fun formatPrice(price: Double): String {
        return if (price % 1.0 == 0.0) price.toLong().toString() else price.toString()
    }

    private fun cleanTags(tags: List<String>): List<String> {
        return tags.map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun update(block: BarterUiState.() -> BarterUiState) {
        _uiState.value = _uiState.value.block()
    }
}

data class BarterUiState(
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val wantedItemName: String = "",
    val wantedTags: List<String> = emptyList(),
    val formErrors: List<String> = emptyList(),
    val isRecognizingItem: Boolean = false,
    val isParsingWantedTags: Boolean = false,
    val isPosting: Boolean = false,
    val isRequestingExchange: Boolean = false,
    val isShowingMatchResults: Boolean = false,
    val isLoadingItems: Boolean = false,
    val isLoadingMatches: Boolean = false,
    val myItems: List<Item> = emptyList(),
    val selectedItemId: String? = null,
    val matches: List<MatchedItem> = emptyList(),
    val message: String? = null
)
