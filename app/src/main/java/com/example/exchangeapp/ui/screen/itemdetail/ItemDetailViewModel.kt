package com.example.exchangeapp.ui.screen.itemdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.service.LocationService
import com.example.exchangeapp.domain.usecase.CalculateDistanceUseCase
import com.example.exchangeapp.domain.usecase.GetItemDetailsUseCase
import com.example.exchangeapp.domain.usecase.GetMatchedItemsUseCase
import com.example.exchangeapp.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 物品详情界面的ViewModel
 *
 * 获取物品详情(调用GetItemDetailsUseCase)
 * 获取匹配物品(调用GetMatchedItemsUseCase)
 * 处理收藏状态切换
 * 处理联系卖家操作
 *
 * **Validates: Requirements 4.5, 5.3, 10.3**
 *
 * Requirements:
 * - 4.5: 在物品详情界面显示匹配物品推荐
 * - 5.3: 提供物品详情界面
 * - 10.3: 物品详情界面提供Favorite按钮
 */
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val getItemDetailsUseCase: GetItemDetailsUseCase,
    private val getMatchedItemsUseCase: GetMatchedItemsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val calculateDistanceUseCase: CalculateDistanceUseCase,
    private val currentUserProvider: CurrentUserProvider,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    private var currentItemId: String? = null
    private var userLocation: Location? = null

    /**
     * 加载物品详情
     *
     * @param itemId 物品ID
     */
    fun loadItemDetails(itemId: String) {
        currentItemId = itemId
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // 获取用户当前位置
                userLocation = locationService.getCurrentLocation()
                
                // 获取物品详情
                val itemResult = getItemDetailsUseCase(itemId)
                if (itemResult.isSuccess) {
                    val item = itemResult.getOrThrow()
                    
                    // 更新物品详情
                    _uiState.update { it.copy(item = item, isLoading = false) }
                    
                    // 计算距离
                    val distance = calculateDistance(item.location, userLocation)
                    
                    // 获取匹配物品
                    loadMatchedItems(item)
                    
                    // 更新UI状态（包含距离）
                    _uiState.update { it.copy(
                        item = item,
                        distance = distance,
                        isLoading = false
                    ) }
                } else {
                    _uiState.update { it.copy(
                        error = itemResult.exceptionOrNull()?.message ?: "加载失败",
                        isLoading = false
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "加载失败: ${e.message ?: "未知错误"}",
                    isLoading = false
                ) }
            }
        }
    }

    /**
     * 加载匹配物品
     *
     * @param item 当前物品
     */
    private fun loadMatchedItems(item: Item) {
        viewModelScope.launch {
            try {
                val matchedItemsResult = getMatchedItemsUseCase(item.id)
                if (matchedItemsResult.isSuccess) {
                    val matchedItems = matchedItemsResult.getOrThrow().map { it.item }
                    _uiState.update { it.copy(matchedItems = matchedItems) }
                }
                // 不处理失败情况，因为匹配物品是可选的
            } catch (e: Exception) {
                // 静默处理，匹配物品是可选的
            }
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite() {
        val itemId = currentItemId ?: return
        
        viewModelScope.launch {
            try {
                val result = toggleFavoriteUseCase(itemId)
                if (result.isSuccess) {
                    val isFavorite = result.getOrThrow().isFavorite
                    // 更新UI状态中的收藏状态
                    _uiState.update { it.copy(isFavorite = isFavorite) }
                }
                // 不处理失败情况，收藏状态将在下次加载时同步
            } catch (e: Exception) {
                // 静默处理
            }
        }
    }

    /**
     * 计算物品距离
     *
     * @param itemLocation 物品位置
     * @param userLocation 用户位置
     * @return 格式化后的距离字符串，或null（如果无法计算）
     */
    private suspend fun calculateDistance(
        itemLocation: Location?,
        userLocation: Location?
    ): String? {
        if (itemLocation == null || userLocation == null) {
            return null
        }

        val result = calculateDistanceUseCase(itemLocation, userLocation)
        return if (result.isSuccess) {
            result.getOrThrow()?.formattedDistance
        } else {
            null
        }
    }

    /**
     * 格式化距离
     *
     * @param distanceInMeters 距离（米）
     * @return 格式化后的距离字符串
     */
    private fun formatDistance(distanceInMeters: Float): String {
        return if (distanceInMeters < 1000) {
            "${distanceInMeters.toInt()}米"
        } else {
            val kilometers = distanceInMeters / 1000
            String.format("%.1f公里", kilometers)
        }
    }

    /**
     * 重置错误状态
     */
    fun resetError() {
        _uiState.update { it.copy(error = null) }
    }
}