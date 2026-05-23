package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpGfzApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpNoaa27DayApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpNoaaApi
import com.ScienceFiction.DronePassAndroid.domain.model.Kp27DayForecast
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kp 지수 데이터 Repository
 *
 * 30분 캐시를 사용하며, GFZ -> NOAA -> 캐시 순서로 Fallback한다.
 */
@Singleton
class KpIndexRepository @Inject constructor(
    private val gfzApi: KpGfzApi,
    private val noaaApi: KpNoaaApi,
    private val noaa27DayApi: KpNoaa27DayApi
) {
    companion object {
        private const val TAG = "KpIndexRepository"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30분
    }

    // 동시 호출(여러 ViewModel 의 refresh)에 대비한 캐시 동시성.
    // @Volatile 로 가시성 확보 + Mutex 로 read-modify-write 직렬화.
    @Volatile private var cachedCurrentKp: KpIndexData? = null
    @Volatile private var cachedForecast: List<KpIndexData>? = null
    @Volatile private var cached27DayForecast: List<Kp27DayForecast>? = null
    @Volatile private var lastFetchTime: Long = 0L
    @Volatile private var last27DayFetchTime: Long = 0L
    private val cacheMutex = Mutex()

    /**
     * 현재 Kp 지수의 StateFlow 노출 (여러 ViewModel 가 collect 하기 위한 단일 진실 원천).
     * iOS `KPIndexManager.currentKP` @Published 와 동등. KpViewModel·SettingsViewModel 공유.
     * 갱신은 getCurrentKp() 호출 시 자동으로 emit.
     */
    private val _currentKpFlow = MutableStateFlow<KpIndexData?>(null)
    val currentKpFlow: StateFlow<KpIndexData?> = _currentKpFlow.asStateFlow()

    /**
     * 현재 Kp 지수 조회
     *
     * Fallback 순서: GFZ Potsdam -> NOAA SWPC -> 캐시
     */
    suspend fun getCurrentKp(): Result<KpIndexData> {
        // 캐시 hit-path 는 락 없이 빠르게 통과 (@Volatile 가시성).
        if (isCacheValid()) {
            cachedCurrentKp?.let { return Result.success(it) }
        }

        // miss-path 는 직렬화하여 동시 GFZ/NOAA 호출 중복 방지.
        return cacheMutex.withLock {
            // 락 획득 후 재확인 (다른 코루틴이 이미 갱신했을 수 있음)
            if (isCacheValid()) {
                cachedCurrentKp?.let { return@withLock Result.success(it) }
            }

            // 1차: GFZ Potsdam
            try {
                val gfzResult = fetchFromGfz()
                if (gfzResult != null) {
                    cachedCurrentKp = gfzResult
                    _currentKpFlow.value = gfzResult
                    lastFetchTime = System.currentTimeMillis()
                    return@withLock Result.success(gfzResult)
                }
            } catch (e: Exception) {
                Log.w(TAG, "GFZ 데이터 로드 실패: ${e.message}")
            }

            // 2차: NOAA SWPC
            try {
                val noaaResult = fetchForecastFromNoaa()
                if (noaaResult.isNotEmpty()) {
                    val current = noaaResult.lastOrNull { it.observed == "observed" }
                        ?: noaaResult.lastOrNull { it.observed == "estimated" }
                        ?: noaaResult.first()
                    cachedCurrentKp = current
                    _currentKpFlow.value = current
                    cachedForecast = noaaResult
                    lastFetchTime = System.currentTimeMillis()
                    return@withLock Result.success(current)
                }
            } catch (e: Exception) {
                Log.w(TAG, "NOAA 데이터 로드 실패: ${e.message}")
            }

            // 3차: 캐시 (만료된 stale 캐시라도 반환)
            cachedCurrentKp?.let { return@withLock Result.success(it) }

            Result.failure(Exception("Kp 지수 데이터를 가져올 수 없습니다"))
        }
    }

    /**
     * Kp 지수 예보 데이터 조회
     */
    suspend fun getForecast(): Result<List<KpIndexData>> {
        if (isCacheValid()) {
            cachedForecast?.let { return Result.success(it) }
        }

        return cacheMutex.withLock {
            if (isCacheValid()) {
                cachedForecast?.let { return@withLock Result.success(it) }
            }
            try {
                val forecast = fetchForecastFromNoaa()
                if (forecast.isNotEmpty()) {
                    cachedForecast = forecast
                    lastFetchTime = System.currentTimeMillis()
                    return@withLock Result.success(forecast)
                }
            } catch (e: Exception) {
                Log.w(TAG, "예보 데이터 로드 실패: ${e.message}")
            }

            cachedForecast?.let { return@withLock Result.success(it) }
            Result.failure(Exception("Kp 지수 예보 데이터를 가져올 수 없습니다"))
        }
    }

    /**
     * 27일 장기 예보 데이터 조회
     *
     * NOAA SWPC 27-day outlook 텍스트를 파싱하여 일별 Kp/Ap 예측값을 반환한다.
     */
    suspend fun get27DayForecast(): Result<List<Kp27DayForecast>> {
        if (System.currentTimeMillis() - last27DayFetchTime < CACHE_DURATION_MS) {
            cached27DayForecast?.let { return Result.success(it) }
        }

        return cacheMutex.withLock {
            if (System.currentTimeMillis() - last27DayFetchTime < CACHE_DURATION_MS) {
                cached27DayForecast?.let { return@withLock Result.success(it) }
            }
            try {
                val response = noaa27DayApi.get27DayOutlook()
                val text = response.string()
                val forecasts = parse27DayOutlook(text)
                if (forecasts.isNotEmpty()) {
                    cached27DayForecast = forecasts
                    last27DayFetchTime = System.currentTimeMillis()
                    return@withLock Result.success(forecasts)
                }
            } catch (e: Exception) {
                Log.w(TAG, "27일 장기예보 로드 실패: ${e.message}")
            }

            cached27DayForecast?.let { return@withLock Result.success(it) }
            Result.failure(Exception("27일 장기예보 데이터를 가져올 수 없습니다"))
        }
    }

    /**
     * NOAA SWPC 27-day outlook 텍스트 파싱
     *
     * 텍스트 형식 예:
     * ```
     * :Product: 27-day Space Weather Outlook Table 27DO.txt
     * ...
     * 2026 Feb 25     5     20     0.01
     * 2026 Feb 26     3     12     0.05
     * ```
     *
     * 데이터 행: 날짜(Year Mon Day) + Kp + Ap + (기타) 형식
     * 해당하지 않는 행(주석, 빈 행, 헤더)은 건너뛴다.
     */
    private fun parse27DayOutlook(text: String): List<Kp27DayForecast> {
        val results = mutableListOf<Kp27DayForecast>()
        val months = setOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        for (line in text.lines()) {
            val trimmed = line.trim()
            // 주석, 빈 줄, 헤더 건너뛰기
            if (trimmed.isEmpty() || trimmed.startsWith(":") || trimmed.startsWith("#")) continue

            val parts = trimmed.split("\\s+".toRegex())
            // 데이터 행: Year Month Day RadioFlux AIndex KpIndex
            // 예: 2026 Feb 23     112          20          5
            if (parts.size >= 6) {
                val yearStr = parts[0]
                val monthStr = parts[1]
                val dayStr = parts[2]

                // 연도가 4자리 숫자이고 월이 영문 약어인 경우만 파싱
                val year = yearStr.toIntOrNull() ?: continue
                if (year < 2000 || year > 2100) continue
                if (monthStr !in months) continue
                val day = dayStr.toIntOrNull() ?: continue
                if (day < 1 || day > 31) continue

                // parts[3]=RadioFlux, parts[4]=AIndex, parts[5]=KpIndex
                val kp = parts[5].toDoubleOrNull() ?: continue
                val ap = parts[4].toIntOrNull() ?: continue

                results.add(
                    Kp27DayForecast(
                        date = "$year $monthStr $day",
                        kp = kp,
                        ap = ap
                    )
                )
            }
        }

        return results
    }

    /**
     * 캐시 초기화
     */
    fun clearCache() {
        cachedCurrentKp = null
        cachedForecast = null
        cached27DayForecast = null
        lastFetchTime = 0L
        last27DayFetchTime = 0L
    }

    private fun isCacheValid(): Boolean {
        return System.currentTimeMillis() - lastFetchTime < CACHE_DURATION_MS
    }

    /**
     * GFZ Potsdam 텍스트 데이터 파싱
     *
     * 텍스트 파일 형식:
     * - # 으로 시작하는 줄은 주석
     * - 공백으로 구분된 컬럼, Kp 값은 7번째 인덱스(0-based)
     * - 역순 검색으로 가장 최근 데이터 사용
     */
    private suspend fun fetchFromGfz(): KpIndexData? {
        val response = gfzApi.getKpNowcast()
        val text = response.string()

        val lines = text.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }

        // 역순으로 유효한 데이터 찾기
        for (line in lines.reversed()) {
            try {
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 8) {
                    val year = parts[0]
                    val month = parts[1]
                    val day = parts[2]
                    val hour = parts[3]

                    val kpStr = parts[7]
                    val kp = kpStr.toDoubleOrNull() ?: continue

                    // 유효하지 않은 Kp 값 (-1 등) 건너뛰기
                    if (kp < 0) continue

                    val timeTag = "$year-$month-$day ${hour}:00:00"
                    return KpIndexData(
                        timeTag = timeTag,
                        kp = kp,
                        observed = "observed"
                    )
                }
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * NOAA SWPC JSON 예보 데이터 로드.
     *
     * 응답은 객체 배열 `[{"time_tag":"...","kp":1.0,"observed":"observed","noaa_scale":"0"}, ...]`.
     * Retrofit + Moshi 가 [KpForecastItemDto] 로 자동 디코딩하므로 수동 파싱이 불필요.
     *
     * 옛 응답은 2D 배열(`[["time_tag","kp",...],["2026-02-24...","2.33",...]]`) 형식이었으나
     * NOAA 가 객체 배열로 전환했다. iOS 의 [ForecastAPIResponse] 와 동일한 스키마.
     */
    private suspend fun fetchForecastFromNoaa(): List<KpIndexData> {
        return noaaApi.getKpForecast().map { item ->
            KpIndexData(
                timeTag = item.timeTag,
                kp = item.kp,
                observed = item.observed,
            )
        }
    }
}
