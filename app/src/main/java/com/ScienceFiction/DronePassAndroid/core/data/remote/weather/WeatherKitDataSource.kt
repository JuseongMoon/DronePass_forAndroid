package com.ScienceFiction.DronePassAndroid.core.data.remote.weather

import com.ScienceFiction.DronePassAndroid.core.di.WeatherCallableUrl
import com.ScienceFiction.DronePassAndroid.core.di.WeatherHttpClient
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.IOException
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

/** callable HTTP 프로토콜의 App Check 헤더. 이 요청에 붙는 헤더는 이것과 Content-Type 뿐이다. */
internal const val AppCheckHeader = "X-Firebase-AppCheck"

/** `https://{region}-{projectId}.cloudfunctions.net/getAndroidWeather` */
internal fun weatherCallableUrl(projectId: String, region: String): String =
    "https://$region-$projectId.cloudfunctions.net/$WEATHERKIT_CALLABLE_NAME"

/** App Check 토큰 공급. 테스트에서는 가짜로 바꾼다. */
interface AppCheckTokenSource {
    /** 받지 못하면 null(서버가 permission-denied 로 거절한다). */
    suspend fun token(): String?
}

/**
 * Firebase callable `getAndroidWeather` 로 WeatherKit 날씨를 받는다.
 *
 * Functions SDK 를 쓰지 않고 callable HTTP 프로토콜로 직접 부른다. SDK 는 로그인 토큰(Authorization)과
 * FCM 토큰(Firebase-Instance-ID-Token)을 붙이는데, 둘 다 계정과 이어질 수 있어 위치가 담긴 요청에는 넣지 않는다
 * (사양 v2 F). 이 요청의 헤더는 Content-Type 과 App Check 토큰뿐이다([buildWeatherCallableRequest]).
 * 위치가 담긴 요청이라 로깅 인터셉터가 없는 전용 클라이언트를 쓴다.
 */
@Singleton
class FirebaseWeatherKitDataSource @Inject constructor(
    @WeatherHttpClient private val client: OkHttpClient,
    @WeatherCallableUrl private val url: String,
    private val appCheck: AppCheckTokenSource,
    private val moshi: Moshi,
) : WeatherKitDataSource {

    override suspend fun fetch(latitude: Double, longitude: Double, timezone: String): WeatherKitResponse {
        val body = weatherCallableBody(weatherKitRequest(latitude, longitude, timezone), moshi)
        val request = buildWeatherCallableRequest(url, body, appCheck.token())
        val (code, responseBody) = try {
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response -> response.code to response.body?.string().orEmpty() }
            }
        } catch (e: IOException) {
            throw WeatherServiceException(WeatherServiceFailure.OTHER, e)
        }
        return parseWeatherCallableResponse(code, responseBody, moshi)
    }
}

/** callable 요청 본문 `{"data": {...}}`. */
internal fun weatherCallableBody(data: Map<String, Any>, moshi: Moshi): String {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    return moshi.adapter<Map<String, Any>>(type).toJson(mapOf("data" to data))
}

internal fun buildWeatherCallableRequest(url: String, body: String, appCheckToken: String?): Request =
    Request.Builder()
        .url(url)
        .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .apply { if (appCheckToken != null) header(AppCheckHeader, appCheckToken) }
        .build()

/** callable 응답: 성공은 `{"result": {...}}`, 실패는 `{"error": {"status": "...", ...}}`. */
internal fun parseWeatherCallableResponse(httpCode: Int, body: String, moshi: Moshi): WeatherKitResponse {
    var result: WeatherKitResponse? = null
    var errorStatus: String? = null
    try {
        val reader = JsonReader.of(Buffer().writeUtf8(body))
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "result" -> result = moshi.adapter(WeatherKitResponse::class.java).fromJson(reader)
                "error" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        if (reader.nextName() == "status") errorStatus = reader.nextString() else reader.skipValue()
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
    } catch (e: Exception) {
        if (httpCode !in 200..299) throw WeatherServiceException(weatherFailureForHttp(httpCode, null), e)
        throw WeatherServiceException(WeatherServiceFailure.OTHER, e)
    }
    if (errorStatus != null || httpCode !in 200..299) {
        throw WeatherServiceException(
            weatherFailureForHttp(httpCode, errorStatus),
            IllegalStateException("getAndroidWeather failed: ${errorStatus ?: httpCode}"),
        )
    }
    return result ?: throw WeatherServiceException(WeatherServiceFailure.OTHER, IllegalStateException("Empty weather payload"))
}

internal fun weatherFailureForHttp(httpCode: Int, status: String?): WeatherServiceFailure = when (status) {
    "UNAVAILABLE" -> WeatherServiceFailure.UNAVAILABLE
    "PERMISSION_DENIED", "UNAUTHENTICATED" -> WeatherServiceFailure.APP_VERIFICATION
    "INVALID_ARGUMENT" -> WeatherServiceFailure.INVALID_REQUEST
    null -> when (httpCode) {
        503 -> WeatherServiceFailure.UNAVAILABLE
        401, 403 -> WeatherServiceFailure.APP_VERIFICATION
        400 -> WeatherServiceFailure.INVALID_REQUEST
        else -> WeatherServiceFailure.OTHER
    }
    else -> WeatherServiceFailure.OTHER
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

/** callable 결과 JSON(= fixture 와 같은 형식)을 응답 모델로 바꾼다. */
internal fun parseWeatherKitResponse(json: String, moshi: Moshi): WeatherKitResponse =
    moshi.adapter(WeatherKitResponse::class.java).fromJson(json)
        ?: throw WeatherServiceException(WeatherServiceFailure.OTHER, IllegalStateException("Empty weather payload"))
