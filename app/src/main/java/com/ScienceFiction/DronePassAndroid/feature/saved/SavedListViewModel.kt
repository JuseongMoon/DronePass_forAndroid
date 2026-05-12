package com.ScienceFiction.DronePassAndroid.feature.saved

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
 * 4개의 StateFlow 를 단일 객체로 묶어 검색어 1글자 변경 시에도 1번만 정렬/분류된다.
 * (이전: filteredAndSortedShapes + notStarted/active/expired/total 의 4번 stateIn 파이프라인.)
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
    private val droneRepository: DroneRepository
) : ViewModel() {

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.FLIGHT_START)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val _selectedShapeId = MutableStateFlow<String?>(null)
    val selectedShapeId: StateFlow<String?> = _selectedShapeId.asStateFlow()

    private val _showShapeDetail = MutableStateFlow(false)
    val showShapeDetail: StateFlow<Boolean> = _showShapeDetail.asStateFlow()

    /** 드론 필터: null = 전체, 빈 문자열 = 미연결, 그 외 = 드론 ID */
    private val _selectedDroneFilter = MutableStateFlow<String?>(null)
    val selectedDroneFilter: StateFlow<String?> = _selectedDroneFilter.asStateFlow()

    /**
     * 단일 파이프라인: 검색/드론필터/정렬을 Dispatchers.Default 로 옮긴 뒤
     * 한 번에 active/notStarted/expired/total 로 분류한다.
     *
     * 이전: filteredAndSortedShapes(stateIn) + notStarted/active/expired/total 4개
     *   stateIn 으로 검색 1글자에 500개 도형 정렬이 메인 스레드에서 5번 수행됨.
     * 수정: flowOn(Default) 로 백그라운드 처리 + 단일 [SavedShapeSections] 객체 emit.
     */
    val sections: StateFlow<SavedShapeSections> = combine(
        activeShapes, _searchQuery, _sortOption, _sortDirection, _selectedDroneFilter
    ) { shapes, query, sort, direction, droneFilter ->
        // 검색 필터
        val searchFiltered = if (query.isBlank()) shapes else shapes.filter { shape ->
            shape.title.contains(query, ignoreCase = true) ||
                shape.address?.contains(query, ignoreCase = true) == true ||
                shape.memo?.contains(query, ignoreCase = true) == true
        }
        // 드론 필터
        val droneFiltered = when (droneFilter) {
            null -> searchFiltered
            "" -> searchFiltered.filter { it.droneId == null }
            else -> searchFiltered.filter { it.droneId == droneFilter }
        }
        val sorted = sortShapes(droneFiltered, sort, direction)
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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.ASCENDING)
            SortDirection.DESCENDING else SortDirection.ASCENDING
    }

    /**
     * 드론 필터 변경
     * @param droneId null = 전체, "" = 미연결, 그 외 = 특정 드론 ID
     */
    fun updateDroneFilter(droneId: String?) {
        _selectedDroneFilter.value = droneId
    }

    fun onShapeSelected(shapeId: String) {
        _selectedShapeId.value = shapeId
        _showShapeDetail.value = true
    }

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
     * 드론 ID로 드론 이름 조회. [droneNameById] 캐시를 사용하여 O(1) 조회.
     */
    fun getDroneName(droneId: String?): String? {
        if (droneId == null) return null
        return droneNameById.value[droneId]
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
