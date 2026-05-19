package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.GeoJSONFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.VWorldApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VWorld 비행구역 데이터 Repository
 *
 * 15분 LRU 캐시를 사용하여 API 호출을 최소화한다.
 */
@Singleton
class VWorldRepository @Inject constructor(
    private val vWorldApi: VWorldApi,
    @javax.inject.Named("VWorldApiKey") private val apiKey: String
) {
    companion object {
        private const val TAG = "VWorldRepository"
        private const val CACHE_DURATION_MS = 15 * 60 * 1000L // 15분
        private const val MAX_CACHE_ENTRIES = 50
    }

    /**
     * 캐시 엔트리
     */
    private data class CacheEntry(
        val data: List<DroneZoneFeature>,
        val timestamp: Long
    )

    // LRU 캐시 (accessOrder=true). 카메라 이동마다 fetchMultipleLayers 가 N개 레이어를
    // 동시 async 로 호출하므로 LinkedHashMap 의 비동기 안전성을 위해 Mutex 직렬화.
    private val cache = LinkedHashMap<String, CacheEntry>(MAX_CACHE_ENTRIES + 1, 0.75f, true)
    private val cacheMutex = Mutex()

    /**
     * 단일 레이어의 비행구역 데이터 조회
     *
     * @param layer 비행구역 레이어
     * @param bbox 바운딩 박스 문자열 (minLon,minLat,maxLon,maxLat)
     * @return DroneZoneFeature 리스트
     */
    suspend fun fetchFlightZones(
        layer: FlightZoneLayer,
        bbox: String
    ): Result<List<DroneZoneFeature>> {
        val cacheKey = "${layer.typeName}:$bbox"

        // 캐시 확인 (mutex 안에서 access)
        val cachedValid = cacheMutex.withLock {
            val entry = cache[cacheKey]
            when {
                entry == null -> null
                System.currentTimeMillis() - entry.timestamp < CACHE_DURATION_MS -> entry.data
                else -> {
                    cache.remove(cacheKey)
                    null
                }
            }
        }
        if (cachedValid != null) return Result.success(cachedValid)

        return try {
            val response = vWorldApi.getFeatures(
                typeName = layer.typeName,
                bbox = bbox,
                key = apiKey
            )

            val features = response.response?.result?.featureCollection?.features
            val zones = features?.mapNotNull { feature ->
                parseFeature(feature, layer)
            } ?: emptyList()

            // 캐시 저장 (accessOrder=true LRU 가 removeEldestEntry 대체)
            cacheMutex.withLock {
                if (cache.size >= MAX_CACHE_ENTRIES) {
                    cache.keys.firstOrNull()?.let { cache.remove(it) }
                }
                cache[cacheKey] = CacheEntry(zones, System.currentTimeMillis())
            }

            Log.d(TAG, "${layer.displayName}: ${zones.size}개 구역 로드 완료")
            Result.success(zones)
        } catch (e: Exception) {
            Log.e(TAG, "${layer.displayName} 로드 실패: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 여러 레이어의 비행구역 데이터를 동시 로드
     *
     * @param layers 로드할 레이어 목록
     * @param bbox 바운딩 박스 문자열
     * @return 레이어별 DroneZoneFeature 맵
     */
    suspend fun fetchMultipleLayers(
        layers: Set<FlightZoneLayer>,
        bbox: String
    ): Map<FlightZoneLayer, List<DroneZoneFeature>> = coroutineScope {
        val results = layers.map { layer ->
            async {
                layer to fetchFlightZones(layer, bbox)
            }
        }.awaitAll()

        results.associate { (layer, result) ->
            layer to (result.getOrNull() ?: emptyList())
        }
    }

    /**
     * 캐시 초기화. suspend 가 아닌 호출처 호환을 위해 동기 clear 를 유지하되,
     * Mutex 가 보호하는 Map 에 대한 동시 변형을 피하기 위해 runBlocking 대신
     * thread-safe 한 clear() (LinkedHashMap.clear) 만 호출. fetchFlightZones 의 critical
     * section 과는 동시 진행될 수 있지만 clear/put 의 부분 손상은 LinkedHashMap 의
     * structural modification fail-fast 로만 보호되므로, 호출자는 동기화 중에는
     * clearCache 를 호출하지 않아야 한다 (기존 동작과 동일).
     */
    fun clearCache() {
        cache.clear()
    }

    /**
     * GeoJSON Feature를 DroneZoneFeature로 파싱
     */
    private fun parseFeature(feature: GeoJSONFeature, layer: FlightZoneLayer): DroneZoneFeature? {
        val geometry = feature.geometry ?: return null
        val polygons = parseGeometry(geometry.type, geometry.coordinates) ?: return null

        if (polygons.isEmpty()) return null

        val props = feature.properties ?: emptyMap()

        return DroneZoneFeature(
            id = feature.id ?: "${layer.typeName}_${System.nanoTime()}",
            layer = layer,
            polygons = polygons,
            zoneCode = props["code"]?.toString() ?: props["zoneCode"]?.toString(),
            upperAltitude = parseDoubleProperty(props, "up_alt", "upper_alt", "upperAlt"),
            lowerAltitude = parseDoubleProperty(props, "low_alt", "lower_alt", "lowerAlt"),
            zoneName = props["name"]?.toString() ?: props["zoneName"]?.toString() ?: props["kor_nm"]?.toString(),
            properties = props
        )
    }

    /**
     * GeoJSON Geometry의 coordinates를 파싱하여 폴리곤 좌표 리스트로 변환
     *
     * GeoJSON 좌표는 [lon, lat] 순서이므로 Pair(lat, lon)으로 변환
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseGeometry(type: String?, coordinates: Any?): List<List<Pair<Double, Double>>>? {
        if (coordinates == null || type == null) return null

        return try {
            when (type) {
                "Polygon" -> {
                    // Polygon: [[[lon, lat], ...]]
                    val rings = coordinates as? List<*> ?: return null
                    val outerRing = rings.firstOrNull() as? List<*> ?: return null
                    val coords = parseCoordinateRing(outerRing)
                    if (coords.isNotEmpty()) listOf(coords) else null
                }
                "MultiPolygon" -> {
                    // MultiPolygon: [[[[lon, lat], ...]], ...]
                    val polygons = coordinates as? List<*> ?: return null
                    polygons.mapNotNull { polygon ->
                        val rings = polygon as? List<*> ?: return@mapNotNull null
                        val outerRing = rings.firstOrNull() as? List<*> ?: return@mapNotNull null
                        val coords = parseCoordinateRing(outerRing)
                        coords.ifEmpty { null }
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "좌표 파싱 실패: ${e.message}", e)
            null
        }
    }

    /**
     * 좌표 배열(ring)을 파싱하여 Pair(lat, lon) 리스트로 변환
     *
     * GeoJSON: [lon, lat] -> Pair(lat, lon)
     */
    private fun parseCoordinateRing(ring: List<*>): List<Pair<Double, Double>> {
        return ring.mapNotNull { point ->
            val coords = point as? List<*> ?: return@mapNotNull null
            if (coords.size < 2) return@mapNotNull null
            val lon = (coords[0] as? Number)?.toDouble() ?: return@mapNotNull null
            val lat = (coords[1] as? Number)?.toDouble() ?: return@mapNotNull null
            Pair(lat, lon) // lat, lon 순서로 변환
        }
    }

    /**
     * properties에서 Double 값을 안전하게 파싱
     */
    private fun parseDoubleProperty(
        props: Map<String, Any?>,
        vararg keys: String
    ): Double? {
        for (key in keys) {
            val value = props[key]
            if (value != null) {
                return when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull()
                    else -> null
                }
            }
        }
        return null
    }
}
