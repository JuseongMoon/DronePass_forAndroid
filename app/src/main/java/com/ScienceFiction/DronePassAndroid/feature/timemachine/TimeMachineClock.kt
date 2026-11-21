package com.ScienceFiction.DronePassAndroid.feature.timemachine

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 판정 시각(D-85): 타임머신이 미래 시각을 가리키면 그 시각, 아니면 지금.
 * 저장 목록 분류·정렬, 지도 만료 회색, 만료 숨김만 이 시각을 쓴다.
 * 무료 한도, 종료일 알림, 동기화, 저장 때 기간 검증은 언제나 실제 지금이다.
 */
internal fun timeMachineJudgmentTime(selection: TimeMachineSelection?, nowMillis: Long): Long =
    selection?.timeMillis ?: nowMillis

/**
 * 타임머신 상태(D-58 화면, D-83 NOTAM 제외). 지도 화면·카드·도형 상세가 같은 선택을 본다.
 * - 동그라미: 지금 "now", 미래 주황 D+N / +Nh / +N0m. 누르면 두 줄 패널을 펼친다(이미 두 줄이면 접는다).
 * - 지도 빈 곳 탭: 미래면 1줄만 남기고, 지금이면 모두 접는다.
 * - 도형 "이 비행 일정으로 보기": 비행 시작 시각(날짜만이면 그날 일출을 10분 올림, 진행 중이면 지금), 패널 펼침.
 * 선택은 저장하지 않는다(앱을 새로 켜면 지금).
 */
@Singleton
class TimeMachineClock internal constructor(private val now: () -> Long) {

    @Inject constructor() : this(System::currentTimeMillis)

    enum class Panel {
        /** 동그라미만. */
        HIDDEN,
        /** 1줄(날짜·시간 + 현재로)만 — 미래 시각을 정하고 지도 빈 곳을 눌렀을 때. */
        DATE_ONLY,
        /** 두 줄 모두. */
        FULL,
    }

    private val _selection = MutableStateFlow<TimeMachineSelection?>(null)
    /** 지금 보는 선택(null = 실제 지금). */
    val selection: StateFlow<TimeMachineSelection?> = _selection.asStateFlow()

    private val _panel = MutableStateFlow(Panel.HIDDEN)
    val panel: StateFlow<Panel> = _panel.asStateFlow()

    val isFuture: Boolean get() = _selection.value != null

    fun nowMillis(): Long = now()

    /** 패널이 보여 주는 날(KST 00:00). 지금이면 오늘. */
    fun day(): Long = _selection.value?.dayStartMillis ?: TimeMachineSlot.dayStart(now())

    /** 패널이 보여 주는 칸. 지금이면 지금 뒤 첫 칸. */
    fun slot(): Int {
        _selection.value?.let { return it.slot }
        val n = now()
        return minOf(TimeMachineSlot.COUNT - 1, TimeMachineSlot.firstSlot(n, n))
    }

    /** 패널이 보여 주는 시각. 지금이면 실제 지금. */
    fun time(): Long = _selection.value?.timeMillis ?: now()

    /** 동그라미 문구. 지금이면 null(화면이 지역화한 "now"를 쓴다). */
    fun badge(): String? = _selection.value?.let { TimeMachineSlot.label(it.timeMillis, now()) }

    private fun apply(resolved: TimeMachineSlot.Resolved, shapeId: String? = _selection.value?.shapeId) {
        val new = TimeMachineSelection(resolved.dayStartMillis, resolved.slot, shapeId)
        if (_selection.value != new) _selection.value = new
    }

    /** 동그라미: 펼친다. 이미 두 줄이면 접는다(미래면 1줄, 지금이면 모두). */
    fun tapCircle() {
        _panel.value = if (_panel.value == Panel.FULL) {
            if (isFuture) Panel.DATE_ONLY else Panel.HIDDEN
        } else {
            Panel.FULL
        }
    }

    fun expand() {
        _panel.value = Panel.FULL
    }

    /** 지도 빈 곳 탭: 미래면 2줄만 숨기고(1줄 + 주황 동그라미), 지금이면 모두 접는다. */
    fun collapse() {
        if (_panel.value == Panel.HIDDEN) return
        _panel.value = if (isFuture) Panel.DATE_ONLY else Panel.HIDDEN
    }

    /** 슬라이더 칸(그날 안에서, 오늘이면 지금 앞 칸 금지). */
    fun setSlot(slot: Int) {
        val r = TimeMachineSlot.resolve(now(), day(), slot)
        val current = _selection.value
        if (current != null && r.dayStartMillis == current.dayStartMillis && r.slot == current.slot) return
        apply(r)
    }

    /** ‹ › 하루씩(시각 유지). 오늘이면 지금 뒤 첫 칸으로 맞춘다. */
    fun stepDay(delta: Int) {
        if (!canStepDay(delta)) return
        apply(TimeMachineSlot.resolve(now(), TimeMachineSlot.addDays(day(), delta), slot()))
    }

    fun canStepDay(delta: Int): Boolean = TimeMachineSlot.canStepDay(now(), day(), delta)

    /** 캘린더에서 날 고르기(시각 유지). */
    fun pickDay(dayMillis: Long) {
        apply(TimeMachineSlot.resolve(now(), dayMillis, slot()))
    }

    /** "현재로": 지금으로 돌아오고 패널을 모두 숨긴다. */
    fun returnToNow() {
        _selection.value = null
        _panel.value = Panel.HIDDEN
    }

    /** 도형 "이 비행 일정으로 보기": 비행 시작 시각으로 보고 패널을 펼친다. 진행 중이면 지금 그대로 패널만 펼친다. */
    fun showShapeSchedule(shape: ShapeModel, zone: ZoneId = ZoneId.systemDefault()) {
        when (val result = TimeMachineShapeSchedule.resolve(shape, now(), zone)) {
            TimeMachineShapeSchedule.Result.Unavailable -> return
            TimeMachineShapeSchedule.Result.Now -> _selection.value = null
            is TimeMachineShapeSchedule.Result.At -> apply(result.resolved, shapeId = shape.id)
        }
        _panel.value = Panel.FULL
    }

    fun canShowSchedule(shape: ShapeModel, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        TimeMachineShapeSchedule.resolve(shape, now(), zone) != TimeMachineShapeSchedule.Result.Unavailable
}
