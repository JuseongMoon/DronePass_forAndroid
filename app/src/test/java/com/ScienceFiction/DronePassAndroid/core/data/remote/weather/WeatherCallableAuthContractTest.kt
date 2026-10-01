package com.ScienceFiction.DronePassAndroid.core.data.remote.weather

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * `getAndroidWeather` 요청에 계정과 이어질 수 있는 식별자가 붙지 않아야 한다(사양 v2 F).
 * SDK 를 쓰지 않고 callable HTTP 로 직접 부르며, 앱이 붙이는 헤더는 Content-Type 과 App Check 토큰뿐이다.
 */
class WeatherCallableAuthContractTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val root = "src/main/java/com/ScienceFiction/DronePassAndroid"

    @Test
    fun `weather request carries only content type and App Check headers`() {
        val body = weatherCallableBody(weatherKitRequest(37.566535, 126.977969, "Asia/Seoul"), moshi)
        val request = buildWeatherCallableRequest(
            url = weatherCallableUrl("dronepass-test", "asia-northeast3"),
            body = body,
            appCheckToken = "app-check-token",
        )

        assertEquals("https://asia-northeast3-dronepass-test.cloudfunctions.net/getAndroidWeather", request.url.toString())
        assertEquals("POST", request.method)
        assertEquals(setOf(AppCheckHeader), request.headers.names())
        assertEquals("app-check-token", request.header(AppCheckHeader))
        assertEquals("application/json; charset=utf-8", request.body?.contentType().toString())
        for (identifying in listOf("Authorization", "Firebase-Instance-ID-Token", "Cookie", "X-Firebase-GMPID")) {
            assertEquals(identifying, null, request.header(identifying))
        }

        val sent = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
        assertEquals("""{"data":{"latitude":37.57,"longitude":126.98,"timezone":"Asia/Seoul"}}""", sent)
    }

    @Test
    fun `without an App Check token no header is added at all`() {
        val request = buildWeatherCallableRequest("https://example.invalid/getAndroidWeather", "{}", appCheckToken = null)

        assertTrue(request.headers.names().isEmpty())
    }

    @Test
    fun `callable result is parsed into the weather response`() {
        val fixture = requireNotNull(javaClass.classLoader?.getResource("weatherkit/android-weather-response.json")).readText()

        val response = parseWeatherCallableResponse(200, """{"result":$fixture}""", moshi)

        assertEquals(77, response.forecastHourly?.hours?.size)
        assertEquals(37.57, response.gridLatitude ?: 0.0, 0.0)
    }

    @Test
    fun `callable errors keep the existing failure meaning`() {
        val cases = listOf(
            503 to """{"error":{"status":"UNAVAILABLE","message":"quota"}}""" to WeatherServiceFailure.UNAVAILABLE,
            403 to """{"error":{"status":"PERMISSION_DENIED","message":"app check"}}""" to WeatherServiceFailure.APP_VERIFICATION,
            401 to """{"error":{"status":"UNAUTHENTICATED"}}""" to WeatherServiceFailure.APP_VERIFICATION,
            400 to """{"error":{"status":"INVALID_ARGUMENT"}}""" to WeatherServiceFailure.INVALID_REQUEST,
            500 to """{"error":{"status":"INTERNAL"}}""" to WeatherServiceFailure.OTHER,
            502 to "<html>bad gateway</html>" to WeatherServiceFailure.OTHER,
            503 to "" to WeatherServiceFailure.UNAVAILABLE,
        )
        for ((input, expected) in cases) {
            val (code, body) = input
            try {
                parseWeatherCallableResponse(code, body, moshi)
                fail("expected failure for $code $body")
            } catch (e: WeatherServiceException) {
                assertEquals("$code $body", expected, e.failure)
            }
        }
    }

    @Test
    fun `weather call does not use the Functions SDK or a logging client`() {
        val dataSource = File("$root/core/data/remote/weather/WeatherKitDataSource.kt").readText()
        assertFalse(dataSource.contains("FirebaseFunctions"))
        assertFalse(dataSource.contains("getHttpsCallable"))

        val module = File("$root/core/di/FirebaseModule.kt").readText()
        val client = module.substringAfter("fun provideWeatherHttpClient(").substringBefore("@Provides")
        assertFalse(client.contains("Interceptor"))
    }

    @Test
    fun `cached sun alarm locations are rounded to the weather grid`() {
        for (path in listOf("feature/weather/WeatherViewModel.kt", "feature/settings/SettingsViewModel.kt")) {
            val writes = Regex("KEY_LAST_(LATITUDE|LONGITUDE)] = ([^\\n]+)").findAll(File("$root/$path").readText())
                .map { it.groupValues[2] }.toList()
            assertTrue(path, writes.isNotEmpty())
            assertTrue("$path: $writes", writes.all { it.startsWith("weatherGridCoordinate(") })
        }
    }
}
