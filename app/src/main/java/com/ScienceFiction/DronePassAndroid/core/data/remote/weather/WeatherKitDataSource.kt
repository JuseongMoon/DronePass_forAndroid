package com.ScienceFiction.DronePassAndroid.core.data.remote.weather

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/** 날씨 원본 데이터 출처. 테스트에서는 fixture 를 돌려주는 가짜로 바꾼다. */
interface WeatherKitDataSource {
    suspend fun fetch(latitude: Double, longitude: Double, timezone: String): WeatherKitResponse
}

/** 중계 함수 오류 종류. 화면에서 원인별 안내 문구를 고르는 데 쓴다. */
enum class WeatherServiceFailure {
    /** 서버 상한 초과 등으로 줄 수 있는 캐시가 없음 (`unavailable`). */
    UNAVAILABLE,
    /** App Check 토큰이 없거나 맞지 않음 (`permission-denied`). */
    APP_VERIFICATION,
    /** 요청 값 오류 (`invalid-argument`). */
    INVALID_REQUEST,
    /** 네트워크 오류 등 그 밖의 실패. */
    OTHER,
}

class WeatherServiceException(
    val failure: WeatherServiceFailure,
    cause: Throwable? = null,
) : Exception(cause?.message ?: failure.name, cause)

internal const val WEATHERKIT_CALLABLE_NAME = "getAndroidWeather"

/**
 * Firebase callable `getAndroidWeather` 로 WeatherKit 날씨를 받는다.
 * App Check 토큰은 Firebase SDK 가 자동으로 붙인다(Application 에서 공급자 설치).
 */
@Singleton
class FirebaseWeatherKitDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
    private val moshi: Moshi,
) : WeatherKitDataSource {

    override suspend fun fetch(latitude: Double, longitude: Double, timezone: String): WeatherKitResponse {
        val request = weatherKitRequest(latitude, longitude, timezone)
        val data = try {
            functions.getHttpsCallable(WEATHERKIT_CALLABLE_NAME).call(request).await().getData()
        } catch (e: FirebaseFunctionsException) {
            throw WeatherServiceException(e.code.toWeatherServiceFailure(), e)
        }
        val map = data as? Map<*, *>
            ?: throw WeatherServiceException(WeatherServiceFailure.OTHER, IllegalStateException("Unexpected weather payload"))
        return parseWeatherKitResponse(JSONObject(map).toString(), moshi)
    }
}

/**
 * 중계 함수 요청 본문. 측위 원본 좌표는 단말 밖으로 보내지 않고 0.01° 격자로 반올림해 보낸다.
 * 서버도 같은 격자로 Apple 에 요청하므로 날씨 결과는 같다.
 */
internal fun weatherKitRequest(latitude: Double, longitude: Double, timezone: String): Map<String, Any> = mapOf(
    "latitude" to weatherGridCoordinate(latitude),
    "longitude" to weatherGridCoordinate(longitude),
    "timezone" to timezone,
)

/**
 * 서버 `gridCoordinate` 와 같은 규칙: sign(v) * round((|v| + 1e-10) * 100) / 100, -0 은 0.
 * 서버가 다시 반올림해도 격자가 바뀌지 않도록 같은 식을 쓴다. 유한하지 않은 값은 서버가 거부하도록 그대로 둔다.
 */
internal fun weatherGridCoordinate(value: Double): Double {
    if (!value.isFinite()) return value
    val rounded = Math.signum(value) * Math.round((abs(value) + 1e-10) * 100) / 100
    return if (rounded == 0.0) 0.0 else rounded
}

internal fun FirebaseFunctionsException.Code.toWeatherServiceFailure(): WeatherServiceFailure = when (this) {
    FirebaseFunctionsException.Code.UNAVAILABLE -> WeatherServiceFailure.UNAVAILABLE
    FirebaseFunctionsException.Code.PERMISSION_DENIED,
    FirebaseFunctionsException.Code.UNAUTHENTICATED -> WeatherServiceFailure.APP_VERIFICATION
    FirebaseFunctionsException.Code.INVALID_ARGUMENT -> WeatherServiceFailure.INVALID_REQUEST
    else -> WeatherServiceFailure.OTHER
}

/** callable 결과 JSON(= fixture 와 같은 형식)을 응답 모델로 바꾼다. */
internal fun parseWeatherKitResponse(json: String, moshi: Moshi): WeatherKitResponse =
    moshi.adapter(WeatherKitResponse::class.java).fromJson(json)
        ?: throw WeatherServiceException(WeatherServiceFailure.OTHER, IllegalStateException("Empty weather payload"))
