package com.ScienceFiction.DronePassAndroid.feature.sketch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.util.DistanceCalculator
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 스케치 기능의 ViewModel.
 *
 * 자유 그리기, 지우개, Undo/Redo 기능을 제공한다.
 * SketchRepository를 통해 스케치 데이터를 영속화한다.
 */
@HiltViewModel
class SketchViewModel @Inject constructor(
    private val sketchRepository: SketchRepository,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    // ──────────────────────────────────────────────
    // UI 상태
    // ──────────────────────────────────────────────

    /** 스케치 모드 활성화 여부 */
    private val _isSketchMode = MutableStateFlow(false)
    val isSketchMode: StateFlow<Boolean> = _isSketchMode.asStateFlow()

    /** 지우개 모드 활성화 여부 */
    private val _isEraserMode = MutableStateFlow(false)
    val isEraserMode: StateFlow<Boolean> = _isEraserMode.asStateFlow()

    /** 현재 선택된 펜 색상 (HEX) */
    private val _currentColor = MutableStateFlow(PaletteColor.RED.hex)
    val currentColor: StateFlow<String> = _currentColor.asStateFlow()

    /** 현재 펜 두께 */
    private val _currentStrokeWidth = MutableStateFlow(4.0)
    val currentStrokeWidth: StateFlow<Double> = _currentStrokeWidth.asStateFlow()

    /** 현재 펜 투명도 (0.0 ~ 1.0) */
    private val _currentOpacity = MutableStateFlow(1.0)
    val currentOpacity: StateFlow<Double> = _currentOpacity.asStateFlow()

    /** 현재 그리는 중인 포인트 리스트 (실시간 프리뷰용) */
    private val _currentDrawingPoints = MutableStateFlow<List<Coordinate>>(emptyList())
    val currentDrawingPoints: StateFlow<List<Coordinate>> = _currentDrawingPoints.asStateFlow()

    /** Undo 가능 여부 */
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    /** Redo 가능 여부 */
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    /** 활성 스케치 목록 (Room DB Flow) */
    val activeSketches: StateFlow<List<SketchModel>> = sketchRepository.getActiveSketches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ──────────────────────────────────────────────
    // Undo/Redo 스택
    // ──────────────────────────────────────────────

    /** Undo/Redo 액션 타입 */
    private sealed class SketchAction {
        data class Create(val sketch: SketchModel) : SketchAction()
        data class Delete(val sketch: SketchModel) : SketchAction()
    }

    private val undoStack = ArrayDeque<SketchAction>()
    private val redoStack = ArrayDeque<SketchAction>()

    /** 마지막으로 샘플링된 포인트 (최소 거리 필터링용) */
    private var lastSampledPoint: Coordinate? = null

    /** 최소 샘플링 거리 (미터) */
    private companion object {
        const val MIN_SAMPLING_DISTANCE_METERS = 5.0
        const val ERASER_THRESHOLD_METERS = 30.0
    }

    // ──────────────────────────────────────────────
    // 스케치 모드 진입/종료
    // ──────────────────────────────────────────────

    /**
     * 스케치 모드로 진입한다.
     */
    fun enterSketchMode() {
        _isSketchMode.value = true
        _isEraserMode.value = false
        analyticsLogger.logSketchModeEntered()
    }

    /**
     * 스케치 모드를 종료한다.
     * 진행 중인 그리기가 있으면 취소한다.
     */
    fun exitSketchMode() {
        _currentDrawingPoints.value = emptyList()
        lastSampledPoint = null
        _isSketchMode.value = false
        _isEraserMode.value = false
    }

    // ──────────────────────────────────────────────
    // 그리기
    // ──────────────────────────────────────────────

    /**
     * 그리기를 시작한다.
     *
     * @param coordinate 시작 좌표
     */
    fun startDrawing(coordinate: Coordinate) {
        if (_isEraserMode.value) return
        _currentDrawingPoints.value = listOf(coordinate)
        lastSampledPoint = coordinate
    }

    /**
     * 그리기를 계속한다. 최소 5m 샘플링 적용.
     *
     * @param coordinate 현재 좌표
     */
    fun continueDrawing(coordinate: Coordinate) {
        if (_isEraserMode.value) return
        val lastPoint = lastSampledPoint ?: return

        val distance = DistanceCalculator.fastApprox(lastPoint, coordinate)
        if (distance >= MIN_SAMPLING_DISTANCE_METERS) {
            _currentDrawingPoints.value = _currentDrawingPoints.value + coordinate
            lastSampledPoint = coordinate
        }
    }

    /**
     * 그리기를 완료한다.
     * 포인트가 2개 이상이면 스케치를 저장한다.
     */
    fun finishDrawing() {
        val points = _currentDrawingPoints.value
        if (points.size < 2) {
            _currentDrawingPoints.value = emptyList()
            lastSampledPoint = null
            return
        }

        val sketch = SketchModel(
            points = points,
            color = _currentColor.value,
            strokeWidth = _currentStrokeWidth.value,
            opacity = _currentOpacity.value
        )

        viewModelScope.launch {
            sketchRepository.insertSketch(sketch)
            pushUndoAction(SketchAction.Create(sketch))
            analyticsLogger.logSketchSaved()
        }

        _currentDrawingPoints.value = emptyList()
        lastSampledPoint = null
    }

    // ──────────────────────────────────────────────
    // 지우개
    // ──────────────────────────────────────────────

    /**
     * 지우개 모드를 토글한다.
     */
    fun toggleEraserMode() {
        _isEraserMode.value = !_isEraserMode.value
        // 지우개 모드 진입 시 진행 중인 그리기 취소
        if (_isEraserMode.value) {
            _currentDrawingPoints.value = emptyList()
            lastSampledPoint = null
        }
    }

    /**
     * 지우개: 터치 지점과 가까운 스케치를 삭제한다.
     *
     * BoundingBox 2단계 필터링:
     * 1. BoundingBox로 후보 스케치 빠르게 필터링
     * 2. 정밀 거리 계산으로 최종 판정
     *
     * @param point 터치 좌표
     */
    fun deleteSketchAtPoint(point: Coordinate) {
        val sketches = activeSketches.value
        if (sketches.isEmpty()) return

        viewModelScope.launch {
            for (sketch in sketches) {
                if (sketch.points.size < 2) continue

                // 1단계: BoundingBox 필터링
                if (!isPointNearBoundingBox(point, sketch.points)) continue

                // 2단계: 정밀 거리 계산 (점-선분 거리)
                val isNear = sketch.points.zipWithNext().any { (start, end) ->
                    DistanceCalculator.distanceToSegment(point, start, end) < ERASER_THRESHOLD_METERS
                }

                if (isNear) {
                    sketchRepository.softDeleteSketch(sketch)
                    pushUndoAction(SketchAction.Delete(sketch))
                    break // 한 번에 하나의 스케치만 삭제
                }
            }
        }
    }

    /**
     * BoundingBox 내에 포인트가 있는지 빠르게 확인한다.
     * 여유 영역(threshold)을 포함한다.
     */
    private fun isPointNearBoundingBox(
        point: Coordinate,
        sketchPoints: List<Coordinate>
    ): Boolean {
        // 약 30m에 해당하는 위도/경도 여유값
        val latThreshold = ERASER_THRESHOLD_METERS / 111000.0
        val lngThreshold = ERASER_THRESHOLD_METERS / 85000.0  // 한국 위도 기준 대략적인 값

        val minLat = sketchPoints.minOf { it.latitude } - latThreshold
        val maxLat = sketchPoints.maxOf { it.latitude } + latThreshold
        val minLng = sketchPoints.minOf { it.longitude } - lngThreshold
        val maxLng = sketchPoints.maxOf { it.longitude } + lngThreshold

        return point.latitude in minLat..maxLat && point.longitude in minLng..maxLng
    }

    // ──────────────────────────────────────────────
    // Undo / Redo
    // ──────────────────────────────────────────────

    /**
     * 마지막 액션을 취소한다.
     * - Create 취소 → soft delete
     * - Delete 취소 → restore
     */
    fun undo() {
        val action = undoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            when (action) {
                is SketchAction.Create -> {
                    sketchRepository.softDeleteSketch(action.sketch)
                }
                is SketchAction.Delete -> {
                    sketchRepository.restoreSketch(action.sketch)
                }
            }
            redoStack.addLast(action)
            updateUndoRedoState()
        }
    }

    /**
     * 마지막으로 취소한 액션을 다시 실행한다.
     * - Create 재실행 → restore
     * - Delete 재실행 → soft delete
     */
    fun redo() {
        val action = redoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            when (action) {
                is SketchAction.Create -> {
                    sketchRepository.restoreSketch(action.sketch)
                }
                is SketchAction.Delete -> {
                    sketchRepository.softDeleteSketch(action.sketch)
                }
            }
            undoStack.addLast(action)
            updateUndoRedoState()
        }
    }

    /**
     * Undo 스택에 액션을 추가한다.
     * 새 액션이 추가되면 Redo 스택은 비운다.
     */
    private fun pushUndoAction(action: SketchAction) {
        undoStack.addLast(action)
        redoStack.clear()
        updateUndoRedoState()
    }

    /** Undo/Redo 버튼 활성화 상태를 업데이트한다. */
    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    // ──────────────────────────────────────────────
    // 설정 변경
    // ──────────────────────────────────────────────

    /** 펜 색상을 변경한다. */
    fun setColor(hex: String) {
        _currentColor.value = hex
    }

    /** 펜 두께를 변경한다. */
    fun setStrokeWidth(width: Double) {
        _currentStrokeWidth.value = width
    }

    /** 펜 투명도를 변경한다. */
    fun setOpacity(opacity: Double) {
        _currentOpacity.value = opacity.coerceIn(0.0, 1.0)
    }

    // ──────────────────────────────────────────────
    // 전체 삭제
    // ──────────────────────────────────────────────

    /**
     * 모든 스케치를 삭제한다 (하드 삭제).
     * Undo/Redo 스택도 초기화한다.
     */
    fun deleteAllSketches() {
        viewModelScope.launch {
            sketchRepository.deleteAllSketches()
            undoStack.clear()
            redoStack.clear()
            updateUndoRedoState()
        }
    }
}
