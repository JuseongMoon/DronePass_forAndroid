package com.ScienceFiction.DronePassAndroid.feature.vworld

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 비행구역을 불러오는 중에 레이어를 모두 해제하면 로딩 스피너가 지도에 계속 남았다(3.6.1 에뮬레이터 재현).
 * 취소된 조회가 일반 오류 처리로 들어가 로딩 플래그를 끄지 못했고, 다음 조회는 레이어가 없어 시작되지 않았다.
 */
class FlightZoneLoadingCancellationTest {

    private fun source(path: String): String {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        return File(root, "com/ScienceFiction/DronePassAndroid/$path").readText()
    }

    @Test
    fun `flight zone loading flag is cleared even when the load is cancelled`() {
        val load = source("feature/map/MapViewModel.kt")
            .substringAfter("_flightZonesLoading.value = true")
        val cancel = load.indexOf("catch (e: CancellationException)")
        assertTrue(cancel >= 0 && cancel < load.indexOf("catch (e: Exception)"))
        assertTrue(load.substringAfter("finally {").substringBefore("}").contains("_flightZonesLoading.value = false"))
    }

    @Test
    fun `a cancelled layer fetch is not reported as an empty result`() {
        val fetch = source("core/data/repository/VWorldRepository.kt")
        val cancel = fetch.indexOf("catch (e: CancellationException)")
        assertTrue(cancel >= 0 && cancel < fetch.indexOf("catch (e: Exception)"))
    }
}
