package com.ScienceFiction.DronePassAndroid.core.di

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.FirebaseWeatherKitDataSource
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitDataSource
import com.ScienceFiction.DronePassAndroid.core.location.DeviceLocationReader
import com.ScienceFiction.DronePassAndroid.core.location.DeviceLocationSource
import com.ScienceFiction.DronePassAndroid.core.location.FirebaseLocationUsageServer
import com.ScienceFiction.DronePassAndroid.core.location.FusedDeviceLocationSource
import com.ScienceFiction.DronePassAndroid.core.location.LocationUsageServer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Repository 모듈.
 *
 * 대부분의 Repository / RealtimeSyncManager 는 `@Singleton @Inject constructor` 로
 * Hilt 가 자동 그래프를 구성하므로 별도의 `@Provides` 가 필요하지 않다.
 * 인터페이스로 추상화한 데이터 소스만 여기서 `@Binds` 로 연결한다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /** 날씨: Firebase callable `getAndroidWeather`(WeatherKit 중계). 테스트는 가짜 소스로 바꾼다. */
    @Binds
    abstract fun bindWeatherKitDataSource(impl: FirebaseWeatherKitDataSource): WeatherKitDataSource

    /** 기기 위치: Fused Location. 위치 동의 게이트는 [DeviceLocationReader] 가 맡는다. */
    @Binds
    abstract fun bindDeviceLocationSource(impl: FusedDeviceLocationSource): DeviceLocationSource

    /** 위치정보 이용사실 확인자료 서버(callable `recordLocationUsage`·`deleteLocationUsage`). */
    @Binds
    abstract fun bindLocationUsageServer(impl: FirebaseLocationUsageServer): LocationUsageServer
}
