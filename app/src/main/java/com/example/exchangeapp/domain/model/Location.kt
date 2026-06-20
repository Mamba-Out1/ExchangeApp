package com.example.exchangeapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String?
) {
    companion object {
        /**
         * 默认校区位置。
         *
         * 当用户未授权位置权限时使用此默认位置进行距离计算与推荐排序。
         *
         * **验证需求: Requirements 15.6**
         */
        val DEFAULT_CAMPUS = Location(
            latitude = 39.9075,
            longitude = 116.3972,
            address = "默认校区"
        )
    }
}
