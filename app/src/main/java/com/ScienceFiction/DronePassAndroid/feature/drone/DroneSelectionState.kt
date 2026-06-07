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
    private fun onPersistedSelectionLoaded(selection: StoredDroneSelection) {
        selection.selectedDroneId?.let { savedDroneId ->
            _selectedDroneId.value = savedDroneId
        }
        if (selection.selectedDroneIds.isNotEmpty() && !hasExplicitRuntimeSelection) {
            _selectedDroneIds.value = selection.selectedDroneIds
        }
        hasLoadedPersistedSelection = true

        val pending = pendingActiveDrones
        pendingActiveDrones = null
        if (pending != null) {
            syncActiveDrones(pending)
        }
    }

    /**
     * iOS setupInitialDroneIfNeeded 정합.
     * 첫 활성 드론 목록 로드 시 전체 선택하고, 전체가 선택된 상태에서 새 드론이 추가되면
     * 새 드론도 선택 목록에 포함한다. 사용자가 직접 모두 해제한 런타임 상태는 유지한다.
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
            if (!hasCompletedInitialLoad) {
                _selectedDroneId.value = null
                _selectedDroneIds.value = emptySet()
                _highlightedDroneIds.value = emptySet()
                lastActiveDroneIds = emptySet()
            }
            return
        }

        val current = _selectedDroneIds.value
        val selectedExisting = current.intersect(activeIds)
        val wasAllSelected = hasCompletedInitialLoad &&
            lastActiveDroneIds.isNotEmpty() &&
            current.containsAll(lastActiveDroneIds)

        val nextSelectedIds = when {
            !hasCompletedInitialLoad -> activeIds
            wasAllSelected -> activeIds
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
        val next = _selectedDroneIds.value.toMutableSet()
        if (next.contains(droneId)) {
            next.remove(droneId)
            _highlightedDroneIds.value = _highlightedDroneIds.value - droneId
        } else {
            next.add(droneId)
        }
        _selectedDroneIds.value = next
        saveSelectedDroneIds(next)
    }

    @Synchronized
    fun addDroneToSelection(droneId: String) {
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

    @Synchronized
    fun resetForAccountSwitch() {
        _selectedDroneId.value = null
        _selectedDroneIds.value = emptySet()
        _highlightedDroneIds.value = emptySet()
        hasCompletedInitialLoad = false
        lastActiveDroneIds = emptySet()
        hasExplicitRuntimeSelection = false
        pendingActiveDrones = null
        saveSelectedDroneId(null)
        saveSelectedDroneIds(emptySet())
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
    val selectedDroneIds: Set<String>,
)

internal fun storedDroneSelection(preferences: Preferences): StoredDroneSelection {
    return StoredDroneSelection(
        selectedDroneId = preferences[DroneSelectionPreferenceKeys.SELECTED_DRONE_ID],
        selectedDroneIds = storedDroneSelectedIds(preferences),
    )
}

internal fun storedDroneSelectedIds(preferences: Preferences): Set<String> {
    return preferences[DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS]
        ?: preferences[DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS]
        ?: emptySet()
}

internal fun MutablePreferences.writeDroneSelectedDroneId(id: String?) {
    if (id == null) {
        remove(DroneSelectionPreferenceKeys.SELECTED_DRONE_ID)
    } else {
        this[DroneSelectionPreferenceKeys.SELECTED_DRONE_ID] = id
    }
}

internal fun MutablePreferences.writeDroneSelectedIds(ids: Set<String>) {
    if (ids.isEmpty()) {
        remove(DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS)
    } else {
        this[DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS] = ids
    }
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
