package com.ScienceFiction.DronePassAndroid.core.di

import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.VWorldContactsApi
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

    /**
     * Naver Maps APIGW 헤더명.
     */
    internal const val NaverApiKeyIdHeader = "X-NCP-APIGW-API-KEY-ID"
    internal const val NaverApiKeyHeader = "X-NCP-APIGW-API-KEY"

    /**
     * Naver API는 인증값이 header로 전달되므로 debug 로깅 시에도 header redaction을 고정한다.
     */
    internal fun naverHttpLoggingInterceptor(
        logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT,
        debug: Boolean = BuildConfig.DEBUG,
    ): HttpLoggingInterceptor = HttpLoggingInterceptor(logger).apply {
        redactHeader(NaverApiKeyIdHeader)
        redactHeader(NaverApiKeyHeader)
        level = if (debug) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    /**
     * VWorld key는 query parameter로 전달되어 OkHttp 로거가 안전하게 redaction할 수 없다.
     */
    internal fun genericHttpLoggingInterceptor(
        logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT,
    ): HttpLoggingInterceptor = HttpLoggingInterceptor(logger).apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    /**
     * Naver Maps APIGW 전용 OkHttpClient.
     * X-NCP-APIGW-API-KEY-ID / -API-KEY 헤더를 모든 요청에 부착하므로 다른 API 에서
     * 재사용하지 않아야 한다. @Named("NaverOkHttp") 한정자로 명시 분리.
     */
    @Provides
    @Singleton
    @Named("NaverOkHttp")
    fun provideNaverOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader(NaverApiKeyIdHeader, BuildConfig.NAVER_MAP_CLIENT_ID)
                    .addHeader(NaverApiKeyHeader, BuildConfig.NAVER_MAP_CLIENT_SECRET)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(naverHttpLoggingInterceptor())
            .build()
    }

    /**
     * VWorld / Kp / Weather API 용 범용 OkHttpClient (네이버 헤더 없음).
     */
    @Provides
    @Singleton
    @Named("GenericOkHttp")
    fun provideGenericOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(genericHttpLoggingInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Naver Maps APIGW 전용 Retrofit. @Named("NaverOkHttp") OkHttpClient 만 사용한다.
     * 기본 Retrofit 으로 사용되면 다른 API 호출에도 Naver 헤더가 부착되어 키가 의도치 않게
     * 노출될 위험이 있으므로 @Named 한정자로 명시 분리.
     *
     * timeout 은 [provideNaverOkHttpClient] 의 connect/read 30s 가 적용된다 (Retrofit 자체엔
     * 별도 timeout 이 없고 OkHttpClient 에 위임).
     */
    @Provides
    @Singleton
    @Named("NaverRetrofit")
    fun provideNaverRetrofit(
        @Named("NaverOkHttp") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://maps.apigw.ntruss.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideNaverGeocodingApi(@Named("NaverRetrofit") retrofit: Retrofit): NaverGeocodingApi {
        return retrofit.create(NaverGeocodingApi::class.java)
    }

    // ===== VWorld API =====

    @Provides
    @Singleton
    @Named("vWorldRetrofit")
    fun provideVWorldRetrofit(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.vworld.kr/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideVWorldApi(@Named("vWorldRetrofit") retrofit: Retrofit): VWorldApi {
        return retrofit.create(VWorldApi::class.java)
    }

    @Provides
    @Singleton
    @Named("VWorldApiKey")
    fun provideVWorldApiKey(): String {
        return BuildConfig.VWORLD_API_KEY
    }

    // ===== VWorld 공공기관 연락처 (S3 텍스트 파일) =====
    // iOS VWorldContactManager 와 동일한 endpoint. JSON 변환 없이 ResponseBody 만 받는다.

    @Provides
    @Singleton
    @Named("vWorldContactsRetrofit")
    fun provideVWorldContactsRetrofit(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://sciencefiction.co.kr/")
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideVWorldContactsApi(
        @Named("vWorldContactsRetrofit") retrofit: Retrofit,
    ): VWorldContactsApi = retrofit.create(VWorldContactsApi::class.java)

    // ===== Document API (자체 서버 plain-text 문서) =====
    // iOS FetchWebDocuments 와 동일 endpoint (`https://sciencefiction.co.kr/dronepass/...`).
    // VWorldContacts 와 같은 도메인이지만 Retrofit 인스턴스를 분리하여 timeout/캐시 정책을
    // 독립 조정할 수 있게 한다 (Kp NOAA 패턴과 동일).

    @Provides
    @Singleton
    @Named("documentRetrofit")
    fun provideDocumentRetrofit(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://sciencefiction.co.kr/")
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideDocumentApi(
        @Named("documentRetrofit") retrofit: Retrofit,
    ): com.ScienceFiction.DronePassAndroid.core.data.remote.document.DocumentApi =
        retrofit.create(com.ScienceFiction.DronePassAndroid.core.data.remote.document.DocumentApi::class.java)

    // ===== Kp Index API =====
    // NOAA SWPC 의 nowcast/예보/27일outlook 은 baseUrl 이 같지만 Retrofit 인스턴스를
    // 명시 분리하여 API 별로 timeout/Converter 정책을 독립 조정할 수 있게 한다.

    @Provides
    @Singleton
    @Named("kpNoaaRetrofit")
    fun provideKpNoaaRetrofit(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://services.swpc.noaa.gov/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideKpNoaaApi(@Named("kpNoaaRetrofit") retrofit: Retrofit): KpNoaaApi {
        return retrofit.create(KpNoaaApi::class.java)
    }

    @Provides
    @Singleton
    @Named("kpNoaa27DayRetrofit")
    fun provideKpNoaa27DayRetrofit(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://services.swpc.noaa.gov/")
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideKpNoaa27DayApi(@Named("kpNoaa27DayRetrofit") retrofit: Retrofit): KpNoaa27DayApi {
        return retrofit.create(KpNoaa27DayApi::class.java)
    }

    @Provides
    @Singleton
    @Named("kpGfzRetrofit")
    fun provideKpGfzRetrofit(
        @Named("GenericOkHttp") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://kp.gfz.de/")
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideKpGfzApi(@Named("kpGfzRetrofit") retrofit: Retrofit): KpGfzApi {
        return retrofit.create(KpGfzApi::class.java)
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
