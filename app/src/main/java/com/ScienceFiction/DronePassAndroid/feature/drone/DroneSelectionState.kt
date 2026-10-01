package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ScienceFiction.DronePassAndroid.core.util.compareIosLocalizedStandardStrings
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * iOS DroneManager 의 지도 드론 선택 상태와 같은 앱 전역 상태.
 * Map/Saved 목록이 같은 selectedDroneIds 를 공유해야 두 화면의 도형 표시가 일치한다.
 */
@Singleton
class DroneSelectionState private constructor(
    private val dataStore: DataStore<Preferences>? = null,
    loadPersistedSelection: Boolean,
) {
    @Inject
    constructor(dataStore: DataStore<Preferences>) : this(
        dataStore = dataStore,
        loadPersistedSelection = true,
    )

    internal constructor() : this(
        dataStore = null,
        loadPersistedSelection = false,
    )

    private val _selectedDroneIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedDroneIds: StateFlow<Set<String>> = _selectedDroneIds.asStateFlow()

    private val _selectedDroneId = MutableStateFlow<String?>(null)
    val selectedDroneId: StateFlow<String?> = _selectedDroneId.asStateFlow()

    private val _highlightedDroneIds = MutableStateFlow<Set<String>>(emptySet())
    val highlightedDroneIds: StateFlow<Set<String>> = _highlightedDroneIds.asStateFlow()

    private var hasCompletedInitialLoad = false
    private var lastActiveDroneIds: Set<String> = emptySet()
    private var hasLoadedPersistedSelection = !loadPersistedSelection
    private var hasExplicitRuntimeSelection = false
    /** 저장된 다중 선택값이 있었는지(비어 있는 선택 포함). 없으면 첫 실행으로 보고 전체 선택한다. */
    private var hasSavedSelection = false
    private var pendingActiveDrones: List<DroneModel>? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        if (loadPersistedSelection) dataStore?.let { store ->
            scope.launch {
                val savedSelection = store.data
                    .map(::storedDroneSelection)
                    .first()
                onPersistedSelectionLoaded(savedSelection)
            }
        }
    }

    @Synchronized
    internal fun onPersistedSelectionLoaded(selection: StoredDroneSelection) {
        selection.selectedDroneId?.let { savedDroneId ->
            _selectedDroneId.value = savedDroneId
        }
        selection.selectedDroneIds?.let { savedIds ->
            hasSavedSelection = true
            if (!hasExplicitRuntimeSelection) {
                _selectedDroneIds.value = savedIds
            }
        }
        hasLoadedPersistedSelection = true

        val pending = pendingActiveDrones
        pendingActiveDrones = null
        if (pending != null) {
            syncActiveDrones(pending)
        }
    }

    /**
     * iOS DroneManager.restoreSelection / dronesDidChange 정합.
     * - 첫 로드: 저장값이 없으면(첫 실행) 전체 선택, 있으면 활성 드론에 해당하는 ID 만 복원한다.
     *   사용자가 모두 해제해 둔 상태(빈 선택)도 그대로 복원한다.
     *   저장된 ID 가 활성 드론과 하나도 겹치지 않으면(손상된 값) 전체 선택으로 되돌린다.
     * - 이후 목록 변경(실시간 동기화 등): 선택은 그대로 두고 사라진 드론만 뺀다.
     *   다른 기기에서 들어온 드론은 자동 선택하지 않는다(iOS 와 같이 칩에 나타나지 않음).
     * 사용자가 추가한 새 드론은 [addDroneToSelection] 에서 iOS addDrone 처럼 명시 선택한다.
     */
    @Synchronized
    fun syncActiveDrones(activeDrones: List<DroneModel>) {
        if (!hasLoadedPersistedSelection) {
            pendingActiveDrones = activeDrones
            return
        }

        val activeIds = activeDrones.map { it.id }.toSet()
        if (activeIds.isEmpty()) {
            // 로컬 드론 로딩 전의 빈 목록. 복원한 선택을 지우면 곧 들어올 실제 목록 기준으로 빈 선택이 저장된다.
            return
        }

        val current = _selectedDroneIds.value
        val selectedExisting = current.intersect(activeIds)

        val nextSelectedIds = when {
            !hasCompletedInitialLoad && !hasSavedSelection && current.isEmpty() -> activeIds
            // iOS restoreSelection 의 손상 복구: 저장값에서 복원한 선택이 활성 드론과 전혀 겹치지 않을 때만.
            !hasCompletedInitialLoad && hasSavedSelection && !hasExplicitRuntimeSelection &&
                current.isNotEmpty() && selectedExisting.isEmpty() -> activeIds
            else -> selectedExisting
        }
        val nextSelectedDroneId = _selectedDroneId.value
            ?.takeIf { it in activeIds }
            ?: activeDrones.firstOrNull()?.id

        _selectedDroneId.value = nextSelectedDroneId
        _selectedDroneIds.value = nextSelectedIds
        saveSelectedDroneId(nextSelectedDroneId)
        saveSelectedDroneIds(nextSelectedIds)

        _highlightedDroneIds.value = _highlightedDroneIds.value.intersect(activeIds)

        hasCompletedInitialLoad = true
        lastActiveDroneIds = activeIds
    }

    @Synchronized
    fun toggleDroneSelection(droneId: String) {
        hasExplicitRuntimeSelection = true
        val next = _selectedDroneIds.value.toMutableSet()
        if (next.contains(droneId)) {
            next.remove(droneId)
        } else {
            next.add(droneId)
        }
        _selectedDroneIds.value = next
        saveSelectedDroneIds(next)
    }

    /**
     * iOS `DroneSelectionDropdown` button action 정합.
     *
     * `DroneManager.toggleDroneSelection()` 자체는 강조 상태를 직접 건드리지 않지만,
     * 메인 드롭다운 UI는 체크 해제 직후 해당 드론이 강조 중이면 강조도 함께 해제한다.
     */
    @Synchronized
    fun toggleDroneSelectionFromMainDropdown(droneId: String) {
        toggleDroneSelection(droneId)
        if (droneId !in _selectedDroneIds.value && droneId in _highlightedDroneIds.value) {
            _highlightedDroneIds.value = _highlightedDroneIds.value - droneId
        }
    }

    @Synchronized
    fun addDroneToSelection(droneId: String) {
        hasExplicitRuntimeSelection = true
        val next = _selectedDroneIds.value + droneId
        _selectedDroneIds.value = next
        saveSelectedDroneIds(next)
    }

    /**
     * iOS `DroneManager.selectAllDrones()` 정합.
     * 명시적인 로그인 성공 흐름에서만 호출하여 현재 활성 드론을 모두 선택한다.
     */
    @Synchronized
    fun selectAllDrones(activeDrones: List<DroneModel>) {
        val activeIds = activeDrones.map { it.id }.toSet()
        _selectedDroneIds.value = activeIds
        _highlightedDroneIds.value = _highlightedDroneIds.value.intersect(activeIds)
        hasCompletedInitialLoad = activeIds.isNotEmpty()
        lastActiveDroneIds = activeIds
        hasExplicitRuntimeSelection = true
        saveSelectedDroneIds(activeIds)
    }

    /**
     * 로그인 병합에서 지운 기기 기본 드론을 대신할 드론으로 선택·대표 선택·강조를 옮긴다.
     * 선택돼 있지 않던 드론은 새로 선택하지 않는다.
     */
    @Synchronized
    fun replaceDrones(replacements: Map<String, String>) {
        if (replacements.isEmpty()) return
        fun Set<String>.replaced() = map { replacements[it] ?: it }.toSet()
        val nextSelected = _selectedDroneIds.value.replaced()
        if (nextSelected != _selectedDroneIds.value) {
            _selectedDroneIds.value = nextSelected
            saveSelectedDroneIds(nextSelected)
        }
        _selectedDroneId.value?.let { current ->
            replacements[current]?.let { replacement ->
                _selectedDroneId.value = replacement
                saveSelectedDroneId(replacement)
            }
        }
        _highlightedDroneIds.value = _highlightedDroneIds.value.replaced()
    }

    @Synchronized
    fun resetForAccountSwitch() {
        _selectedDroneId.value = null
        _selectedDroneIds.value = emptySet()
        _highlightedDroneIds.value = emptySet()
        hasCompletedInitialLoad = false
        lastActiveDroneIds = emptySet()
        hasExplicitRuntimeSelection = false
        hasSavedSelection = false
        pendingActiveDrones = null
        saveSelectedDroneId(null)
        clearSavedSelectedDroneIds()
    }

    fun toggleDroneHighlight(droneId: String) {
        val next = _highlightedDroneIds.value.toMutableSet()
        if (next.contains(droneId)) {
            next.remove(droneId)
        } else {
            next.add(droneId)
        }
        _highlightedDroneIds.value = next
    }

    private fun saveSelectedDroneIds(ids: Set<String>) {
        val store = dataStore ?: return
        scope.launch {
            store.edit { preferences ->
                preferences.writeDroneSelectedIds(ids)
            }
        }
    }

    /** 계정 전환: 저장값을 지워 새 계정의 첫 로드가 전체 선택으로 시작하게 한다. */
    private fun clearSavedSelectedDroneIds() {
        val store = dataStore ?: return
        scope.launch {
            store.edit { preferences ->
                preferences.remove(DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS)
                preferences.remove(DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS)
            }
        }
    }

    private fun saveSelectedDroneId(id: String?) {
        val store = dataStore ?: return
        scope.launch {
            store.edit { preferences ->
                preferences.writeDroneSelectedDroneId(id)
            }
        }
    }
}

internal object DroneSelectionPreferenceKeys {
    val SELECTED_DRONE_ID = stringPreferencesKey("selectedDroneId")
    val SELECTED_DRONE_IDS = stringSetPreferencesKey("selectedDroneIds")
    val LEGACY_SELECTED_DRONE_IDS = stringSetPreferencesKey("selected_drone_ids")
}

internal data class StoredDroneSelection(
    val selectedDroneId: String?,
    /** null 이면 저장값 없음(첫 실행). 빈 집합은 사용자가 모두 해제해 둔 상태. */
    val selectedDroneIds: Set<String>?,
)

internal fun storedDroneSelection(preferences: Preferences): StoredDroneSelection {
    return StoredDroneSelection(
        selectedDroneId = preferences[DroneSelectionPreferenceKeys.SELECTED_DRONE_ID],
        selectedDroneIds = storedDroneSelectedIds(preferences),
    )
}

internal fun storedDroneSelectedIds(preferences: Preferences): Set<String>? {
    return preferences[DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS]
        ?: preferences[DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS]
}

internal fun MutablePreferences.writeDroneSelectedDroneId(id: String?) {
    if (id == null) {
        remove(DroneSelectionPreferenceKeys.SELECTED_DRONE_ID)
    } else {
        this[DroneSelectionPreferenceKeys.SELECTED_DRONE_ID] = id
    }
}

/** 빈 선택도 저장한다(iOS UserDefaults 에 빈 배열 저장과 같음). 다음 실행에서 해제 상태를 복원한다. */
internal fun MutablePreferences.writeDroneSelectedIds(ids: Set<String>) {
    this[DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS] = ids
    remove(DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS)
}

internal fun selectedDronesForIosDropdown(
    activeDrones: List<DroneModel>,
    selectedDroneIds: Set<String>,
): List<DroneModel> {
    return sortDronesByIosLocalizedStandardName(
        activeDrones.filter { it.id in selectedDroneIds },
    )
}

internal fun sortDronesByIosLocalizedStandardName(
    drones: List<DroneModel>,
    locale: Locale = Locale.getDefault(),
): List<DroneModel> {
    return drones.sortedWith { first, second ->
        compareIosLocalizedStandardNames(first.name, second.name, locale)
    }
}

internal fun compareIosLocalizedStandardNames(
    first: String,
    second: String,
    locale: Locale = Locale.getDefault(),
): Int {
    return compareIosLocalizedStandardStrings(first, second, locale)
}

/**
 * iOS MapViewModel.reloadOverlays / SavedTableListView.updateSortedShapes 의 드론 필터 규칙.
 * droneId 가 없는 레거시 도형은 첫 번째 활성 드론에 연결된 것으로 처리한다.
 */
fun filterShapesForSelectedDrones(
    shapes: List<ShapeModel>,
    activeDrones: List<DroneModel>,
    selectedDroneIds: Set<String>,
): List<ShapeModel> {
    if (selectedDroneIds.isEmpty()) return emptyList()

    val firstDroneId = activeDrones.firstOrNull()?.id
    return shapes.filter { shape ->
        val droneId = shape.droneId ?: firstDroneId
        droneId != null && droneId in selectedDroneIds
    }
}
