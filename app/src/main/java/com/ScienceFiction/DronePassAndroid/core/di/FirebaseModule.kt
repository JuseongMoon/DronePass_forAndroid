package com.ScienceFiction.DronePassAndroid.core.di

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.AppCheckTokenSource
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.weatherCallableUrl
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

internal const val FIREBASE_FUNCTIONS_REGION = "asia-northeast3"

/** 날씨 callable 전용 OkHttpClient(로깅 인터셉터 없음: 요청에 위치가 담긴다). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WeatherHttpClient

/** `getAndroidWeather` 의 callable URL. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WeatherCallableUrl

/**
 * Firebase 관련 의존성을 제공하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return FirebaseFunctions.getInstance(FIREBASE_FUNCTIONS_REGION)
    }

    @Provides
    @Singleton
    @WeatherHttpClient
    fun provideWeatherHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides
    @WeatherCallableUrl
    fun provideWeatherCallableUrl(): String =
        weatherCallableUrl(requireNotNull(FirebaseApp.getInstance().options.projectId), FIREBASE_FUNCTIONS_REGION)

    /** 기본 앱의 App Check(Application 에서 공급자 설치: 릴리스 Play Integrity, 디버그 디버그 공급자). */
    @Provides
    @Singleton
    fun provideAppCheckTokenSource(): AppCheckTokenSource = object : AppCheckTokenSource {
        override suspend fun token(): String? =
            runCatching { FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token }.getOrNull()
    }
}
