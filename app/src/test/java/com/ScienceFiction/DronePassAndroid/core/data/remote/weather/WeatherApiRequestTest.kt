package com.ScienceFiction.DronePassAndroid.core.data.remote.weather

import com.ScienceFiction.DronePassAndroid.core.data.repository.WeatherRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class WeatherApiRequestTest {

    @Test
    fun `repository requests exactly three past hours and 73 forecast hours`() = runBlocking {
        val requestUrl = AtomicReference<HttpUrl>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestUrl.set(chain.request().url)
                throw IOException("Request captured")
            }
            .build()
        val api = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
            .build()
            .create(WeatherApi::class.java)

        val result = WeatherRepository(api).fetchWeather(latitude = 37.0, longitude = 127.0)

        assertTrue(result.isFailure)
        assertNotNull("Request was not sent: ${result.exceptionOrNull()}", requestUrl.get())
        val url = requireNotNull(requestUrl.get())
        assertEquals("/v1/forecast", url.encodedPath)
        assertEquals("3", url.queryParameter("forecast_days"))
        assertEquals("3", url.queryParameter("past_hours"))
        assertEquals("73", url.queryParameter("forecast_hours"))
    }
}
