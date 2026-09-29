package com.ScienceFiction.DronePassAndroid.core.di

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.FirebaseWeatherKitDataSource
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitDataSource
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
}
