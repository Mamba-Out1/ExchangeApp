package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.service.LocationService
import javax.inject.Inject

/**
 * 距离计算结果。
 *
 * 同时携带原始距离（米）与可读的格式化字符串，便于物品列表既能基于数值排序/筛选，
 * 又能直接展示给用户。
 *
 * @property distanceInMeters 用户位置与物品发布位置之间的距离，单位为米。
 * @property formattedDistance 经 [LocationService.formatDistance] 格式化后的可读字符串。
 */
data class DistanceResult(
    val distanceInMeters: Double,
    val formattedDistance: String
)

/**
 * 计算用户与物品发布位置之间距离的 Use Case。
 *
 * 封装单一业务逻辑：调用 [LocationService] 计算用户当前位置与物品发布位置之间的距离，
 * 并返回既包含原始数值（米）又包含格式化展示字符串的 [DistanceResult]，以支持在物品列表中
 * 同时进行距离排序与距离信息展示。
 *
 * 设计说明：
 * - 调用方可显式提供用户位置；当不提供时，本 Use Case 会通过
 *   [LocationService.getCurrentLocation] 尝试获取当前位置。
 * - 当物品位置为 null，或用户位置不可用（未授权/定位失败导致为 null）时，
 *   无法得出有意义的距离，返回 [Result.success] 包装的 null，由调用方决定如何展示
 *   （例如隐藏距离信息）。
 * - 计算过程中发生异常时，失败信息通过 [Result.failure] 向上传播。
 *
 * @property locationService 位置服务，负责获取当前位置、计算距离与格式化距离。
 *
 * **验证需求: Requirements 15.3, 15.4**
 */
class CalculateDistanceUseCase @Inject constructor(
    private val locationService: LocationService
) {

    /**
     * 计算用户与物品发布位置之间的距离。
     *
     * @param itemLocation 物品发布位置，可为 null（无位置信息时无法计算距离）。
     * @param userLocation 用户位置，可为 null；为 null 时将尝试通过
     *        [LocationService.getCurrentLocation] 获取当前位置。
     * @return 成功时返回 [Result.success]：当用户位置与物品位置均可用时为携带数值与格式化
     *         字符串的 [DistanceResult]，否则为 null；计算失败时返回 [Result.failure]。
     */
    suspend operator fun invoke(
        itemLocation: Location?,
        userLocation: Location? = null
    ): Result<DistanceResult?> = runCatching {
        if (itemLocation == null) {
            return@runCatching null
        }

        val resolvedUserLocation = userLocation ?: locationService.getCurrentLocation()
            ?: return@runCatching null

        val distanceInMeters = locationService.calculateDistance(resolvedUserLocation, itemLocation)
        DistanceResult(
            distanceInMeters = distanceInMeters,
            formattedDistance = locationService.formatDistance(distanceInMeters)
        )
    }
}
