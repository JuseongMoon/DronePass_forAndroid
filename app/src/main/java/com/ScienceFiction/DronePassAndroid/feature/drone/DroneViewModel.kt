package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random
import javax.inject.Inject

/**
 * 드론 삭제 시 연결된 도형 처리 방법
 */
sealed class ShapeHandling {
    /** 다른 드론으로 도형 재할당 */
    data class Reassign(val targetDroneId: String) : ShapeHandling()
    /** 연결된 도형 모두 삭제 */
    data object DeleteAll : ShapeHandling()
}

sealed class DroneDeleteError {
    data class Validation(val error: DroneDeleteValidationError) : DroneDeleteError()
    data class Failure(val message: String) : DroneDeleteError()
}

@HiltViewModel
class DroneViewModel @Inject constructor(
    private val droneRepository: DroneRepository,
    private val shapeRepository: ShapeRepository,
    private val droneSelectionState: DroneSelectionState,
    private val analyticsLogger: AnalyticsLogger,
) : ViewModel() {

    /**
     * 활성 드론 목록 (soft delete 되지 않은 드론)
     */
    val activeDrones: StateFlow<List<DroneModel>> = droneRepository.getActiveDrones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 현재 선택된 드론 ID */
    private val _selectedDroneId = MutableStateFlow<String?>(null)
    val selectedDroneId: StateFlow<String?> = _selectedDroneId.asStateFlow()

    /** 드론 상세 시트 표시 여부 */
    private val _showDroneDetail = MutableStateFlow(false)
    val showDroneDetail: StateFlow<Boolean> = _showDroneDetail.asStateFlow()

    /** 드론 편집 시트 표시 여부 */
    private val _showDroneEdit = MutableStateFlow(false)
    val showDroneEdit: StateFlow<Boolean> = _showDroneEdit.asStateFlow()

    /** 현재 선택된 드론 (상세/편집 대상) */
    private val _selectedDrone = MutableStateFlow<DroneModel?>(null)
    val selectedDrone: StateFlow<DroneModel?> = _selectedDrone.asStateFlow()

    /** iOS DroneDetailView 의 showingErrorAlert/errorMessage 대응. */
    private val _deleteError = MutableStateFlow<DroneDeleteError?>(null)
    val deleteError: StateFlow<DroneDeleteError?> = _deleteError.asStateFlow()

    init {
        viewModelScope.launch {
            droneRepository.getAllDrones().collect { drones ->
                if (!_showDroneEdit.value) {
                    _selectedDrone.value = resolveDroneDetailSnapshot(
                        selectedDrone = _selectedDrone.value,
                        allDrones = drones,
                    )
                }
            }
        }
    }

    /**
     * 드론 추가
     */
    fun addDrone(name: String, color: String, serialNumber: String? = null,
                 takeoffWeight: String? = null, size: String? = null, memo: String? = null) {
        viewModelScope.launch {
            val drone = DroneModel(
                name = name,
                color = color,
                serialNumber = serialNumber,
                takeoffWeight = takeoffWeight,
                size = size,
                memo = memo
            )
            droneRepository.insertDrone(drone)
            droneSelectionState.addDroneToSelection(drone.id)
            analyticsLogger.logDroneCreated()
        }
    }

    /**
     * 드론 업데이트
     */
    fun updateDrone(drone: DroneModel) {
        viewModelScope.launch {
            val updatedDrone = drone.copy(updatedAt = System.currentTimeMillis())
            droneRepository.updateDrone(updatedDrone)
        }
    }

    /**
     * 드론 삭제 (연결된 도형 처리 포함)
     *
     * 트랜잭션 일관성 한계: shape 처리 → drone 삭제는 두 Repository 호출이라 한 트랜잭션이
     * 아니다. 사이에 앱이 죽으면 shape 만 reassign/삭제되고 drone 은 살아있는 상태가 가능.
     * 회복 경로: 다음 launch 시 사용자가 동일 작업을 재시도하거나 drone 화면에서 직접 삭제.
     * 진정한 atomic update 가 필요해지면 Room @Transaction 으로 묶는 도메인 서비스가 필요.
     */
    fun deleteDrone(drone: DroneModel, shapeHandling: ShapeHandling) {
        viewModelScope.launch {
            try {
                val currentDrones = activeDrones.value
                val connectedShapeCount = shapeRepository.getActiveShapeCountByDroneId(drone.id)
                val validationError = validateDroneDeleteRequest(
                    activeDrones = currentDrones,
                    connectedShapeCount = connectedShapeCount,
                    shapeHandling = shapeHandling,
                    deletingDroneId = drone.id,
                )
                if (validationError != null) {
                    _deleteError.value = DroneDeleteError.Validation(validationError)
                    return@launch
                }

                // 연결된 도형 처리
                when (shapeHandling) {
                    is ShapeHandling.Reassign -> {
                        val targetDrone = currentDrones.find { it.id == shapeHandling.targetDroneId }
                        if (targetDrone != null && connectedShapeCount > 0) {
                            shapeRepository.reassignShapes(drone.id, targetDrone.id)
                        }
                    }
                    is ShapeHandling.DeleteAll -> {
                        if (connectedShapeCount > 0) {
                            shapeRepository.softDeleteShapesByDroneId(drone.id)
                        }
                    }
                }

                // 드론 소프트 삭제 (Repository 내부에서 softDelete() 로 updatedAt 갱신됨 — stale 가드 불필요)
                droneRepository.softDeleteDrone(drone)
                analyticsLogger.logDroneDeleted()

                // 상세/편집 시트 닫기
                _showDroneDetail.value = false
                _showDroneEdit.value = false
                _selectedDrone.value = null
            } catch (e: Exception) {
                _deleteError.value = DroneDeleteError.Failure(
                    e.localizedMessage ?: e.message ?: e.toString()
                )
            }
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    /**
     * 다음 색상 추천 (사용되지 않은 PaletteColor 반환, GRAY 제외).
     *
     * StateFlow.value 가 stateIn upstream emit 전에는 빈 리스트인 시점 문제를 회피하기
     * 위해 [drones] 를 명시 인자로 받는 형태를 권장한다. 인자 미지정 시 fallback 으로
     * activeDrones.value 를 사용하되 빈 리스트일 가능성을 호출자가 인지해야 한다.
     */
    fun suggestNextColor(drones: List<DroneModel> = activeDrones.value): PaletteColor {
        return suggestNextDroneColor(drones)
    }

    /**
     * 이름 중복 체크. 마찬가지로 호출자가 collectAsStateWithLifecycle 결과를 직접
     * 전달하는 것을 권장 (시점 race 회피).
     */
    fun isDuplicateName(
        name: String,
        excludeId: String? = null,
        drones: List<DroneModel> = activeDrones.value
    ): Boolean {
        return drones.any {
            it.name.equals(name, ignoreCase = true) && it.id != excludeId
        }
    }

    /**
     * 드론 선택 (상세 시트 표시)
     */
    fun selectDrone(droneId: String) {
        val drone = activeDrones.value.find { it.id == droneId }
        _selectedDrone.value = drone
        _selectedDroneId.value = droneId
        _showDroneDetail.value = true
    }

    /**
     * 편집 저장 후 iOS DroneDetailView 와 동일하게 상세 화면으로 돌아간다.
     */
    fun showDetailSheet(drone: DroneModel) {
        _selectedDrone.value = drone
        _selectedDroneId.value = drone.id
        _showDroneEdit.value = false
        _showDroneDetail.value = true
    }

    /**
     * 선택 해제
     */
    fun clearSelection() {
        _selectedDrone.value = null
        _selectedDroneId.value = null
        _showDroneDetail.value = false
        _showDroneEdit.value = false
    }

    /**
     * 편집 시트 표시
     */
    fun showEditSheet(drone: DroneModel? = null) {
        _selectedDrone.value = drone
        _showDroneEdit.value = true
        _showDroneDetail.value = false
    }

    /**
     * 편집 시트 닫기
     */
    fun dismissEditSheet() {
        _showDroneEdit.value = false
        val currentDrone = _selectedDrone.value
        if (!shouldReturnToDroneDetailAfterEditDismiss(currentDrone) || currentDrone == null) {
            return
        }

        _showDroneDetail.value = true
        viewModelScope.launch {
            _selectedDrone.value = droneRepository.getDroneById(currentDrone.id) ?: currentDrone
        }
    }

    /**
     * 상세 시트 닫기
     */
    fun dismissDetailSheet() {
        _showDroneDetail.value = false
        _selectedDrone.value = null
    }

    /**
     * 특정 드론에 연결된 활성 도형 수 조회
     */
    suspend fun getShapeCountForDrone(droneId: String): Int {
        return shapeRepository.getActiveShapeCountByDroneId(droneId)
    }
}

internal fun suggestNextDroneColor(
    drones: List<DroneModel>,
    randomIndex: (Int) -> Int = { bound -> Random.nextInt(bound) },
): PaletteColor {
    val selectableColors = PaletteColor.droneSelectableEntries
    val usedColors = drones.mapNotNull { PaletteColor.fromHex(it.color) }.toSet()
    val availableColors = selectableColors.filter { it !in usedColors }

    return availableColors.firstOrNull()
        ?: selectableColors[randomIndex(selectableColors.size).coerceIn(selectableColors.indices)]
}

internal fun resolveDroneDetailSnapshot(
    selectedDrone: DroneModel?,
    allDrones: List<DroneModel>,
): DroneModel? {
    if (selectedDrone == null) return null
    return allDrones.find { it.id == selectedDrone.id } ?: selectedDrone
}

internal fun shouldReturnToDroneDetailAfterEditDismiss(selectedDrone: DroneModel?): Boolean {
    return selectedDrone != null
}
