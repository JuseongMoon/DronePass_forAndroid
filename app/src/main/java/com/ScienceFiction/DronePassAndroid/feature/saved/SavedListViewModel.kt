package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val filteredAndSortedShapes: StateFlow<List<ShapeModel>> = combine(
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
            null -> searchFiltered // 전체
            "" -> searchFiltered.filter { it.droneId == null } // 미연결
            else -> searchFiltered.filter { it.droneId == droneFilter } // 특정 드론
        }
        sortShapes(droneFiltered, sort, direction)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notStartedShapes: StateFlow<List<ShapeModel>> = filteredAndSortedShapes.map { shapes ->
        shapes.filter { it.isNotStarted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeFilteredShapes: StateFlow<List<ShapeModel>> = filteredAndSortedShapes.map { shapes ->
        shapes.filter { !it.isNotStarted && !it.isExpired }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiredShapes: StateFlow<List<ShapeModel>> = filteredAndSortedShapes.map { shapes ->
        shapes.filter { it.isExpired }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedShape: StateFlow<ShapeModel?> = combine(
        _selectedShapeId, activeShapes
    ) { shapeId, shapes ->
        shapeId?.let { id -> shapes.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalCount: StateFlow<Int> = filteredAndSortedShapes.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
     * 드론 ID로 드론 이름 조회
     */
    fun getDroneName(droneId: String?): String? {
        if (droneId == null) return null
        return activeDrones.value.find { it.id == droneId }?.name
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
