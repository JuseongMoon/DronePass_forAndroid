package com.ScienceFiction.DronePassAndroid.core.location

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
)

/** 기기 위치 원천. 테스트에서는 호출 수를 세는 가짜로 바꾼다. */
interface DeviceLocationSource {
    /** 현재 위치, 없으면 마지막으로 알려진 위치. 권한이 없으면 [SecurityException]. */
    suspend fun currentOrLastKnown(): DeviceLocation?
}

@Singleton
class FusedDeviceLocationSource @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
) : DeviceLocationSource {

    @SuppressLint("MissingPermission")
    override suspend fun currentOrLastKnown(): DeviceLocation? {
        val location = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            CancellationTokenSource().token,
        ).await() ?: fusedLocationClient.lastLocation.await()
        return location?.let {
            DeviceLocation(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracyMeters = if (it.hasAccuracy()) it.accuracy else null,
            )
        }
    }
}

sealed interface DeviceLocationResult {
    data class Available(val location: DeviceLocation) : DeviceLocationResult
    /** 위치 동의가 없어 위치 API 를 부르지 않았다. */
    data object NotAllowed : DeviceLocationResult
    data object PermissionDenied : DeviceLocationResult
    data object Unavailable : DeviceLocationResult
}

/**
 * 기기 위치를 읽는 유일한 길. 동의 게이트를 먼저 확인하고, 실제로 위치를 읽었을 때만
 * 목적별 이용 기록을 남긴다.
 */
@Singleton
class DeviceLocationReader @Inject constructor(
    private val consentRepository: LocationConsentRepository,
    private val source: DeviceLocationSource,
) {
    suspend fun read(vararg purposes: LocationUsagePurpose): DeviceLocationResult {
        if (!consentRepository.isAllowed()) return DeviceLocationResult.NotAllowed
        val location = try {
            source.currentOrLastKnown()
        } catch (e: SecurityException) {
            return DeviceLocationResult.PermissionDenied
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return DeviceLocationResult.Unavailable
        } ?: return DeviceLocationResult.Unavailable
        purposes.forEach { consentRepository.recordUsage(it) }
        return DeviceLocationResult.Available(location)
    }
}
