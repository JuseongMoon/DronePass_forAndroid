package com.ScienceFiction.DronePassAndroid.core.di

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class NetworkModuleTest {

    @Test
    fun `Naver debug HTTP logger는 API key header 값을 redaction한다`() {
        val logs = mutableListOf<String>()
        val interceptor = NetworkModule.naverHttpLoggingInterceptor(
            logger = HttpLoggingInterceptor.Logger { logs += it },
            debug = true,
        )
        val request = Request.Builder()
            .url("https://maps.apigw.ntruss.com/map-geocode/v2/geocode?query=Seoul")
            .header(NetworkModule.NaverApiKeyIdHeader, "secret-key-id")
            .header(NetworkModule.NaverApiKeyHeader, "secret-key")
            .build()

        interceptor.intercept(FakeChain(request))
        val loggedText = logs.joinToString("\n")

        assertTrue(loggedText.contains(NetworkModule.NaverApiKeyIdHeader))
        assertTrue(loggedText.contains(NetworkModule.NaverApiKeyHeader))
        assertFalse(loggedText.contains("secret-key-id"))
        assertFalse(loggedText.contains("secret-key"))
    }

    @Test
    fun `Naver release HTTP logger는 비활성화된다`() {
        assertEquals(
            HttpLoggingInterceptor.Level.NONE,
            NetworkModule.naverHttpLoggingInterceptor(debug = false).level,
        )
    }

    @Test
    fun `범용 HTTP logger는 VWorld query key 노출 방지를 위해 debug에서도 비활성화된다`() {
        assertEquals(
            HttpLoggingInterceptor.Level.NONE,
            NetworkModule.genericHttpLoggingInterceptor().level,
        )
    }

    private class FakeChain(
        private val request: Request,
    ) : Interceptor.Chain {

        override fun request(): Request = request

        override fun proceed(request: Request): Response =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody())
                .build()

        override fun connection(): Connection? = null

        override fun call(): Call {
            error("FakeChain.call is not used by HttpLoggingInterceptor")
        }

        override fun connectTimeoutMillis(): Int = 0

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun readTimeoutMillis(): Int = 0

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 0

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }
}
