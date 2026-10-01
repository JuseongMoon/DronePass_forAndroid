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
    /** 그 목적의 위치 동의가 없어 위치 API 를 부르지 않았다. */
    data object NotAllowed : DeviceLocationResult
    data object PermissionDenied : DeviceLocationResult
    data object Unavailable : DeviceLocationResult
}

/**
 * 기기 위치를 읽는 유일한 길. 목적별 동의 게이트를 먼저 확인하고, 실제로 위치를 읽었을 때만
 * 확인자료(시간·목적·제공받는 자)를 남긴다.
 */
@Singleton
class DeviceLocationReader @Inject constructor(
    private val consentRepository: LocationConsentRepository,
    private val source: DeviceLocationSource,
) {
    /** [usages] 는 모두 같은 동의 목적이어야 한다(예: 날씨 조회와 일출·일몰 계산은 둘 다 P2). */
    suspend fun read(vararg usages: LocationUsage): DeviceLocationResult {
        val purposes = usages.map { it.purpose.consentPurpose }.toSet()
        require(purposes.size == 1) { "Location usages must share one consent purpose" }
        if (!consentRepository.isAllowed(purposes.single())) return DeviceLocationResult.NotAllowed
        val location = try {
            source.currentOrLastKnown()
        } catch (e: SecurityException) {
            return DeviceLocationResult.PermissionDenied
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return DeviceLocationResult.Unavailable
        } ?: return DeviceLocationResult.Unavailable
        usages.forEach { consentRepository.recordUsage(it) }
        return DeviceLocationResult.Available(location)
    }
}
