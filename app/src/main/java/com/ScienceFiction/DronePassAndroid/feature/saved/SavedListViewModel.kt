package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 저장 도형 목록의 섹션 분류 결과.
 * 4개의 StateFlow 를 단일 객체로 묶어 정렬 옵션 변경 시에도 1번만 정렬/분류된다.
 */
data class SavedShapeSections(
    val activeFiltered: List<ShapeModel>,
    val notStarted: List<ShapeModel>,
    val expired: List<ShapeModel>,
    val total: Int
) {
    companion object {
        val EMPTY = SavedShapeSections(emptyList(), emptyList(), emptyList(), 0)
    }
}

@HiltViewModel
class SavedListViewModel @Inject constructor(
    private val shapeRepository: ShapeRepository,
    private val droneRepository: DroneRepository,
    dataStore: DataStore<Preferences>,
) : ViewModel() {

    companion object {
        private val KEY_KOREA_FEATURES_ENABLED = booleanPreferencesKey("korea_features_enabled")
    }

    /**
     * 한국 특화 기능 토글 — Settings 정합화 commit 에서 추가된 DataStore key 공유.
     * ShapeDetailSheet 의 외부 지도 다이얼로그 옵션 분기에 사용 (ON: 4개 / OFF: Google 만).
     */
    val koreaFeaturesEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_KOREA_FEATURES_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val activeShapes: StateFlow<List<ShapeModel>> = shapeRepository.getActiveShapes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 활성 드론 목록 */
    val activeDrones: StateFlow<List<DroneModel>> = droneRepository.getActiveDrones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * droneId → droneName Map. 도형 목록에서 매 셀마다 List<DroneModel> 을 순회하던
     * O(N×M) 비용을 O(N) 으로 감소시키기 위한 캐시. [getDroneName] 도 이 맵을 사용.
     */
    val droneNameById: StateFlow<Map<String, String>> = activeDrones
        .map { drones -> drones.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * droneId → DroneModel Map. ShapeDetailSheet 의 드론 3상태(정상/삭제됨/미할당) 분기에 사용.
     */
    val droneById: StateFlow<Map<String, DroneModel>> = activeDrones
        .map { drones -> drones.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _sortOption = MutableStateFlow(SortOption.FLIGHT_START)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val _selectedShapeId = MutableStateFlow<String?>(null)
    val selectedShapeId: StateFlow<String?> = _selectedShapeId.asStateFlow()

    private val _showShapeDetail = MutableStateFlow(false)
    val showShapeDetail: StateFlow<Boolean> = _showShapeDetail.asStateFlow()

    /**
     * 단일 파이프라인: 정렬을 Dispatchers.Default 로 옮긴 뒤
     * 한 번에 active/notStarted/expired/total 로 분류한다.
     * 검색/드론필터는 iOS SavedTableListView 동등으로 제거됨 (글로벌 드론 매니저 연동은 별도).
     */
    val sections: StateFlow<SavedShapeSections> = combine(
        activeShapes, _sortOption, _sortDirection
    ) { shapes, sort, direction ->
        val sorted = sortShapes(shapes, sort, direction)
        SavedShapeSections(
            activeFiltered = sorted.filter { !it.isNotStarted && !it.isExpired },
            notStarted = sorted.filter { it.isNotStarted },
            expired = sorted.filter { it.isExpired },
            total = sorted.size
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavedShapeSections.EMPTY)

    // 하위 호환을 위한 개별 StateFlow (sections 에서 derive). 추후 호출처 정리 시 제거 가능.
    val activeFilteredShapes: StateFlow<List<ShapeModel>> = sections
        .map { it.activeFiltered }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notStartedShapes: StateFlow<List<ShapeModel>> = sections
        .map { it.notStarted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expiredShapes: StateFlow<List<ShapeModel>> = sections
        .map { it.expired }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totalCount: StateFlow<Int> = sections
        .map { it.total }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val selectedShape: StateFlow<ShapeModel?> = combine(
        _selectedShapeId, activeShapes
    ) { shapeId, shapes ->
        shapeId?.let { id -> shapes.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.ASCENDING)
            SortDirection.DESCENDING else SortDirection.ASCENDING
    }

    fun onShapeSelected(shapeId: String) {
        _selectedShapeId.value = shapeId
        _showShapeDetail.value = true
    }

    /**
     * 도형 상세 시트 닫기. selectedShapeId 와 showShapeDetail 을 동시 리셋한다.
     * 호출처는 deleteShape 내부에서 한 번만 호출하면 됨 (외부 중복 호출 불필요).
     */
    fun dismissShapeDetail() {
        _selectedShapeId.value = null
        _showShapeDetail.value = false
    }

    fun deleteShape(shape: ShapeModel) {
        viewModelScope.launch {
            shapeRepository.softDeleteShape(shape)
            dismissShapeDetail()
        }
    }

    /**
     * 도형 복제 — 신규 UUID + " (복사)" suffix + 신규 createdAt 으로 Repository 에 insert.
     * iOS ShapeEditView(isDuplicateMode=true) 정합 — 저장 목록 시트에선 즉시 복제 (편집 UI 생략).
     * 지도 화면 진입 후 ShapeDetailSheet 의 메뉴에서 복제하면 ShapeEditScreen(isDuplicateMode=true) 진입.
     */
    fun duplicateShape(shape: ShapeModel, copySuffix: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val duplicated = shape.copy(
                id = java.util.UUID.randomUUID().toString(),
                title = "${shape.title} $copySuffix",
                createdAt = now,
                updatedAt = now,
            )
            shapeRepository.insertShape(duplicated)
            dismissShapeDetail()
        }
    }

    /**
     * 드론 ID로 드론 이름 조회. [droneNameById] 캐시를 사용하여 O(1) 조회.
     */
    fun getDroneName(droneId: String?): String? {
        if (droneId == null) return null
        return droneNameById.value[droneId]
    }

    /**
     * 드론 ID 로 DroneModel 조회. ShapeDetailSheet 의 드론 3상태(정상/삭제됨/미할당) 분기에 사용.
     */
    fun getDroneById(droneId: String?): DroneModel? {
        if (droneId == null) return null
        return droneById.value[droneId]
    }

    private fun sortShapes(
        shapes: List<ShapeModel>,
        option: SortOption,
        direction: SortDirection
    ): List<ShapeModel> {
        val comparator: Comparator<ShapeModel> = when (option) {
            SortOption.TITLE -> compareBy { it.title.lowercase() }
            SortOption.DATE_CREATED -> compareBy { it.createdAt }
            SortOption.FLIGHT_START -> compareBy { it.flightStartDate }
            SortOption.FLIGHT_END -> compareBy { it.flightEndDate ?: Long.MAX_VALUE }
        }
        return if (direction == SortDirection.ASCENDING) {
            shapes.sortedWith(comparator)
        } else {
            shapes.sortedWith(comparator.reversed())
        }
    }
}
