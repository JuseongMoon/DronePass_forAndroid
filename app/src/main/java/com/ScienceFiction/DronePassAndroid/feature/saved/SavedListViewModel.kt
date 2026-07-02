package com.ScienceFiction.DronePassAndroid.feature.saved

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneSelectionState
import com.ScienceFiction.DronePassAndroid.feature.drone.filterShapesForSelectedDrones
import com.ScienceFiction.DronePassAndroid.feature.settings.SettingsPreferenceKeys
import com.ScienceFiction.DronePassAndroid.feature.settings.defaultKoreaFeaturesEnabled
import com.ScienceFiction.DronePassAndroid.feature.settings.resolveCurrentAppLanguage
import com.ScienceFiction.DronePassAndroid.feature.settings.resolveKoreaFeaturesEnabled
import com.ScienceFiction.DronePassAndroid.feature.settings.storedHideExpiredShapes
import com.ScienceFiction.DronePassAndroid.feature.settings.storedHideNotStartedShapes
import com.ScienceFiction.DronePassAndroid.feature.settings.storedKoreaFeaturesEnabled
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeEditDefaults
import com.ScienceFiction.DronePassAndroid.feature.shape.resolveSelectedShapeSnapshot
import com.ScienceFiction.DronePassAndroid.feature.shape.resolveShapeEditDefaultColor
import com.ScienceFiction.DronePassAndroid.feature.shape.resolveShapeEditConflict
import com.ScienceFiction.DronePassAndroid.feature.shape.shapeEditSaveFailureMessage
import com.ScienceFiction.DronePassAndroid.feature.shape.storedShapeEditDefaults
import com.ScienceFiction.DronePassAndroid.feature.shape.writeShapeEditDateOnlyMode
import com.ScienceFiction.DronePassAndroid.feature.shape.writeShapeEditDefaults
import com.ScienceFiction.DronePassAndroid.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
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

data class SavedShapeVisibilitySettings(
    val hideExpired: Boolean = false,
    val hideNotStarted: Boolean = false,
)

internal enum class SavedDetailEditPostSaveAction {
    RETURN_TO_DETAIL,
    FOCUS_SAVED_LIST,
}

internal enum class SavedShapeDeleteSource {
    LIST,
    DETAIL,
}

internal data class SavedShapeDeletePresentationUpdate(
    val dismissDetailAndClearSelection: Boolean,
)

internal fun resolveSavedShapeDeletePresentationUpdate(
    source: SavedShapeDeleteSource,
): SavedShapeDeletePresentationUpdate {
    return SavedShapeDeletePresentationUpdate(
        dismissDetailAndClearSelection = source == SavedShapeDeleteSource.DETAIL,
    )
}

internal fun resolveSavedDetailEditPostSaveAction(
    isDuplicate: Boolean,
): SavedDetailEditPostSaveAction {
    return if (isDuplicate) {
        SavedDetailEditPostSaveAction.FOCUS_SAVED_LIST
    } else {
        SavedDetailEditPostSaveAction.RETURN_TO_DETAIL
    }
}

internal fun buildSavedShapeSections(
    shapes: List<ShapeModel>,
    activeDrones: List<DroneModel>,
    selectedDroneIds: Set<String>,
    sortOption: SortOption,
    sortDirection: SortDirection,
    visibilitySettings: SavedShapeVisibilitySettings,
    now: Long = System.currentTimeMillis(),
): SavedShapeSections {
    val droneFilteredShapes = filterShapesForSelectedDrones(shapes, activeDrones, selectedDroneIds)

    val sections = if (sortOption == SortOption.FLIGHT_END) {
        buildSavedShapeSectionsFromGloballySortedShapes(
            shapes = droneFilteredShapes,
            sortOption = sortOption,
            sortDirection = sortDirection,
            visibilitySettings = visibilitySettings,
            now = now,
        )
    } else {
        buildSavedShapeSectionsFromIndividuallySortedSections(
            shapes = droneFilteredShapes,
            sortOption = sortOption,
            sortDirection = sortDirection,
            visibilitySettings = visibilitySettings,
            now = now,
        )
    }

    return SavedShapeSections(
        activeFiltered = sections.activeFiltered,
        notStarted = sections.notStarted,
        expired = sections.expired,
        total = sections.activeFiltered.size + sections.notStarted.size + sections.expired.size,
    )
}

internal fun buildSavedShapeSectionsFromGloballySortedShapes(
    shapes: List<ShapeModel>,
    sortOption: SortOption,
    sortDirection: SortDirection,
    visibilitySettings: SavedShapeVisibilitySettings,
    now: Long = System.currentTimeMillis(),
): SavedShapeSections {
    val sorted = sortSavedShapes(shapes, sortOption, sortDirection)
    return splitSavedShapeSections(
        shapes = sorted,
        visibilitySettings = visibilitySettings,
        now = now,
    )
}

internal fun buildSavedShapeSectionsFromIndividuallySortedSections(
    shapes: List<ShapeModel>,
    sortOption: SortOption,
    sortDirection: SortDirection,
    visibilitySettings: SavedShapeVisibilitySettings,
    now: Long = System.currentTimeMillis(),
): SavedShapeSections {
    val split = splitSavedShapeSections(
        shapes = shapes,
        visibilitySettings = visibilitySettings,
        now = now,
    )

    return SavedShapeSections(
        activeFiltered = sortSavedShapes(split.activeFiltered, sortOption, sortDirection),
        notStarted = sortSavedShapes(split.notStarted, sortOption, sortDirection),
        expired = sortSavedShapes(split.expired, sortOption, sortDirection),
        total = split.total,
    )
}

private fun splitSavedShapeSections(
    shapes: List<ShapeModel>,
    visibilitySettings: SavedShapeVisibilitySettings,
    now: Long,
): SavedShapeSections {
    val active = shapes.filter { !isSavedListNotStarted(it, now) && !isSavedListExpired(it, now) }
    val notStarted = if (visibilitySettings.hideNotStarted) {
        emptyList()
    } else {
        shapes.filter { isSavedListNotStarted(it, now) }
    }
    val expired = if (visibilitySettings.hideExpired) {
        emptyList()
    } else {
        shapes.filter { isSavedListExpired(it, now) }
    }
    return SavedShapeSections(
        activeFiltered = active,
        notStarted = notStarted,
        expired = expired,
        total = active.size + notStarted.size + expired.size,
    )
}

internal fun isSavedListNotStarted(shape: ShapeModel, now: Long = System.currentTimeMillis()): Boolean {
    return shape.flightStartDate > now
}

internal fun isSavedListExpired(shape: ShapeModel, now: Long = System.currentTimeMillis()): Boolean {
    return shape.flightEndDate?.let { it <= now } ?: false
}

internal fun sortSavedShapes(
    shapes: List<ShapeModel>,
    option: SortOption,
    direction: SortDirection,
): List<ShapeModel> {
    val collator = Collator.getInstance(Locale.getDefault())
    val multiplier = if (direction == SortDirection.ASCENDING) 1 else -1

    fun compareStrings(first: String?, second: String?): Int {
        return collator.compare(first.orEmpty(), second.orEmpty())
    }

    fun compareDates(first: Long, second: Long): Int {
        return first.compareTo(second)
    }

    return shapes.sortedWith { first, second ->
        val comparisons = when (option) {
            SortOption.TITLE -> listOf(
                compareStrings(first.title, second.title),
                compareDates(first.flightStartDate, second.flightStartDate),
                compareStrings(first.address, second.address),
            )
            SortOption.DATE_CREATED -> listOf(
                compareDates(first.createdAt, second.createdAt),
                compareStrings(first.title, second.title),
                compareStrings(first.address, second.address),
            )
            SortOption.FLIGHT_START -> listOf(
                compareDates(first.flightStartDate, second.flightStartDate),
                compareStrings(first.title, second.title),
                compareStrings(first.address, second.address),
            )
            SortOption.FLIGHT_END -> listOf(
                compareDates(first.flightEndDate ?: Long.MAX_VALUE, second.flightEndDate ?: Long.MAX_VALUE),
                compareStrings(first.title, second.title),
                compareStrings(first.address, second.address),
            )
        }
        comparisons.firstOrNull { it != 0 }?.let { it * multiplier } ?: 0
    }
}

@HiltViewModel
class SavedListViewModel @Inject constructor(
    private val shapeRepository: ShapeRepository,
    private val droneRepository: DroneRepository,
    private val droneSelectionState: DroneSelectionState,
    private val dataStore: DataStore<Preferences>,
    val naverGeocodingApi: NaverGeocodingApi,
    private val analyticsLogger: AnalyticsLogger,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    /**
     * 한국 특화 기능 토글 — Settings 정합화 commit 에서 추가된 DataStore key 공유.
     * ShapeDetailSheet 의 외부 지도 다이얼로그 옵션 분기에 사용 (ON: 4개 / OFF: Google 만).
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

    private val activeShapes: StateFlow<List<ShapeModel>> = shapeRepository.getActiveShapes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeShapeIds: StateFlow<Set<String>> = activeShapes
        .map { shapes -> shapes.mapTo(mutableSetOf()) { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val defaultShapeColor: StateFlow<String> = activeShapes
        .map(::resolveShapeEditDefaultColor)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), resolveShapeEditDefaultColor(emptyList()))

    /** 활성 드론 목록 */
    val activeDrones: StateFlow<List<DroneModel>> = droneRepository.getActiveDrones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDroneIds: StateFlow<Set<String>> = droneSelectionState.selectedDroneIds

    companion object {
        private val KEY_SORT_OPTION = stringPreferencesKey("ShapeSortingManager.selectedSortOption")
        private val KEY_SORT_DIRECTION = stringPreferencesKey("ShapeSortingManager.sortDirection")
    }

    init {
        viewModelScope.launch {
            activeDrones.collect { drones ->
                droneSelectionState.syncActiveDrones(drones)
            }
        }
    }

    /**
     * droneId → DroneModel Map. ShapeDetailSheet 의 드론 3상태(정상/삭제됨/미할당) 분기에 사용.
     */
    val droneById: StateFlow<Map<String, DroneModel>> = activeDrones
        .map { drones -> drones.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val sortOption: StateFlow<SortOption> = dataStore.data
        .map { preferences -> SortOption.fromRawValue(preferences[KEY_SORT_OPTION]) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortOption.TITLE)

    val sortDirection: StateFlow<SortDirection> = dataStore.data
        .map { preferences -> SortDirection.fromRawValue(preferences[KEY_SORT_DIRECTION]) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortDirection.ASCENDING)

    private val sortSettings: StateFlow<Pair<SortOption, SortDirection>> = combine(
        sortOption,
        sortDirection,
    ) { option, direction -> option to direction }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortOption.TITLE to SortDirection.ASCENDING)

    private val hideExpiredShapes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> storedHideExpiredShapes(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val hideNotStartedShapes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> storedHideNotStartedShapes(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val visibilitySettings: StateFlow<SavedShapeVisibilitySettings> = combine(
        hideExpiredShapes,
        hideNotStartedShapes,
    ) { hideExpired, hideNotStarted ->
        SavedShapeVisibilitySettings(
            hideExpired = hideExpired,
            hideNotStarted = hideNotStarted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavedShapeVisibilitySettings())

    private val _selectedShapeId = MutableStateFlow<String?>(null)
    val selectedShapeId: StateFlow<String?> = _selectedShapeId.asStateFlow()

    private val _showShapeDetail = MutableStateFlow(false)
    val showShapeDetail: StateFlow<Boolean> = _showShapeDetail.asStateFlow()

    private val _showShapeEdit = MutableStateFlow(false)
    val showShapeEdit: StateFlow<Boolean> = _showShapeEdit.asStateFlow()

    private val _isDuplicateMode = MutableStateFlow(false)
    val isDuplicateMode: StateFlow<Boolean> = _isDuplicateMode.asStateFlow()

    private val _savedShapeFocusEvent = MutableSharedFlow<String>()
    val savedShapeFocusEvent: SharedFlow<String> = _savedShapeFocusEvent.asSharedFlow()

    /**
     * 단일 파이프라인: 정렬을 Dispatchers.Default 로 옮긴 뒤
     * 한 번에 active/notStarted/expired/total 로 분류한다.
     * iOS SavedTableListView 와 동일하게 글로벌 드론 선택 필터를 먼저 적용한 뒤 정렬/분류한다.
     */
    val sections: StateFlow<SavedShapeSections> = combine(
        activeShapes,
        activeDrones,
        selectedDroneIds,
        sortSettings,
        visibilitySettings,
    ) { shapes, drones, selectedIds, sort, visibility ->
        buildSavedShapeSections(
            shapes = shapes,
            activeDrones = drones,
            selectedDroneIds = selectedIds,
            sortOption = sort.first,
            sortDirection = sort.second,
            visibilitySettings = visibility,
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
    val allShapeCount: StateFlow<Int> = activeShapes
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val selectedDroneCount: StateFlow<Int> = selectedDroneIds
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val selectedShape: StateFlow<ShapeModel?> = combine(
        _selectedShapeId, activeShapes
    ) { shapeId, shapes ->
        resolveSelectedShapeSnapshot(shapeId, shapes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val primarySelectedDroneId: StateFlow<String?> = combine(
        activeDrones,
        droneSelectionState.selectedDroneId,
    ) { drones, selectedDroneId ->
        selectedDroneId?.takeIf { id -> drones.any { it.id == id } }
            ?: drones.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shapeEditDefaults: StateFlow<ShapeEditDefaults> = dataStore.data
        .map(::storedShapeEditDefaults)
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShapeEditDefaults())

    fun updateSortOption(option: SortOption) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_SORT_OPTION] = option.rawValue
            }
        }
    }

    fun toggleSortDirection() {
        val next = if (sortDirection.value == SortDirection.ASCENDING) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_SORT_DIRECTION] = next.rawValue
            }
        }
    }

    fun onShapeSelected(shapeId: String) {
        _selectedShapeId.value = shapeId
        _showShapeDetail.value = true
    }

    /**
     * iOS SavedTableListView.handleShapeTap 정합.
     * 리스트 탭은 상세 시트를 열지 않고 선택 표시 + 지도 포커스만 수행한다.
     */
    fun selectShapeForMapFocus(shapeId: String) {
        _selectedShapeId.value = shapeId
        _showShapeDetail.value = false
    }

    /**
     * 도형 상세 시트 닫기. selectedShapeId 와 showShapeDetail 을 동시 리셋한다.
     * 호출처는 deleteShape 내부에서 한 번만 호출하면 됨 (외부 중복 호출 불필요).
     */
    fun dismissShapeDetail() {
        _selectedShapeId.value = null
        _showShapeDetail.value = false
    }

    fun onEditShapeRequested(shape: ShapeModel) {
        _selectedShapeId.value = shape.id
        _isDuplicateMode.value = false
        _showShapeDetail.value = false
        _showShapeEdit.value = true
    }

    fun onDuplicateRequested(shape: ShapeModel) {
        _selectedShapeId.value = shape.id
        _isDuplicateMode.value = true
        _showShapeDetail.value = false
        _showShapeEdit.value = true
    }

    fun dismissShapeEdit() {
        _showShapeEdit.value = false
        _isDuplicateMode.value = false
        if (_selectedShapeId.value != null) {
            _showShapeDetail.value = true
        }
    }

    fun saveShape(
        shape: ShapeModel,
        isDuplicate: Boolean,
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
                if (isDuplicate) {
                    analyticsLogger.logShapeDuplicated()
                }

                _showShapeEdit.value = false
                _isDuplicateMode.value = false
                _selectedShapeId.value = updatedShape.id

                when (resolveSavedDetailEditPostSaveAction(isDuplicate)) {
                    SavedDetailEditPostSaveAction.RETURN_TO_DETAIL -> {
                        _showShapeDetail.value = true
                    }
                    SavedDetailEditPostSaveAction.FOCUS_SAVED_LIST -> {
                        _showShapeDetail.value = false
                        _savedShapeFocusEvent.emit(updatedShape.id)
                    }
                }
            } catch (error: Exception) {
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

    fun deleteShapeFromList(shape: ShapeModel) {
        deleteShape(shape, source = SavedShapeDeleteSource.LIST)
    }

    fun deleteShapeFromDetail(shape: ShapeModel) {
        deleteShape(shape, source = SavedShapeDeleteSource.DETAIL)
    }

    private fun deleteShape(shape: ShapeModel, source: SavedShapeDeleteSource) {
        viewModelScope.launch {
            shapeRepository.softDeleteShape(shape)
            analyticsLogger.logShapeDeleted()
            val presentationUpdate = resolveSavedShapeDeletePresentationUpdate(source)
            if (presentationUpdate.dismissDetailAndClearSelection) {
                _showShapeEdit.value = false
                _isDuplicateMode.value = false
                dismissShapeDetail()
            }
        }
    }

    fun logExternalMapOpened(appName: String) {
        analyticsLogger.logExternalMapOpened(appName)
    }

    /**
     * 드론 ID 로 DroneModel 조회. ShapeDetailSheet 의 드론 3상태(정상/삭제됨/미할당) 분기에 사용.
     */
    fun getDroneById(droneId: String?): DroneModel? {
        if (droneId == null) return null
        return droneById.value[droneId]
    }

}
