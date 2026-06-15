package com.ScienceFiction.DronePassAndroid.feature.map

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.local.PublicContactInfo
import com.ScienceFiction.DronePassAndroid.core.data.local.VWorldContactManager
import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.VWorldServiceException
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.GeocodingRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.VWorldRepository
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.util.FlightZoneCalculator
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneSelectionState
import com.ScienceFiction.DronePassAndroid.feature.drone.filterShapesForSelectedDrones
import com.ScienceFiction.DronePassAndroid.feature.drone.selectedDronesForIosDropdown
import com.ScienceFiction.DronePassAndroid.feature.settings.SettingsPreferenceKeys
import com.ScienceFiction.DronePassAndroid.feature.settings.defaultKoreaFeaturesEnabled
import com.ScienceFiction.DronePassAndroid.feature.settings.resolveCurrentAppLanguage
import com.ScienceFiction.DronePassAndroid.feature.settings.resolveKoreaFeaturesEnabled
import com.ScienceFiction.DronePassAndroid.feature.settings.storedHideExpiredShapes
import com.ScienceFiction.DronePassAndroid.feature.settings.storedHideNotStartedShapes
import com.ScienceFiction.DronePassAndroid.feature.settings.storedKeepScreenAwake
import com.ScienceFiction.DronePassAndroid.feature.settings.storedKoreaFeaturesEnabled
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeEditDefaults
import com.ScienceFiction.DronePassAndroid.feature.shape.resolveShapeEditConflict
import com.ScienceFiction.DronePassAndroid.feature.shape.shapeEditSaveFailureMessage
import com.ScienceFiction.DronePassAndroid.feature.shape.storedShapeEditDefaults
import com.ScienceFiction.DronePassAndroid.feature.shape.writeShapeEditDateOnlyMode
import com.ScienceFiction.DronePassAndroid.feature.shape.writeShapeEditDefaults
import com.ScienceFiction.DronePassAndroid.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val ShapeFocusDefaultRadiusMeters = 100.0
internal const val CameraEventReplay = 0
internal const val MapHighlightClearEventReplay = 0

internal data class MapViewportBounds(
    val southWestLatitude: Double,
    val southWestLongitude: Double,
    val northEastLatitude: Double,
    val northEastLongitude: Double,
)

internal fun calculateShapeFocusZoomLevel(radius: Double): Double {
    val minRadius = 100.0
    val maxRadius = 3000.0
    val minZoom = 11.0
    val maxZoom = 14.0
    if (radius <= minRadius) return maxZoom
    if (radius >= maxRadius) return minZoom
    return maxZoom - ((radius - minRadius) * (maxZoom - minZoom) / (maxRadius - minRadius))
}

internal fun calculateShapeFocusCoordinate(shape: ShapeModel): Coordinate {
    return shape.baseCoordinate
}

internal fun calculateShapeFocusRadiusMeters(shape: ShapeModel): Double {
    return shape.radius ?: ShapeFocusDefaultRadiusMeters
}

internal fun shouldConsumeNaverMapSymbolTap(): Boolean = true

internal fun shouldSkipShapeFocusMove(
    currentSelectedShape: ShapeModel?,
    targetShape: ShapeModel,
): Boolean {
    return currentSelectedShape?.baseCoordinate == targetShape.baseCoordinate
}

internal fun resolveShapeFocusCameraEvent(
    currentSelectedShape: ShapeModel?,
    targetShape: ShapeModel,
    skipIfAlreadyFocused: Boolean,
): CameraEvent.MoveToShape? {
    if (
        skipIfAlreadyFocused &&
        shouldSkipShapeFocusMove(currentSelectedShape, targetShape)
    ) {
        return null
    }

    val focusCoordinate = calculateShapeFocusCoordinate(targetShape)
    return CameraEvent.MoveToShape(
        coordinate = focusCoordinate,
        zoom = calculateShapeFocusZoomLevel(calculateShapeFocusRadiusMeters(targetShape)),
    )
}

internal data class ShapeOverlayTapAction(
    val shapeId: String,
)

internal fun resolveShapeOverlayTapAction(
    tappedShape: ShapeModel?,
): ShapeOverlayTapAction? {
    val shape = tappedShape ?: return null
    return ShapeOverlayTapAction(
        shapeId = shape.id,
    )
}

internal fun shouldConsumeMissingMapShapeRequest(activeShapes: List<ShapeModel>): Boolean =
    activeShapes.isNotEmpty()

internal fun resolvePendingMapShapeRequestTarget(
    shapeId: String,
    visibleShapes: List<ShapeModel>,
): ShapeModel? {
    return visibleShapes.firstOrNull { it.id == shapeId }
}

enum class NewShapeConfirmDialogType {
    CONFIRM,
    GEOCODING_FAILED,
}

data class PendingNewShapeRequest(
    val coordinate: Coordinate,
    val address: String?,
    val dialogType: NewShapeConfirmDialogType,
)

internal fun pendingNewShapeRequestForReverseGeocodeResult(
    coordinate: Coordinate,
    address: String?,
): PendingNewShapeRequest {
    return PendingNewShapeRequest(
        coordinate = coordinate,
        address = address,
        dialogType = if (address == null) {
            NewShapeConfirmDialogType.GEOCODING_FAILED
        } else {
            NewShapeConfirmDialogType.CONFIRM
        },
    )
}

internal fun resolvePendingNewShapeAddress(
    request: PendingNewShapeRequest,
    addressNotFoundFallback: String,
): String = request.address ?: addressNotFoundFallback

internal fun filterShapesByMapVisibilitySettings(
    shapes: List<ShapeModel>,
    hideExpired: Boolean,
    hideNotStarted: Boolean,
    now: Long = System.currentTimeMillis(),
): List<ShapeModel> {
    val notStartedFilteredShapes = if (hideNotStarted) {
        shapes.filter { !isMapShapeNotStarted(it.flightStartDate, now) }
    } else {
        shapes
    }

    return if (hideExpired) {
        notStartedFilteredShapes.filter { !isMapShapeExpired(it.flightEndDate, now) }
    } else {
        notStartedFilteredShapes
    }
}

internal fun isMapShapeExpired(
    flightEndDateMillis: Long?,
    now: Long = System.currentTimeMillis(),
): Boolean {
    return flightEndDateMillis?.let { it < now } ?: false
}

internal fun isMapShapeNotStarted(
    flightStartDateMillis: Long,
    now: Long = System.currentTimeMillis(),
): Boolean {
    return flightStartDateMillis > now
}

internal fun <T> filterFlightZoneCacheForVisibleLayers(
    current: Map<FlightZoneLayer, T>,
    visibleLayers: Set<FlightZoneLayer>,
): Map<FlightZoneLayer, T> {
    if (visibleLayers.isEmpty()) return emptyMap()
    return current.filterKeys { it in visibleLayers }
}

internal fun resolveInitialVisibleFlightZoneLayers(
    koreaFeaturesEnabled: Boolean,
    storedLayerIds: Set<String>?,
): Set<FlightZoneLayer> {
    if (!koreaFeaturesEnabled || storedLayerIds.isNullOrEmpty()) return emptySet()
    return storedLayerIds.mapNotNull { storedId ->
        FlightZoneLayer.entries.firstOrNull { layer ->
            layer.typeName == storedId || layer.name == storedId
        }
    }.toSet()
}

internal fun shouldApplyFlightZoneLayerResult(
    layer: FlightZoneLayer,
    currentVisibleLayers: Set<FlightZoneLayer>,
): Boolean = layer in currentVisibleLayers

internal enum class ShapeEditPostSaveAction {
    CLOSE,
    FOCUS_SAVED_LIST,
    RETURN_TO_DETAIL,
}

internal fun resolveShapeEditPostSaveAction(
    isDuplicate: Boolean,
    focusAfterSave: Boolean,
    returnToDetailAfterEditSave: Boolean,
): ShapeEditPostSaveAction {
    return when {
        focusAfterSave -> ShapeEditPostSaveAction.FOCUS_SAVED_LIST
        !isDuplicate && returnToDetailAfterEditSave -> ShapeEditPostSaveAction.RETURN_TO_DETAIL
        else -> ShapeEditPostSaveAction.CLOSE
    }
}

internal fun shouldReturnToShapeDetailAfterEditDismiss(
    selectedShapeId: String?,
    returnToDetailAfterEditDismiss: Boolean,
): Boolean {
    return selectedShapeId != null && returnToDetailAfterEditDismiss
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val shapeRepository: ShapeRepository,
    private val vWorldRepository: VWorldRepository,
    private val vWorldContactManager: VWorldContactManager,
    private val droneRepository: DroneRepository,
    private val geocodingRepository: GeocodingRepository,
    val naverGeocodingApi: NaverGeocodingApi,
    private val dataStore: DataStore<Preferences>,
    private val droneSelectionState: DroneSelectionState,
    private val analyticsLogger: AnalyticsLogger,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private var hasCheckedLegacyShapeMigration = false
    private var returnToShapeDetailAfterEditSave = false
    private var returnToShapeDetailAfterEditDismiss = false
    private var pendingNewShapeRequestGeneration = 0L

    init {
        // 공공기관 연락처 사전로딩 (5일 캐시, 실패해도 무시 — 오프라인 fallback 내장).
        // 사전협의/국립공원 상세 시트에서 기관명으로 lookup 한다.
        viewModelScope.launch { vWorldContactManager.ensureLoaded() }
        viewModelScope.launch { ensureDefaultDroneIfNeeded() }
        // 한국 특화 기능 변경 시 레이어 해제는 koreaFeaturesEnabled 선언 후의
        // 두 번째 init 블록에서 처리한다 (참조 순서 NPE 방지).
    }

    private suspend fun ensureDefaultDroneIfNeeded() {
        try {
            droneRepository.ensureDefaultDroneIfNeeded()
        } catch (e: Exception) {
            Log.w(TAG, "기본 드론 자동 생성 실패", e)
        }
    }

    /**
     * 공공기관 연락처. VWorldZoneDetailSheet 의 `findContact(zoneName)` 에 사용.
     */
    val vWorldContacts: StateFlow<Map<String, PublicContactInfo>> = vWorldContactManager.contacts

    /** 구역명으로 공공기관 연락처 검색 (정확 → 부분 일치). */
    fun findContact(name: String?): PublicContactInfo? = vWorldContactManager.findContact(name)

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
     * 복제 모드 플래그 — ShapeDetailSheet 의 "복제" 메뉴 → ShapeEditScreen 진입 시 true.
     * ShapeEditScreen 은 iOS처럼 원본 입력값을 유지하되 신규 UUID + 신규 createdAt 로 저장한다.
     * 저장/취소 후 자동 리셋 (false).
     */
    private val _isDuplicateMode = MutableStateFlow(false)
    val isDuplicateMode: StateFlow<Boolean> = _isDuplicateMode.asStateFlow()

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

    init {
        viewModelScope.launch {
            activeDrones.collect { drones ->
                droneSelectionState.syncActiveDrones(drones)
                migrateLegacyShapesIfNeeded(drones)
            }
        }
    }

    private suspend fun migrateLegacyShapesIfNeeded(drones: List<DroneModel>) {
        if (hasCheckedLegacyShapeMigration) return
        val firstDroneId = drones.firstOrNull()?.id ?: return

        hasCheckedLegacyShapeMigration = true
        try {
            shapeRepository.migrateLegacyShapesToDrone(firstDroneId)
        } catch (e: Exception) {
            Log.w(TAG, "레거시 도형 드론 연결 실패", e)
        }
    }

    /**
     * droneId → droneName Map. ShapeDetailSheet 의 O(1) 조회용.
     *
     * SharingStarted.Eagerly + droneRepository 직접 구독: MapScreen 은 이 StateFlow 를
     * 화면에서 collectAsStateWithLifecycle 로 구독하지 않고 [getDroneName] 으로 `.value` 만
     * 즉시 읽기 때문에, activeDrones(WhileSubscribed) 를 거치면 collector 부재로 빈 맵이 반환되어
     * ShapeDetailSheet 에서 항상 "연결된 드론 없음"이 표시된다. ViewModel 생성 즉시 활성화한다.
     */
    val droneNameById: StateFlow<Map<String, String>> = droneRepository.getActiveDrones()
        .map { drones -> drones.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /**
     * droneId → DroneModel Map. ShapeDetailSheet 가 색상 원 + 이름 + 삭제됨/미할당 상태 분기에 사용.
     * iOS `connectedDrone` 정합.
     */
    val droneById: StateFlow<Map<String, DroneModel>> = droneRepository.getActiveDrones()
        .map { drones -> drones.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /**
     * 드론 ID 로 이름 조회. droneId 가 null 이거나 매칭이 없으면 null 반환.
     * 호출자(예: MapScreenLayers 의 ShapeDetailSheet) 가 null 일 때 fallback 텍스트 표시.
     */
    fun getDroneName(droneId: String?): String? {
        if (droneId == null) return null
        return droneNameById.value[droneId]
    }

    /**
     * 드론 ID 로 DroneModel 조회. ShapeDetailSheet 의 드론 3상태(정상/삭제됨/미할당) 분기에 사용.
     * iOS `connectedDrone` 정합 — droneId != null 이지만 결과가 null 이면 "삭제된 드론" 상태.
     */
    fun getDroneById(droneId: String?): DroneModel? {
        if (droneId == null) return null
        return droneById.value[droneId]
    }

    /**
     * 지도에서 선택된 드론 ID Set (드론 선택 드롭다운용)
     */
    val selectedDroneIds: StateFlow<Set<String>> = droneSelectionState.selectedDroneIds

    /**
     * 강조 표시된 드론 ID Set (선택된 드론 칩 탭 시)
     */
    val highlightedDroneIds: StateFlow<Set<String>> = droneSelectionState.highlightedDroneIds

    /**
     * 선택된 드론 목록 (activeDrones + selectedDroneIds 조합)
     */
    val selectedDrones: StateFlow<List<DroneModel>> = combine(
        activeDrones,
        selectedDroneIds
    ) { drones, selectedIds ->
        selectedDronesForIosDropdown(drones, selectedIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 새 도형 생성 기본 드론 후보.
     * iOS `DroneManager.selectedDroneId` 정합: 지도 필터 체크박스 선택과 독립된 단일
     * 선택 드론을 우선 사용하고, 유효하지 않으면 첫 활성 드론으로 fallback 한다.
     */
    val primarySelectedDroneId: StateFlow<String?> = combine(
        activeDrones,
        droneSelectionState.selectedDroneId,
    ) { drones, selectedDroneId ->
        selectedDroneId?.takeIf { id -> drones.any { it.id == id } }
            ?: drones.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * 드론 선택/해제 토글
     */
    fun toggleDroneSelection(droneId: String) {
        droneSelectionState.toggleDroneSelection(droneId)
    }

    /**
     * 드론 강조 토글 (선택된 드론 칩 탭 시)
     */
    fun toggleDroneHighlight(droneId: String) {
        droneSelectionState.toggleDroneHighlight(droneId)
    }

    /**
     * 역지오코딩으로 변환된 주소 (새 도형 생성 시 사용)
     */
    private val _reverseGeocodedAddress = MutableStateFlow<String?>(null)
    val reverseGeocodedAddress: StateFlow<String?> = _reverseGeocodedAddress.asStateFlow()

    private val _pendingNewShapeRequest = MutableStateFlow<PendingNewShapeRequest?>(null)
    val pendingNewShapeRequest: StateFlow<PendingNewShapeRequest?> =
        _pendingNewShapeRequest.asStateFlow()

    /**
     * 카메라 이동 이벤트 (SharedFlow로 1회성 이벤트 처리).
     *
     * iOS NotificationCenter 의 MoveToShapeNotification 처럼 과거 이벤트를 재생하지 않는다.
     * 지도 준비 전 포커스 요청은 MapScreen 의 pending shape id 경로에서 mapReady 이후 처리한다.
     */
    private val _cameraEvent = MutableSharedFlow<CameraEvent>(replay = CameraEventReplay)
    val cameraEvent: SharedFlow<CameraEvent> = _cameraEvent.asSharedFlow()

    private val _clearMapHighlightEvent = MutableSharedFlow<Unit>(
        replay = MapHighlightClearEventReplay,
        extraBufferCapacity = 1,
    )
    val clearMapHighlightEvent: SharedFlow<Unit> = _clearMapHighlightEvent.asSharedFlow()

    private val _savedShapeFocusEvent = MutableSharedFlow<String>()
    val savedShapeFocusEvent: SharedFlow<String> = _savedShapeFocusEvent.asSharedFlow()

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
     * 지도 오버레이 탭 시 호출한다.
     * iOS ShapeOverlayTapped 흐름처럼 상세 시트를 바로 열지 않고 저장 목록을 열어 해당 행으로 포커스한다.
     */
    fun onShapeOverlayTapped(shapeId: String) {
        val shape = activeShapes.value.firstOrNull { it.id == shapeId }
        val action = resolveShapeOverlayTapAction(shape) ?: return

        _selectedShapeId.value = action.shapeId
        _showShapeDetail.value = false
        viewModelScope.launch {
            _savedShapeFocusEvent.emit(action.shapeId)
        }
    }

    /**
     * 저장 목록 탭에서 도형을 고를 때 사용한다.
     * iOS SavedTableListView.handleShapeTap 처럼 상세 시트는 열지 않고 지도 하이라이트만 갱신한다.
     */
    fun selectShapeForMapFocus(shapeId: String) {
        _selectedShapeId.value = shapeId
        _showShapeDetail.value = false
    }

    /**
     * 도형 선택 해제
     */
    fun clearSelection() {
        _selectedShapeId.value = null
        _showShapeDetail.value = false
    }

    /**
     * iOS `ClearMapOverlays` 알림 정합.
     * 로그아웃/탈퇴 시 로컬 도형은 보존하고 지도 하이라이트/상세 선택만 정리한다.
     */
    fun clearMapHighlightForAccountSessionEnd() {
        clearSelection()
        _clearMapHighlightEvent.tryEmit(Unit)
    }

    /**
     * 새 도형 생성 요청 (FAB 클릭 시 호출).
     * iOS plusButtonView 와 동일하게 지도 중심 좌표만 넘기고 주소는 비워둔다.
     */
    fun onCreateShapeRequested(coordinate: Coordinate) {
        resetShapeSelectionForNewShapeEdit()
        pendingNewShapeRequestGeneration++
        _pendingNewShapeRequest.value = null
        _newShapeCoordinate.value = coordinate
        _reverseGeocodedAddress.value = null
        _showShapeEdit.value = true
    }

    /**
     * 지도 롱프레스로 새 도형 생성 요청.
     * iOS MainView.handleLongPress 처럼 역지오코딩 후 확인창을 거쳐 편집 시트를 연다.
     */
    fun onCreateShapeAtCoordinate(coordinate: Coordinate) {
        resetShapeSelectionForNewShapeEdit()
        val requestGeneration = ++pendingNewShapeRequestGeneration
        _pendingNewShapeRequest.value = null
        _newShapeCoordinate.value = coordinate
        _reverseGeocodedAddress.value = null
        _showShapeEdit.value = false
        prepareLongPressNewShapeRequest(coordinate, requestGeneration)
    }

    private fun prepareLongPressNewShapeRequest(
        coordinate: Coordinate,
        requestGeneration: Long,
    ) {
        viewModelScope.launch {
            val result = geocodingRepository.reverseGeocode(
                latitude = coordinate.latitude,
                longitude = coordinate.longitude
            )
            result.onSuccess { address ->
                if (requestGeneration != pendingNewShapeRequestGeneration) return@launch
                _pendingNewShapeRequest.value = pendingNewShapeRequestForReverseGeocodeResult(
                    coordinate = coordinate,
                    address = address,
                )
            }.onFailure { error ->
                if (requestGeneration != pendingNewShapeRequestGeneration) return@launch
                Log.e(TAG, "역지오코딩 실패: ${error.message}", error)
                _pendingNewShapeRequest.value = pendingNewShapeRequestForReverseGeocodeResult(
                    coordinate = coordinate,
                    address = null,
                )
            }
        }
    }

    fun confirmPendingNewShapeRequest(addressNotFoundFallback: String) {
        val request = _pendingNewShapeRequest.value ?: return
        resetShapeSelectionForNewShapeEdit()
        pendingNewShapeRequestGeneration++
        _newShapeCoordinate.value = request.coordinate
        _reverseGeocodedAddress.value = resolvePendingNewShapeAddress(
            request = request,
            addressNotFoundFallback = addressNotFoundFallback,
        )
        _pendingNewShapeRequest.value = null
        _showShapeEdit.value = true
    }

    private fun resetShapeSelectionForNewShapeEdit() {
        _selectedShapeId.value = null
        _showShapeDetail.value = false
        _isDuplicateMode.value = false
        returnToShapeDetailAfterEditSave = false
        returnToShapeDetailAfterEditDismiss = false
    }

    fun cancelPendingNewShapeRequest() {
        pendingNewShapeRequestGeneration++
        _pendingNewShapeRequest.value = null
        _newShapeCoordinate.value = null
        _reverseGeocodedAddress.value = null
        _showShapeEdit.value = false
    }

    /**
     * 도형 편집 요청
     */
    fun onEditShapeRequested(
        shape: ShapeModel,
        returnToDetailAfterSave: Boolean = false,
    ) {
        _selectedShapeId.value = shape.id
        _isDuplicateMode.value = false
        returnToShapeDetailAfterEditSave = returnToDetailAfterSave
        returnToShapeDetailAfterEditDismiss = returnToDetailAfterSave
        _showShapeDetail.value = false
        _showShapeEdit.value = true
    }

    /**
     * 도형 복제 요청 — 편집과 동일하지만 isDuplicateMode=true 로 진입.
     * ShapeEditScreen 은 원본 제목/입력값을 유지한 채 신규 UUID 로 저장한다.
     */
    fun onDuplicateRequested(
        shape: ShapeModel,
        returnToDetailAfterDismiss: Boolean = false,
    ) {
        _selectedShapeId.value = shape.id
        _isDuplicateMode.value = true
        returnToShapeDetailAfterEditSave = false
        returnToShapeDetailAfterEditDismiss = returnToDetailAfterDismiss
        _showShapeDetail.value = false
        _showShapeEdit.value = true
    }

    /**
     * 도형 저장 (생성/수정)
     * updatedAt을 현재 시각으로 갱신하여 저장
     */
    fun saveShape(
        shape: ShapeModel,
        isDuplicate: Boolean = false,
        focusAfterSave: Boolean = false,
        originalShapeAtEditStart: ShapeModel? = null,
        onFailure: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val latestShape = if (!isDuplicate && originalShapeAtEditStart != null) {
                    shapeRepository.getShapeById(originalShapeAtEditStart.id)
                } else {
                    null
                }
                val resolvedShape = resolveShapeEditConflict(
                    editedShape = shape,
                    originalShape = originalShapeAtEditStart,
                    latestShape = latestShape,
                )
                val updatedShape = resolvedShape.copy(updatedAt = System.currentTimeMillis())
                shapeRepository.insertShape(updatedShape)
                val postSaveAction = resolveShapeEditPostSaveAction(
                    isDuplicate = isDuplicate,
                    focusAfterSave = focusAfterSave,
                    returnToDetailAfterEditSave = returnToShapeDetailAfterEditSave,
                )
                when {
                    isDuplicate -> analyticsLogger.logShapeDuplicated()
                    focusAfterSave -> analyticsLogger.logShapeCreated(shape.shapeType.rawValue)
                }
                _showShapeEdit.value = false
                _isDuplicateMode.value = false
                _newShapeCoordinate.value = null
                _reverseGeocodedAddress.value = null
                _pendingNewShapeRequest.value = null
                returnToShapeDetailAfterEditSave = false
                returnToShapeDetailAfterEditDismiss = false

                when (postSaveAction) {
                    ShapeEditPostSaveAction.FOCUS_SAVED_LIST -> {
                        _selectedShapeId.value = updatedShape.id
                        _showShapeDetail.value = false
                        _savedShapeFocusEvent.emit(updatedShape.id)
                        _cameraEvent.emit(
                            CameraEvent.MoveToShape(
                                coordinate = calculateShapeFocusCoordinate(updatedShape),
                                zoom = calculateShapeFocusZoomLevel(calculateShapeFocusRadiusMeters(updatedShape)),
                            ),
                        )
                    }
                    ShapeEditPostSaveAction.RETURN_TO_DETAIL -> {
                        _selectedShapeId.value = updatedShape.id
                        _showShapeDetail.value = true
                    }
                    ShapeEditPostSaveAction.CLOSE -> Unit
                }
            } catch (error: Exception) {
                Log.e(TAG, "도형 저장 실패", error)
                onFailure(
                    shapeEditSaveFailureMessage(
                        isEditMode = !isDuplicate && originalShapeAtEditStart != null,
                        localizedMessage = error.localizedMessage,
                        fallback = appContext.getString(R.string.common_unknown_error),
                        addFailureFormat = appContext.getString(R.string.shape_edit_error_add_failed),
                        updateFailureFormat = appContext.getString(R.string.shape_edit_error_update_failed),
                    ),
                )
            }
        }
    }

    fun saveShapeEditDefaults(defaults: ShapeEditDefaults) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences.writeShapeEditDefaults(defaults)
            }
        }
    }

    fun setShapeEditDateOnlyMode(isDateOnly: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences.writeShapeEditDateOnlyMode(isDateOnly)
            }
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
            returnToShapeDetailAfterEditSave = false
            returnToShapeDetailAfterEditDismiss = false
            clearSelection()
            _showShapeEdit.value = false
        }
    }

    /**
     * 도형 위치로 카메라 이동
     * 도형의 반경에 따라 줌 레벨을 자동 계산
     */
    fun moveCameraToShape(
        shape: ShapeModel,
        skipIfAlreadyFocused: Boolean = false,
    ) {
        val event = resolveShapeFocusCameraEvent(
            currentSelectedShape = selectedShape.value,
            targetShape = shape,
            skipIfAlreadyFocused = skipIfAlreadyFocused,
        ) ?: return

        viewModelScope.launch {
            _cameraEvent.emit(event)
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
        val shouldReturnToDetail = shouldReturnToShapeDetailAfterEditDismiss(
            selectedShapeId = _selectedShapeId.value,
            returnToDetailAfterEditDismiss = returnToShapeDetailAfterEditDismiss,
        )
        _showShapeEdit.value = false
        _isDuplicateMode.value = false
        _showShapeDetail.value = shouldReturnToDetail
        returnToShapeDetailAfterEditSave = false
        returnToShapeDetailAfterEditDismiss = false
        _newShapeCoordinate.value = null
        _reverseGeocodedAddress.value = null
        _pendingNewShapeRequest.value = null
    }

    /**
     * 반경 기반 줌 레벨 계산
     * 반경이 작을수록 높은 줌 레벨(확대), 클수록 낮은 줌 레벨(축소)
     *
     * @param radius 도형 반경 (미터 단위)
     * @return 계산된 줌 레벨 (11.0 ~ 14.0)
     */
    fun calculateZoomLevel(radius: Double): Double {
        return calculateShapeFocusZoomLevel(radius)
    }

    // ===== DataStore 설정값 =====

    companion object {
        private const val TAG = "MapViewModel"
        private const val DEBOUNCE_MS = 500L
        private val KEY_VISIBLE_FLIGHT_ZONE_LAYERS = stringSetPreferencesKey("vworld_visible_layers")
    }

    /**
     * 화면 항상 켜기 설정 (DataStore)
     */
    val keepScreenOn: StateFlow<Boolean> = dataStore.data
        .map { preferences -> storedKeepScreenAwake(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 만료된 도형 숨기기 설정 (DataStore)
     */
    private val hideExpiredShapes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> storedHideExpiredShapes(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 시작 전 도형 숨기기 설정 (DataStore)
     */
    private val hideNotStartedShapes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> storedHideNotStartedShapes(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 한국 특화 기능 활성화 — iOS `settingManager.isKoreaFeaturesEnabled` 정합.
     * 기본값: 미설정 시 현재 앱 언어 기준(한국어 ON, 영어 OFF).
     * ON/OFF 전이 시 아래 두 번째 init 블록이 모든 FlightZone 레이어를 해제한다.
     */
    val koreaFeaturesEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            resolveKoreaFeaturesEnabled(
                storedValue = storedKoreaFeaturesEnabled(preferences),
                language = resolveCurrentAppLanguage(appContext),
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            defaultKoreaFeaturesEnabled(appContext),
        )

    /**
     * iOS ShapeEditViewModel 의 UserDefaults 기본값 복원과 같은 값.
     * 새 도형 생성 시 최근 드론/반경/고도/시작일/종료일/날짜모드를 이어 쓴다.
     */
    val shapeEditDefaults: StateFlow<ShapeEditDefaults> = dataStore.data
        .map(::storedShapeEditDefaults)
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShapeEditDefaults())

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
    private val timeFilteredShapes: StateFlow<List<ShapeModel>> = combine(
        activeShapes,
        hideExpiredShapes,
        hideNotStartedShapes,
        expirationTicker
    ) { shapes, hideExpired, hideNotStarted, _ ->
        filterShapesByMapVisibilitySettings(
            shapes = shapes,
            hideExpired = hideExpired,
            hideNotStarted = hideNotStarted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * iOS 와 동일하게 만료/시작전 필터 후, 선택된 드론의 도형만 지도에 표시한다.
     */
    val filteredShapes: StateFlow<List<ShapeModel>> = combine(
        timeFilteredShapes,
        activeDrones,
        selectedDroneIds
    ) { shapes, drones, selectedIds ->
        filterShapesForSelectedDrones(shapes, drones, selectedIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ===== 비행구역 레이어 관리 =====

    /**
     * 현재 표시 중인 비행구역 레이어 Set
     */
    private val _visibleLayers = MutableStateFlow<Set<FlightZoneLayer>>(emptySet())
    val visibleLayers: StateFlow<Set<FlightZoneLayer>> = _visibleLayers.asStateFlow()

    /**
     * 비행구역 FAB 배지(선택된 레이어 수)용 derive.
     * iOS `FlightZoneOverlayManager.visibleLayerCount` 매핑.
     */
    val visibleLayerCount: StateFlow<Int> = _visibleLayers
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
     * 현재 지도 bbox 캐시.
     *
     * 이전 구조(`_mapBoundsFlow` + `distinctUntilChanged`)에서는 진입 직후 빈 `visibleLayers`
     * 로 한 번 emit → `loadFlightZones` 가 early-return → 사용자가 레이어 토글해 visibleLayers
     * 가 채워져도 카메라 미이동 시 동일 bbox 가 `distinctUntilChanged` 에 차단돼 영원히
     * 재호출되지 않는 버그가 있었음. visibleLayers 와 bbox 를 함께 감시하도록 일원화한다.
     */
    private val _currentMapBounds = MutableStateFlow<MapViewportBounds?>(null)
    internal val currentMapBounds: StateFlow<MapViewportBounds?> = _currentMapBounds.asStateFlow()

    // koreaFeaturesEnabled 와 FlightZone 상태 Flow 들이 모두 초기화된 뒤 시작한다.
    init {
        viewModelScope.launch { loadVisibleLayersFromStorage() }
        viewModelScope.launch {
            koreaFeaturesEnabled
                .drop(1)
                .distinctUntilChanged()
                .collect { clearFlightZoneUiForKoreaFeatureChange() }
        }
    }

    @OptIn(FlowPreview::class)
    private val flightZoneLoadCollector: Job = viewModelScope.launch {
        combine(_visibleLayers, _currentMapBounds) { layers, bounds -> layers to bounds }
            .debounce(DEBOUNCE_MS)
            .distinctUntilChanged()
            .collectLatest { (layers, bounds) ->
                if (bounds == null || layers.isEmpty()) return@collectLatest
                loadFlightZones(
                    southWestLat = bounds.southWestLatitude,
                    southWestLon = bounds.southWestLongitude,
                    northEastLat = bounds.northEastLatitude,
                    northEastLon = bounds.northEastLongitude,
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
        setVisibleLayers(current)
    }

    /**
     * 모든 레이어 표시
     */
    fun showAllLayers() {
        setVisibleLayers(FlightZoneLayer.entries.toSet())
    }

    /**
     * 모든 레이어 숨기기
     */
    fun hideAllLayers() {
        setVisibleLayers(emptySet())
    }

    private fun clearFlightZoneUiForKoreaFeatureChange() {
        hideAllLayers()
        _showLayerSelector.value = false
        _selectedZone.value = null
        _showZoneDetail.value = false
    }

    private fun setVisibleLayers(layers: Set<FlightZoneLayer>) {
        _visibleLayers.value = layers
        _flightZones.update { current -> filterFlightZoneCacheForVisibleLayers(current, layers) }
        viewModelScope.launch { saveVisibleLayersToStorage(layers) }
    }

    private suspend fun loadVisibleLayersFromStorage() {
        val storedLayerIds = dataStore.data.first()[KEY_VISIBLE_FLIGHT_ZONE_LAYERS]
        val layers = resolveInitialVisibleFlightZoneLayers(
            koreaFeaturesEnabled = koreaFeaturesEnabled.value,
            storedLayerIds = storedLayerIds,
        )
        _visibleLayers.value = layers
        _flightZones.update { current -> filterFlightZoneCacheForVisibleLayers(current, layers) }
    }

    private suspend fun saveVisibleLayersToStorage(layers: Set<FlightZoneLayer>) {
        dataStore.edit { preferences ->
            if (layers.isEmpty()) {
                preferences.remove(KEY_VISIBLE_FLIGHT_ZONE_LAYERS)
            } else {
                preferences[KEY_VISIBLE_FLIGHT_ZONE_LAYERS] = layers.map { it.typeName }.toSet()
            }
        }
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
     * 지도 이동 시 현재 bbox 를 캐시한다.
     * 실제 비행구역 로드는 [flightZoneLoadCollector] 가 visibleLayers 와 함께 감시하여
     * 둘 중 하나만 바뀌어도 debounce 후 fetch 한다.
     */
    fun onMapBoundsChanged(
        southWestLat: Double,
        southWestLon: Double,
        northEastLat: Double,
        northEastLon: Double
    ) {
        _currentMapBounds.value = MapViewportBounds(
            southWestLatitude = southWestLat,
            southWestLongitude = southWestLon,
            northEastLatitude = northEastLat,
            northEastLongitude = northEastLon,
        )
    }

    /**
     * 비행구역 데이터 로드.
     *
     * iOS `fetchMultipleLayers(onLayerLoaded:)` 매핑: loadingPriority 가 낮은 레이어부터
     * fetch 를 시작하고, 도착하는 즉시 `_flightZones` 에 부분 반영(StateFlow.update 로 atomic 누적)
     * 하여 사용자에게 점진적 표시.
     */
    private suspend fun loadFlightZones(
        southWestLat: Double,
        southWestLon: Double,
        northEastLat: Double,
        northEastLon: Double
    ) {
        val layers = _visibleLayers.value
        if (layers.isEmpty()) {
            _flightZones.value = emptyMap()
            return
        }

        _flightZonesLoading.value = true

        val bbox = FlightZoneCalculator.createBoundingBoxFromBounds(
            southWestLat, southWestLon, northEastLat, northEastLon
        )

        try {
            // 새 fetch 라운드 시작 시 이전 결과는 클리어 (요청한 레이어 셋만 정확히 반영되도록).
            _flightZones.value = emptyMap()
            val results = vWorldRepository.fetchMultipleLayers(layers, bbox) { layer, features ->
                if (shouldApplyFlightZoneLayerResult(layer, _visibleLayers.value)) {
                    _flightZones.update { current -> current + (layer to features) }
                }
            }
            _flightZones.update { current ->
                filterFlightZoneCacheForVisibleLayers(current, _visibleLayers.value)
            }
            Log.d(TAG, "비행구역 로드 완료: ${results.values.sumOf { it.size }}개 구역")
        } catch (e: VWorldServiceException.InvalidKey) {
            // 사용자가 즉시 조치 가능한 에러. local.properties 의 VWORLD_API_KEY 점검 필요.
            Log.e(TAG, "비행구역 로드 실패 - VWorld 인증키 미등록 (${e.message})")
            _flightZonesError.emit(appContext.getString(R.string.map_flight_zones_error_invalid_key))
        } catch (e: VWorldServiceException) {
            Log.e(TAG, "비행구역 로드 실패 - VWorld 서비스 오류: ${e.message}")
            _flightZonesError.emit(appContext.getString(R.string.map_flight_zones_error_service))
        } catch (e: Exception) {
            Log.e(TAG, "비행구역 로드 실패: ${e.message}", e)
            _flightZonesError.emit(appContext.getString(R.string.map_flight_zones_error_load_failed))
        }

        _flightZonesLoading.value = false
    }
}
