package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

@HiltViewModel
class DroneViewModel @Inject constructor(
    private val droneRepository: DroneRepository,
    private val shapeRepository: ShapeRepository
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
     * @return 삭제 성공 여부
     */
    fun deleteDrone(drone: DroneModel, shapeHandling: ShapeHandling) {
        viewModelScope.launch {
            // 마지막 드론 삭제 불가 체크
            val currentDrones = activeDrones.value
            if (currentDrones.size <= 1) return@launch

            // 연결된 도형 처리
            when (shapeHandling) {
                is ShapeHandling.Reassign -> {
                    val targetDrone = currentDrones.find { it.id == shapeHandling.targetDroneId }
                    if (targetDrone != null) {
                        shapeRepository.reassignShapes(drone.id, targetDrone.id, targetDrone.color)
                    }
                }
                is ShapeHandling.DeleteAll -> {
                    shapeRepository.softDeleteShapesByDroneId(drone.id)
                }
            }

            // 드론 소프트 삭제
            droneRepository.softDeleteDrone(drone)

            // 상세/편집 시트 닫기
            _showDroneDetail.value = false
            _showDroneEdit.value = false
            _selectedDrone.value = null
        }
    }

    /**
     * 다음 색상 추천 (사용되지 않은 PaletteColor 반환, GRAY 제외)
     */
    fun suggestNextColor(): PaletteColor {
        val usedColors = activeDrones.value.mapNotNull { PaletteColor.fromHex(it.color) }.toSet()
        val availableColors = PaletteColor.entries.filter { it != PaletteColor.GRAY && it !in usedColors }
        return availableColors.firstOrNull() ?: PaletteColor.BLUE
    }

    /**
     * 이름 중복 체크
     */
    fun isDuplicateName(name: String, excludeId: String? = null): Boolean {
        return activeDrones.value.any {
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
