package com.example.exchangeapp.domain.service

import com.example.exchangeapp.domain.model.Location

/**
 * 位置服务接口
 *
 * 负责获取用户当前位置、计算两个位置之间的距离以及格式化距离显示。
 * 所有方法均基于领域模型 [Location] (latitude/longitude/address)，
 * 以便与下游 Use Case (如 CalculateDistanceUseCase) 和 RecommendationEngine 对齐。
 *
 * **验证需求: Requirements 15.1, 15.2, 15.3, 15.4, 15.7**
 */
interface LocationService {
    /**
     * 获取用户当前位置。
     *
     * 需要 ACCESS_FINE_LOCATION 或 ACCESS_COARSE_LOCATION 权限。
     *
     * @return 当前位置的领域模型 [Location]；当权限未授予时回退为默认校区位置
     *   ([Location.DEFAULT_CAMPUS]，Requirement 15.6)；当定位失败或无可用位置时返回 null。
     */
    suspend fun getCurrentLocation(): Location?

    /**
     * 计算两个位置之间的距离。
     *
     * @param loc1 位置1
     * @param loc2 位置2
     * @return 两点之间的距离，单位为米。
     */
    fun calculateDistance(loc1: Location, loc2: Location): Double

    /**
     * 将距离格式化为可读字符串。
     *
     * @param distanceInMeters 距离，单位为米。
     * @return 小于1000米时格式化为 "XX米"，否则格式化为 "XX.X公里"。
     */
    fun formatDistance(distanceInMeters: Double): String
}
