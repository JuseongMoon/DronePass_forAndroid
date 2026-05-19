package com.ScienceFiction.DronePassAndroid.feature.map

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.GeocodingRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.VWorldRepository
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.util.FlightZoneCalculator
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val shapeRepository: ShapeRepository,
    private val vWorldRepository: VWorldRepository,
    private val droneRepository: DroneRepository,
    private val geocodingRepository: GeocodingRepository,
    val naverGeocodingApi: NaverGeocodingApi,
    private val dataStore: DataStore<Preferences>,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    /**
     * 지도에 표시할 활성(삭제되지 않은) 도형 목록
     */
    val activeShapes: StateFlow<List<ShapeModel>> = shapeRepository.getActiveShapes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 선택된 도형 ID (하이라이트 표시용)
     */
    private val _selectedShapeId = MutableStateFlow<String?>(null)
    val selectedShapeId: StateFlow<String?> = _selectedShapeId.asStateFlow()

    /**
     * 도형 상세 시트 표시 여부
     */
    private val _showShapeDetail = MutableStateFlow(false)
    val showShapeDetail: StateFlow<Boolean> = _showShapeDetail.asStateFlow()

    /**
     * 도형 편집 시트 표시 여부
     */
    private val _showShapeEdit = MutableStateFlow(false)
    val showShapeEdit: StateFlow<Boolean> = _showShapeEdit.asStateFlow()

    /**
     * 새 도형 생성 좌표 (FAB 클릭 시 지도 중심 좌표)
     */
    private val _newShapeCoordinate = MutableStateFlow<Coordinate?>(null)
    val newShapeCoordinate: StateFlow<Coordinate?> = _newShapeCoordinate.asStateFlow()

    /**
     * 활성(삭제되지 않은) 드론 목록
     */
    val activeDrones: StateFlow<List<DroneModel>> = droneRepository.getActiveDrones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 지도에서 선택된 드론 ID Set (드론 선택 드롭다운용)
     */
    private val _selectedDroneIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedDroneIds: StateFlow<Set<String>> = _selectedDroneIds.asStateFlow()

    /**
     * 강조 표시된 드론 ID (선택된 드론 칩 탭 시)
     */
    private val _highlightedDroneId = MutableStateFlow<String?>(null)
    val highlightedDroneId: StateFlow<String?> = _highlightedDroneId.asStateFlow()

    /**
     * 선택된 드론 목록 (activeDrones + selectedDroneIds 조합)
     */
    val selectedDrones: StateFlow<List<DroneModel>> = combine(
        activeDrones,
        _selectedDroneIds
    ) { drones, selectedIds ->
        drones.filter { it.id in selectedIds }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 드론 선택/해제 토글
     */
    fun toggleDroneSelection(droneId: String) {
        val current = _selectedDroneIds.value.toMutableSet()
        if (current.contains(droneId)) {
            current.remove(droneId)
            // 선택 해제 시 강조 상태도 제거
            if (_highlightedDroneId.value == droneId) {
                _highlightedDroneId.value = null
            }
        } else {
            current.add(droneId)
        }
        _selectedDroneIds.value = current
    }

    /**
     * 드론 강조 토글 (선택된 드론 칩 탭 시)
     */
    fun toggleDroneHighlight(droneId: String) {
        _highlightedDroneId.value = if (_highlightedDroneId.value == droneId) null else droneId
    }

    /**
     * 역지오코딩으로 변환된 주소 (새 도형 생성 시 사용)
     */
    private val _reverseGeocodedAddress = MutableStateFlow<String?>(null)
    val reverseGeocodedAddress: StateFlow<String?> = _reverseGeocodedAddress.asStateFlow()

    /**
     * 카메라 이동 이벤트 (SharedFlow로 1회성 이벤트 처리).
     *
     * replay=1: NaverMap 이 준비되기 전(naverMap==null) 에 발생한 첫 카메라 이벤트가
     * MapScreen 의 collect 가 시작되면 자동 재생되도록 보장. 이전(replay=0)에는
     * 저장 목록 → 지도 진입 직후 focusShapeId 이동이 손실되는 경우가 있었음.
     */
    private val _cameraEvent = MutableSharedFlow<CameraEvent>(replay = 1)
    val cameraEvent: SharedFlow<CameraEvent> = _cameraEvent.asSharedFlow()

    /**
     * 선택된 도형 정보
     * selectedShapeId와 activeShapes를 combine하여 파생
     */
    val selectedShape: StateFlow<ShapeModel?> = combine(
        _selectedShapeId,
        activeShapes
    ) { shapeId, shapes ->
        shapeId?.let { id -> shapes.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * 도형 선택 (오버레이 터치 시 호출)
     */
    fun onShapeSelected(shapeId: String) {
        _selectedShapeId.value = shapeId
        _showShapeDetail.value = true
    }

    /**
     * 도형 선택 해제
     */
    fun clearSelection() {
        _selectedShapeId.value = null
        _showShapeDetail.value = false
    }

    /**
     * 새 도형 생성 요청 (FAB 클릭 시 호출)
     * 역지오코딩도 함께 수행
     */
    fun onCreateShapeRequested(coordinate: Coordinate) {
        _newShapeCoordinate.value = coordinate
        _reverseGeocodedAddress.value = null
        _showShapeEdit.value = true
        performReverseGeocode(coordinate)
    }

    /**
     * 지도 롱프레스로 새 도형 생성 요청
     * 역지오코딩을 수행하여 주소를 미리 채움
     */
    fun onCreateShapeAtCoordinate(coordinate: Coordinate) {
        _newShapeCoordinate.value = coordinate
        _reverseGeocodedAddress.value = null
        _showShapeEdit.value = true
        performReverseGeocode(coordinate)
    }

    /**
     * 역지오코딩 수행 (좌표 -> 주소 변환)
     */
    private fun performReverseGeocode(coordinate: Coordinate) {
        viewModelScope.launch {
            val result = geocodingRepository.reverseGeocode(
                latitude = coordinate.latitude,
                longitude = coordinate.longitude
            )
            result.onSuccess { address ->
                _reverseGeocodedAddress.value = address
            }.onFailure { error ->
                Log.e(TAG, "역지오코딩 실패: ${error.message}", error)
                _reverseGeocodedAddress.value = null
            }
        }
    }

    /**
     * 도형 편집 요청
     */
    fun onEditShapeRequested(shape: ShapeModel) {
        _selectedShapeId.value = shape.id
        _showShapeDetail.value = false
        _showShapeEdit.value = true
    }

    /**
     * 도형 저장 (생성/수정)
     * updatedAt을 현재 시각으로 갱신하여 저장
     */
    fun saveShape(shape: ShapeModel, isDuplicate: Boolean = false) {
        viewModelScope.launch {
            val updatedShape = shape.copy(updatedAt = System.currentTimeMillis())
            shapeRepository.insertShape(updatedShape)
            if (isDuplicate) {
                analyticsLogger.logShapeDuplicated()
            } else {
                analyticsLogger.logShapeCreated(shape.shapeType.name)
            }
            _showShapeEdit.value = false
            _newShapeCoordinate.value = null
        }
    }

    /**
     * 도형 소프트 삭제
     * 삭제 후 선택 상태와 시트를 모두 초기화
     */
    fun deleteShape(shape: ShapeModel) {
        viewModelScope.launch {
            shapeRepository.softDeleteShape(shape)
            analyticsLogger.logShapeDeleted()
            clearSelection()
            _showShapeEdit.value = false
        }
    }

    /**
     * 도형 위치로 카메라 이동
     * 도형의 반경에 따라 줌 레벨을 자동 계산
     */
    fun moveCameraToShape(shape: ShapeModel) {
        viewModelScope.launch {
            val zoom = calculateZoomLevel(shape.radius ?: 500.0)
            _cameraEvent.emit(CameraEvent.MoveTo(shape.baseCoordinate, zoom))
        }
    }

    /**
     * 도형 상세 시트 닫기
     */
    fun dismissShapeDetail() {
        _selectedShapeId.value = null
        _showShapeDetail.value = false
    }

    /**
     * 도형 편집 시트 닫기
     */
    fun dismissShapeEdit() {
        _showShapeEdit.value = false
        _newShapeCoordinate.value = null
        _reverseGeocodedAddress.value = null
    }

    /**
     * 반경 기반 줌 레벨 계산
     * 반경이 작을수록 높은 줌 레벨(확대), 클수록 낮은 줌 레벨(축소)
     *
     * @param radius 도형 반경 (미터 단위)
     * @return 계산된 줌 레벨 (11.0 ~ 14.0)
     */
    fun calculateZoomLevel(radius: Double): Double {
        val minRadius = 100.0
        val maxRadius = 3000.0
        val minZoom = 11.0
        val maxZoom = 14.0
        if (radius <= minRadius) return maxZoom
        if (radius >= maxRadius) return minZoom
        return maxZoom - ((radius - minRadius) * (maxZoom - minZoom) / (maxRadius - minRadius))
    }

    // ===== DataStore 설정값 =====

    companion object {
        private const val TAG = "MapViewModel"
        private const val DEBOUNCE_MS = 500L
        private val KEY_SHOW_FLIGHT_ZONE_LAYERS = booleanPreferencesKey("show_flight_zone_layers")
        private val KEY_KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        private val KEY_HIDE_EXPIRED_SHAPES = booleanPreferencesKey("hide_expired_shapes")
        private val KEY_HIDE_NOT_STARTED_SHAPES = booleanPreferencesKey("hide_not_started_shapes")
    }

    /**
     * 비행구역 레이어 표시 설정 (DataStore)
     * 설정에서 OFF 시 비행구역 FAB 및 오버레이를 숨김
     */
    val showFlightZoneLayersSetting: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_SHOW_FLIGHT_ZONE_LAYERS] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 화면 항상 켜기 설정 (DataStore)
     */
    val keepScreenOn: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_KEEP_SCREEN_AWAKE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 만료된 도형 숨기기 설정 (DataStore)
     */
    private val hideExpiredShapes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_HIDE_EXPIRED_SHAPES] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 시작 전 도형 숨기기 설정 (DataStore)
     */
    private val hideNotStartedShapes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_HIDE_NOT_STARTED_SHAPES] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 60초 간격 tick. 시간이 흘러 isExpired/isNotStarted 가 바뀌어도 filteredShapes 가
     * 즉시 재평가되도록 한다. (이전: shapes/설정 변경 시에만 재평가되어 만료 시각이
     * 지나도 새 변경이 없으면 만료 도형이 그대로 표시될 수 있었음.)
     */
    private val expirationTicker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(60_000L)
        }
    }

    /**
     * 설정에 따라 필터링된 도형 목록 (지도 표시용)
     * hideExpiredShapes/hideNotStartedShapes 설정에 따라 만료/미시작 도형을 제외하며
     * 60초마다 만료 상태가 재평가된다.
     */
    val filteredShapes: StateFlow<List<ShapeModel>> = combine(
        activeShapes,
        hideExpiredShapes,
        hideNotStartedShapes,
        expirationTicker
    ) { shapes, hideExpired, hideNotStarted, _ ->
        shapes.filter { shape ->
            val passExpiredFilter = !hideExpired || !shape.isExpired
            val passNotStartedFilter = !hideNotStarted || !shape.isNotStarted
            passExpiredFilter && passNotStartedFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ===== 비행구역 레이어 관리 =====

    /**
     * 현재 표시 중인 비행구역 레이어 Set
     */
    private val _visibleLayers = MutableStateFlow<Set<FlightZoneLayer>>(emptySet())
    val visibleLayers: StateFlow<Set<FlightZoneLayer>> = _visibleLayers.asStateFlow()

    /**
     * 비행구역 로딩 중 여부
     */
    private val _flightZonesLoading = MutableStateFlow(false)
    val flightZonesLoading: StateFlow<Boolean> = _flightZonesLoading.asStateFlow()

    /**
     * 비행구역 로드 에러 메시지 (1회성 이벤트)
     */
    private val _flightZonesError = MutableSharedFlow<String>()
    val flightZonesError: SharedFlow<String> = _flightZonesError.asSharedFlow()

    /**
     * 레이어별 로드된 비행구역 데이터
     */
    private val _flightZones = MutableStateFlow<Map<FlightZoneLayer, List<DroneZoneFeature>>>(emptyMap())
    val flightZones: StateFlow<Map<FlightZoneLayer, List<DroneZoneFeature>>> = _flightZones.asStateFlow()

    /**
     * 선택된 비행구역 (상세 시트용)
     */
    private val _selectedZone = MutableStateFlow<DroneZoneFeature?>(null)
    val selectedZone: StateFlow<DroneZoneFeature?> = _selectedZone.asStateFlow()

    /**
     * 비행구역 레이어 선택 시트 표시 여부
     */
    private val _showLayerSelector = MutableStateFlow(false)
    val showLayerSelector: StateFlow<Boolean> = _showLayerSelector.asStateFlow()

    /**
     * 비행구역 상세 시트 표시 여부
     */
    private val _showZoneDetail = MutableStateFlow(false)
    val showZoneDetail: StateFlow<Boolean> = _showZoneDetail.asStateFlow()

    /**
     * 지도 bbox 변경을 debounce 적용해 collect 하기 위한 SharedFlow.
     * init 블록에서 debounce + distinctUntilChanged 로 자연화한다.
     */
    private val _mapBoundsFlow = MutableSharedFlow<MapBounds>(
        replay = 0,
        extraBufferCapacity = 16,
    )

    private data class MapBounds(
        val sw: Pair<Double, Double>,
        val ne: Pair<Double, Double>,
    )

    @OptIn(FlowPreview::class)
    private val mapBoundsCollector: Job = viewModelScope.launch {
        _mapBoundsFlow
            .distinctUntilChanged()
            .debounce(DEBOUNCE_MS)
            .collect { bounds ->
                loadFlightZones(
                    southWestLat = bounds.sw.first,
                    southWestLon = bounds.sw.second,
                    northEastLat = bounds.ne.first,
                    northEastLon = bounds.ne.second,
                )
            }
    }

    /**
     * 레이어 토글
     */
    fun toggleLayer(layer: FlightZoneLayer) {
        val current = _visibleLayers.value.toMutableSet()
        if (current.contains(layer)) {
            current.remove(layer)
        } else {
            current.add(layer)
        }
        _visibleLayers.value = current
    }

    /**
     * 모든 레이어 표시
     */
    fun showAllLayers() {
        _visibleLayers.value = FlightZoneLayer.entries.toSet()
    }

    /**
     * 모든 레이어 숨기기
     */
    fun hideAllLayers() {
        _visibleLayers.value = emptySet()
    }

    /**
     * 레이어 선택 시트 표시/숨기기
     */
    fun toggleLayerSelector() {
        _showLayerSelector.value = !_showLayerSelector.value
    }

    fun dismissLayerSelector() {
        _showLayerSelector.value = false
    }

    /**
     * 비행구역 상세 시트 표시
     */
    fun onZoneSelected(zone: DroneZoneFeature) {
        _selectedZone.value = zone
        _showZoneDetail.value = true
    }

    /**
     * 비행구역 상세 시트 닫기
     */
    fun dismissZoneDetail() {
        _selectedZone.value = null
        _showZoneDetail.value = false
    }

    /**
     * 지도 이동 시 bbox 를 Flow 에 emit 한다.
     * 실제 비행구역 로드는 [mapBoundsCollector] 가 distinctUntilChanged + debounce 후 호출.
     */
    fun onMapBoundsChanged(
        southWestLat: Double,
        southWestLon: Double,
        northEastLat: Double,
        northEastLon: Double
    ) {
        _mapBoundsFlow.tryEmit(
            MapBounds(
                sw = southWestLat to southWestLon,
                ne = northEastLat to northEastLon,
            )
        )
    }

    /**
     * 비행구역 데이터 로드
     */
    private suspend fun loadFlightZones(
        southWestLat: Double,
        southWestLon: Double,
        northEastLat: Double,
        northEastLon: Double
    ) {
        val layers = _visibleLayers.value
        if (layers.isEmpty()) return

        _flightZonesLoading.value = true

        val bbox = FlightZoneCalculator.createBoundingBoxFromBounds(
            southWestLat, southWestLon, northEastLat, northEastLon
        )

        try {
            val results = vWorldRepository.fetchMultipleLayers(layers, bbox)
            _flightZones.value = results
            Log.d(TAG, "비행구역 로드 완료: ${results.values.sumOf { it.size }}개 구역")
        } catch (e: Exception) {
            Log.e(TAG, "비행구역 로드 실패: ${e.message}", e)
            _flightZonesError.emit("비행구역 데이터를 불러올 수 없습니다")
        }

        _flightZonesLoading.value = false
    }
}
