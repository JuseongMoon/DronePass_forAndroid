package com.ScienceFiction.DronePassAndroid.core.di

import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpGfzApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpNoaa27DayApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpNoaaApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.VWorldApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_MAP_CLIENT_ID)
                    .addHeader("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_MAP_CLIENT_SECRET)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    /**
     * VWorld / Kp API용 범용 OkHttpClient (네이버 헤더 없음)
     */
    @Provides
    @Singleton
    @Named("GenericOkHttp")
    fun provideGenericOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://maps.apigw.ntruss.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideNaverGeocodingApi(retrofit: Retrofit): NaverGeocodingApi {
        return retrofit.create(NaverGeocodingApi::class.java)
    }

    // ===== VWorld API =====

    @Provides
    @Singleton
    fun provideVWorldApi(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): VWorldApi {
        return Retrofit.Builder()
            .baseUrl("https://api.vworld.kr/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(VWorldApi::class.java)
    }

    @Provides
    @Singleton
    @Named("VWorldApiKey")
    fun provideVWorldApiKey(): String {
        return BuildConfig.VWORLD_API_KEY
    }

    // ===== Kp Index API =====

    @Provides
    @Singleton
    fun provideKpNoaaApi(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient
    ): KpNoaaApi {
        return Retrofit.Builder()
            .baseUrl("https://services.swpc.noaa.gov/")
            .client(okHttpClient)
            .build()
            .create(KpNoaaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKpNoaa27DayApi(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient
    ): KpNoaa27DayApi {
        return Retrofit.Builder()
            .baseUrl("https://services.swpc.noaa.gov/")
            .client(okHttpClient)
            .build()
            .create(KpNoaa27DayApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKpGfzApi(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient
    ): KpGfzApi {
        return Retrofit.Builder()
            .baseUrl("https://kp.gfz.de/")
            .client(okHttpClient)
            .build()
            .create(KpGfzApi::class.java)
    }

    // ===== Weather API (Open-Meteo) =====

    @Provides
    @Singleton
    @Named("weatherRetrofit")
    fun provideWeatherRetrofit(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApi(@Named("weatherRetrofit") retrofit: Retrofit): WeatherApi {
        return retrofit.create(WeatherApi::class.java)
    }
}
