package com.example.exchangeapp.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location as AndroidLocation
import androidx.core.app.ActivityCompat
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.service.LocationService
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * [LocationService] 的实现类。
 *
 * 使用 [FusedLocationProviderClient][com.google.android.gms.location.FusedLocationProviderClient]
 * 获取设备最近一次的位置，并使用 [AndroidLocation.distanceBetween] 进行距离计算。
 *
 * @property context 应用上下文，用于权限检查和获取定位客户端。
 *
 * **验证需求: Requirements 15.1, 15.2, 15.3, 15.4, 15.7**
 */
@Singleton
class LocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationService {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * 获取用户当前位置。
     *
     * - 当位置权限未授予时回退为默认校区位置 [Location.DEFAULT_CAMPUS] (Requirement 15.6)。
     * - 当定位成功但无可用位置（lastLocation 为 null）或定位失败时返回 null。
     */
    override suspend fun getCurrentLocation(): Location? = suspendCoroutine { continuation ->
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 未授权位置权限，使用默认校区位置 (Requirement 15.6)
            continuation.resume(Location.DEFAULT_CAMPUS)
            return@suspendCoroutine
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { androidLocation: AndroidLocation? ->
                continuation.resume(androidLocation?.toDomainLocation())
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }

    /**
     * 使用 [AndroidLocation.distanceBetween] 计算两个领域 [Location] 之间的距离（米）。
     */
    override fun calculateDistance(loc1: Location, loc2: Location): Double {
        val results = FloatArray(1)
        AndroidLocation.distanceBetween(
            loc1.latitude, loc1.longitude,
            loc2.latitude, loc2.longitude,
            results
        )
        return results[0].toDouble()
    }

    /**
     * 将距离格式化为 "XX米"（小于1000米）或 "XX.X公里"（大于等于1000米）。
     */
    override fun formatDistance(distanceInMeters: Double): String {
        return if (distanceInMeters < 1000) {
            "${distanceInMeters.toInt()}米"
        } else {
            "%.1f公里".format(distanceInMeters / 1000)
        }
    }

    /**
     * 将 Android 框架的 [AndroidLocation] 映射为领域模型 [Location]。
     */
    private fun AndroidLocation.toDomainLocation(): Location {
        return Location(
            latitude = latitude,
            longitude = longitude,
            address = null
        )
    }
}
