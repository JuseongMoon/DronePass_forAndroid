package com.ScienceFiction.DronePassAndroid.feature.sketch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.util.DistanceCalculator
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.cos
import javax.inject.Inject

internal const val SketchEraserThresholdMeters = 30.0

/**
 * 스케치 기능의 ViewModel.
 *
 * 자유 그리기, 지우개, Undo/Redo 기능을 제공한다.
 * SketchRepository를 통해 스케치 데이터를 영속화한다.
 */
@HiltViewModel
class SketchViewModel @Inject constructor(
    private val sketchRepository: SketchRepository,
    private val analyticsLogger: AnalyticsLogger,
    private val dataStore: DataStore<Preferences>
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
    private val _currentColor = MutableStateFlow(DefaultSketchColor)
    val currentColor: StateFlow<String> = _currentColor.asStateFlow()

    /** 현재 펜 두께 */
    private val _currentStrokeWidth = MutableStateFlow(DefaultSketchStrokeWidth)
    val currentStrokeWidth: StateFlow<Double> = _currentStrokeWidth.asStateFlow()

    /** 현재 펜 투명도 (0.0 ~ 1.0) */
    private val _currentOpacity = MutableStateFlow(DefaultSketchOpacity)
    val currentOpacity: StateFlow<Double> = _currentOpacity.asStateFlow()

    /**
     * 현재 그리는 중인 포인트 버퍼.
     * 내부 ArrayList 에 add 후 toList() 로 새 List 인스턴스를 emit 하여 StateFlow 가
     * 변경을 감지하도록 한다. (이전: _currentDrawingPoints.value + coordinate 는 매 호출
     * 마다 전체 리스트 복사 → O(n²). 새 패턴은 add O(1) + toList O(n) → O(n).)
     */
    private val drawingBuffer = mutableListOf<Coordinate>()

    /** 현재 그리는 중인 포인트 리스트 (실시간 프리뷰용) */
    private val _currentDrawingPoints = MutableStateFlow<List<Coordinate>>(emptyList())
    val currentDrawingPoints: StateFlow<List<Coordinate>> = _currentDrawingPoints.asStateFlow()

    /** 지우개 동시 호출 직렬화용 Mutex. 빠른 드래그 시 N 개 코루틴이 race 로 중복 삭제하던 문제 차단. */
    private val eraserMutex = Mutex()

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
        data class Delete(val sketches: List<SketchModel>) : SketchAction()
    }

    private val undoStack = ArrayDeque<SketchAction>()
    private val redoStack = ArrayDeque<SketchAction>()
    private var latestSketchMutationJob: Job? = null
    private var sketchModeEnterTimeMillis: Long? = null

    /** 마지막으로 샘플링된 포인트 (최소 거리 필터링용) */
    private var lastSampledPoint: Coordinate? = null

    /** 최소 샘플링 거리 (미터) */
    private companion object {
        const val MIN_SAMPLING_DISTANCE_METERS = 5.0
    }

    init {
        viewModelScope.launch {
            val preferences = dataStore.data.first()
            _currentColor.value = preferences[SketchPreferenceKeys.CURRENT_COLOR] ?: DefaultSketchColor
            _currentStrokeWidth.value = clampSketchStrokeWidth(
                preferences[SketchPreferenceKeys.CURRENT_STROKE_WIDTH] ?: DefaultSketchStrokeWidth
            )
            _currentOpacity.value = clampSketchOpacity(
                preferences[SketchPreferenceKeys.CURRENT_OPACITY] ?: DefaultSketchOpacity
            )
        }
    }

    // ──────────────────────────────────────────────
    // 스케치 모드 진입/종료
    // ──────────────────────────────────────────────

    /**
     * 스케치 모드로 진입한다.
     */
    fun enterSketchMode() {
        val transition = enterSketchModeTransition(
            isSketchModeActive = _isSketchMode.value,
            isEraserModeActive = _isEraserMode.value,
        )
        if (!transition.shouldTransition) return

        drawingBuffer.clear()
        _currentDrawingPoints.value = emptyList()
        lastSampledPoint = null
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoState()
        _isSketchMode.value = transition.isSketchModeActive
        _isEraserMode.value = transition.isEraserModeActive
        sketchModeEnterTimeMillis = System.currentTimeMillis()
        analyticsLogger.logSketchModeEntered()
    }

    /**
     * 스케치 모드를 종료한다.
     * 진행 중인 그리기가 있으면 iOS처럼 완료 처리한다.
     */
    fun exitSketchMode() {
        val transition = exitSketchModeTransition(
            isSketchModeActive = _isSketchMode.value,
            isEraserModeActive = _isEraserMode.value,
        )
        if (!transition.shouldTransition) return

        if (drawingBuffer.isNotEmpty()) {
            finishDrawing(recordUndo = false)
        } else {
            _currentDrawingPoints.value = emptyList()
        }
        lastSampledPoint = null
        _isSketchMode.value = transition.isSketchModeActive
        _isEraserMode.value = transition.isEraserModeActive
        syncSketchesOnModeComplete()
        sketchModeEnterTimeMillis?.let { enteredAt ->
            val durationSeconds = ((System.currentTimeMillis() - enteredAt) / 1000L).toInt()
            analyticsLogger.logSketchModeExited(durationSeconds)
        }
        sketchModeEnterTimeMillis = null
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
        drawingBuffer.clear()
        drawingBuffer.add(coordinate)
        _currentDrawingPoints.value = drawingBuffer.toList()
        lastSampledPoint = coordinate
    }

    /**
     * 그리기를 계속한다. 최소 5m 샘플링 적용.
     * 내부 ArrayList 에 add 후 toList() 로 emit 하여 매 호출의 리스트 복사 비용을
     * O(이전 길이) 에서 O(1)+O(현재 길이) 로 분산. 누적 시간복잡도 O(n).
     */
    fun continueDrawing(coordinate: Coordinate) {
        if (_isEraserMode.value) return
        val lastPoint = lastSampledPoint ?: return

        val distance = DistanceCalculator.fastApprox(lastPoint, coordinate)
        if (distance >= MIN_SAMPLING_DISTANCE_METERS) {
            drawingBuffer.add(coordinate)
            _currentDrawingPoints.value = drawingBuffer.toList()
            lastSampledPoint = coordinate
        }
    }

    /**
     * 그리기를 완료한다.
     * 포인트가 2개 이상이면 스케치를 저장한다.
     */
    fun finishDrawing(recordUndo: Boolean = true) {
        val points = drawingBuffer.toList()
        drawingBuffer.clear()
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

        launchSketchMutation {
            sketchRepository.insertSketch(sketch)
            if (recordUndo) {
                pushUndoAction(SketchAction.Create(sketch))
            }
            val counts = sketchRepository.getSketchCounts()
            analyticsLogger.logSketchSaved(
                totalCount = counts.totalCount,
                activeCount = counts.activeCount,
            )
        }

        _currentDrawingPoints.value = emptyList()
        lastSampledPoint = null
    }

    /**
     * 현재 그리는 중인 선을 저장하지 않고 취소한다.
     * 2손가락 지도 조작으로 전환되거나 시스템 터치 취소가 발생할 때 사용한다.
     */
    fun cancelDrawing() {
        drawingBuffer.clear()
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
            cancelDrawing()
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
        launchSketchMutation {
            // 동일 드래그 안에서 빠르게 연속 호출되는 N 개 코루틴 사이의 race 를 직렬화한다.
            // (이전: 동시 삭제로 같은 스케치가 두 번 softDelete 되거나, 여러 스케치가 동시 삭제됨.)
            eraserMutex.withLock {
                val sketch = findClosestErasableSketch(
                    point = point,
                    sketches = activeSketches.value,
                ) ?: return@withLock

                sketchRepository.softDeleteSketch(sketch)
                pushUndoAction(SketchAction.Delete(listOf(sketch)))
                analyticsLogger.logSketchDeleted(
                    activeCount = activeSketches.value.count { !it.isDeleted && it.id != sketch.id },
                )
            }
        }
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
        launchSketchMutation {
            when (action) {
                is SketchAction.Create -> {
                    sketchRepository.softDeleteSketch(action.sketch)
                }
                is SketchAction.Delete -> {
                    action.sketches.forEach { sketchRepository.restoreSketch(it) }
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
        launchSketchMutation {
            when (action) {
                is SketchAction.Create -> {
                    sketchRepository.restoreSketch(action.sketch)
                }
                is SketchAction.Delete -> {
                    action.sketches.forEach { sketchRepository.softDeleteSketch(it) }
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
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SketchPreferenceKeys.CURRENT_COLOR] = hex
            }
        }
    }

    /** 펜 두께를 변경한다. */
    fun setStrokeWidth(width: Double) {
        val clampedWidth = clampSketchStrokeWidth(width)
        _currentStrokeWidth.value = clampedWidth
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SketchPreferenceKeys.CURRENT_STROKE_WIDTH] = clampedWidth
            }
        }
    }

    /** 펜 투명도를 변경한다. */
    fun setOpacity(opacity: Double) {
        val clampedOpacity = clampSketchOpacity(opacity)
        _currentOpacity.value = clampedOpacity
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SketchPreferenceKeys.CURRENT_OPACITY] = clampedOpacity
            }
        }
    }

    // ──────────────────────────────────────────────
    // 전체 삭제
    // ──────────────────────────────────────────────

    /**
     * 모든 활성 스케치를 소프트 삭제한다.
     * iOS처럼 하나의 Undo 액션으로 전체 복구할 수 있게 삭제 전 목록을 저장한다.
     */
    fun deleteAllSketches() {
        launchSketchMutation {
            val deletedSketches = sketchRepository.deleteAllSketches()
            if (deletedSketches.isNotEmpty()) {
                undoStack.addLast(SketchAction.Delete(deletedSketches))
                redoStack.clear()
                updateUndoRedoState()
                analyticsLogger.logSketchAllCleared(deletedSketches.size)
            }
        }
    }

    private fun launchSketchMutation(block: suspend () -> Unit): Job {
        val previousJob = latestSketchMutationJob
        val job = viewModelScope.launch {
            previousJob?.join()
            block()
        }
        latestSketchMutationJob = job
        return job
    }

    private fun syncSketchesOnModeComplete() {
        val previousJob = latestSketchMutationJob
        viewModelScope.launch {
            previousJob?.join()
            sketchRepository.syncToFirebaseOnComplete()
        }
    }
}

internal fun findClosestErasableSketch(
    point: Coordinate,
    sketches: List<SketchModel>,
    thresholdMeters: Double = SketchEraserThresholdMeters,
): SketchModel? {
    return sketches
        .asSequence()
        .filter { !it.isDeleted && it.points.isNotEmpty() }
        .filter { isPointNearSketchBoundingBox(point, it.points, thresholdMeters) }
        .mapNotNull { sketch ->
            val distance = minDistanceToSketch(point, sketch.points)
            if (distance <= thresholdMeters) sketch to distance else null
        }
        .minByOrNull { (_, distance) -> distance }
        ?.first
}

internal fun minDistanceToSketch(
    point: Coordinate,
    sketchPoints: List<Coordinate>,
): Double {
    return when (sketchPoints.size) {
        0 -> Double.POSITIVE_INFINITY
        1 -> DistanceCalculator.haversine(point, sketchPoints[0])
        else -> sketchPoints
            .zipWithNext()
            .minOf { (start, end) ->
                DistanceCalculator.distanceToSegment(point, start, end)
            }
    }
}

/**
 * BoundingBox 내에 포인트가 있는지 빠르게 확인한다.
 * 경도 1° 당 거리는 위도에 따라 달라지므로 iOS처럼 터치 지점 위도로 cosLat을 보정한다.
 */
internal fun isPointNearSketchBoundingBox(
    point: Coordinate,
    sketchPoints: List<Coordinate>,
    thresholdMeters: Double = SketchEraserThresholdMeters,
): Boolean {
    if (sketchPoints.isEmpty()) return false

    val latThreshold = thresholdMeters / 111_000.0
    val pointLatRad = Math.toRadians(point.latitude)
    val cosLat = abs(cos(pointLatRad)).coerceAtLeast(0.01)
    val lngThreshold = thresholdMeters / (111_000.0 * cosLat)

    val minLat = sketchPoints.minOf { it.latitude } - latThreshold
    val maxLat = sketchPoints.maxOf { it.latitude } + latThreshold
    val minLng = sketchPoints.minOf { it.longitude } - lngThreshold
    val maxLng = sketchPoints.maxOf { it.longitude } + lngThreshold

    return point.latitude in minLat..maxLat && point.longitude in minLng..maxLng
}
