package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpForecastItemDto
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpGfzApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpNoaa27DayApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpNoaaApi
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class KpIndexRepositoryTest {

    @Test
    fun `current KP force refresh bypasses cache and parses GFZ decimal hour like iOS`() = runBlocking {
        val gfzApi = FakeGfzApi(gfzText(hour = "03.0", kp = 1.0))
        val repository = repository(gfzApi = gfzApi)

        val first = repository.getCurrentKp().getOrThrow()
        assertEquals(1.0, first.kp, 0.0)
        assertEquals("2026-02-24 03:00:00", first.timeTag)

        gfzApi.text = gfzText(hour = "06.0", kp = 2.0)
        assertEquals(1.0, repository.getCurrentKp().getOrThrow().kp, 0.0)

        val refreshed = repository.getCurrentKp(forceRefresh = true).getOrThrow()
        assertEquals(2.0, refreshed.kp, 0.0)
        assertEquals("2026-02-24 06:00:00", refreshed.timeTag)
        assertEquals(2, gfzApi.calls)
    }

    @Test
    fun `forecast force refresh bypasses cache like iOS refresh`() = runBlocking {
        val noaaApi = FakeNoaaApi(listOf(kpForecast(kp = 1.0)))
        val repository = repository(noaaApi = noaaApi)

        assertEquals(1.0, repository.getForecast().getOrThrow().single().kp, 0.0)

        noaaApi.items = listOf(kpForecast(kp = 2.0))
        assertEquals(1.0, repository.getForecast().getOrThrow().single().kp, 0.0)
        assertEquals(2.0, repository.getForecast(forceRefresh = true).getOrThrow().single().kp, 0.0)
        assertEquals(2, noaaApi.calls)
    }

    @Test
    fun `twenty seven day force refresh bypasses cache like iOS refresh`() = runBlocking {
        val noaa27DayApi = FakeNoaa27DayApi(outlookText(kp = 3.0, ap = 12))
        val repository = repository(noaa27DayApi = noaa27DayApi)

        assertEquals(3.0, repository.get27DayForecast().getOrThrow().single().kp, 0.0)

        noaa27DayApi.text = outlookText(kp = 4.0, ap = 15)
        assertEquals(3.0, repository.get27DayForecast().getOrThrow().single().kp, 0.0)
        val refreshed = repository.get27DayForecast(forceRefresh = true).getOrThrow().single()
        assertEquals(4.0, refreshed.kp, 0.0)
        assertEquals(15, refreshed.ap)
        assertEquals(2, noaa27DayApi.calls)
    }

    @Test
    fun `NOAA fallback current KP uses iOS last observed-like value`() {
        val forecast = listOf(
            KpIndexData(timeTag = "2026-02-24 00:00:00", kp = 1.0, observed = "observed"),
            KpIndexData(timeTag = "2026-02-24 03:00:00", kp = 2.0, observed = "predicted"),
            KpIndexData(timeTag = "2026-02-24 06:00:00", kp = 3.0, observed = null),
            KpIndexData(timeTag = "2026-02-24 09:00:00", kp = 4.0, observed = "predicted"),
        )

        assertEquals(
            3.0,
            requireNotNull(selectCurrentKpFromNoaaForecastLikeIos(forecast)).kp,
            0.0,
        )
    }

    @Test
    fun `NOAA fallback current KP uses iOS last data when no observed-like values exist`() {
        val forecast = listOf(
            KpIndexData(timeTag = "2026-02-24 00:00:00", kp = 1.0, observed = "estimated"),
            KpIndexData(timeTag = "2026-02-24 03:00:00", kp = 2.0, observed = "predicted"),
        )

        assertEquals(
            2.0,
            requireNotNull(selectCurrentKpFromNoaaForecastLikeIos(forecast)).kp,
            0.0,
        )
    }

    private fun repository(
        gfzApi: FakeGfzApi = FakeGfzApi(gfzText()),
        noaaApi: FakeNoaaApi = FakeNoaaApi(emptyList()),
        noaa27DayApi: FakeNoaa27DayApi = FakeNoaa27DayApi(outlookText()),
    ): KpIndexRepository = KpIndexRepository(
        gfzApi = gfzApi,
        noaaApi = noaaApi,
        noaa27DayApi = noaa27DayApi,
    )

    private fun gfzText(hour: String = "00.0", kp: Double = 1.0): String =
        """
        # header
        2026 02 24 $hour 03.50 34253.00000 34253.12500 $kp 10 0
        """.trimIndent()

    private fun outlookText(kp: Double = 3.0, ap: Int = 12): String =
        """
        :Product: 27-day Space Weather Outlook Table 27DO.txt
        2026 Feb 24     112          $ap          $kp
        """.trimIndent()

    private fun kpForecast(kp: Double): KpForecastItemDto = KpForecastItemDto(
        timeTag = "2026-02-24 03:00:00",
        kp = kp,
        observed = "observed",
        noaaScale = null,
    )

    private class FakeGfzApi(var text: String) : KpGfzApi {
        var calls = 0

        override suspend fun getKpNowcast(): ResponseBody {
            calls += 1
            return text.toResponseBody()
        }
    }

    private class FakeNoaaApi(var items: List<KpForecastItemDto>) : KpNoaaApi {
        var calls = 0

        override suspend fun getKpForecast(): List<KpForecastItemDto> {
            calls += 1
            return items
        }
    }

    private class FakeNoaa27DayApi(var text: String) : KpNoaa27DayApi {
        var calls = 0

        override suspend fun get27DayOutlook(): ResponseBody {
            calls += 1
            return text.toResponseBody()
        }
    }
}
