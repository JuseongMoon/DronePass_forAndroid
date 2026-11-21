package com.ScienceFiction.DronePassAndroid.feature.timemachine

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.ui.theme.withLabelSafeLineHeights
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

/** 화면이 같은 [TimeMachineClock](싱글턴)을 받는 통로. */
@HiltViewModel
class TimeMachineViewModel @Inject constructor(val clock: TimeMachineClock) : ViewModel()

@Composable
fun rememberTimeMachineClock(): TimeMachineClock = hiltViewModel<TimeMachineViewModel>().clock

/** iOS .orange — 미래·지금이 든 칸 */
internal val TimeMachineOrange = Color(0xFFFF9500)
/** iOS systemIndigo — 오늘 지난 칸(D-87). 라이트 #5856D6, 다크 #5E5CE6 */
internal val TimeMachineIndigoLight = Color(0xFF5856D6)
internal val TimeMachineIndigoDark = Color(0xFF5E5CE6)

/** 고른 시각의 색(D-87): 미래·지금이 든 칸 주황, 지난 칸 남색. 고르지 않았으면 null. */
internal fun timeMachineTint(tense: TimeMachineSlot.Tense?, dark: Boolean): Color? = when (tense) {
    null -> null
    TimeMachineSlot.Tense.PAST -> if (dark) TimeMachineIndigoDark else TimeMachineIndigoLight
    TimeMachineSlot.Tense.FUTURE, TimeMachineSlot.Tense.NOW -> TimeMachineOrange
}
internal val TimeMachineCircleSize = 32.dp
/** 드론 드롭다운 셰브런과 동그라미 사이. */
internal val TimeMachineCircleSpacing = 8.dp
private val TimeMachineShadow = 4.dp
private val TimeMachinePanelMaxWidthTablet = 420.dp
private val TrackDay = Color(red = 1.0f, green = 0.86f, blue = 0.45f)
/** 오늘이면 지금 위치 선(iOS .blue). */
private val TrackPastBlue = Color(0xFF007AFF)

/** 동그라미 문구가 1분마다 줄어들도록(+Nh·+N0m) 화면이 보는 지금. 칸이 바뀌어도 다시 시작하지 않는다. */
@Composable
private fun rememberMinuteTick(clock: TimeMachineClock): Long {
    var tick by remember { mutableLongStateOf(clock.nowMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            tick = clock.nowMillis()
            delay(60_000)
        }
    }
    return tick
}

/** "11월 9일 (월) 14:30" — iOS MMMdE 템플릿 + 시:분(KST). */
@Composable
internal fun timeMachineDateTimeText(millis: Long): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val formatter = remember(locale) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMdE")
        DateTimeFormatter.ofPattern("$pattern HH:mm", locale).withZone(TimeMachineZone)
    }
    return formatter.format(Instant.ofEpochMilli(millis))
}

/**
 * 드론 드롭다운 셰브런 오른쪽 동그라미. 지금: 흰 바탕 회색 "now", 미래·지금이 든 칸: 주황 바탕 흰 글자
 * "+2h"·"D+3"·"now", 오늘 지난 칸: 남색 바탕 흰 글자 "−2h"·"−30m"(D-87).
 */
@Composable
fun TimeMachineCircleButton(clock: TimeMachineClock, modifier: Modifier = Modifier) {
    val selection by clock.selection.collectAsStateWithLifecycle()
    val now = rememberMinuteTick(clock)
    val current = selection
    val isPinned = current != null
    val tint = timeMachineTint(current?.let { TimeMachineSlot.tense(it.timeMillis, now) }, isSystemInDarkTheme())
    val label = current?.let { TimeMachineSlot.label(it.timeMillis, now) } ?: stringResource(R.string.timemachine_now)
    val a11yTime = if (current != null) timeMachineDateTimeText(current.timeMillis) else stringResource(R.string.timemachine_now)
    val a11y = stringResource(R.string.timemachine_circle_a11y, a11yTime)
    Surface(
        shape = CircleShape,
        color = tint ?: MaterialTheme.colorScheme.surface,
        shadowElevation = TimeMachineShadow,
        modifier = modifier
            .requiredSize(TimeMachineCircleSize)
            .semantics { contentDescription = a11y },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clickable { clock.tapCircle() },
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                color = if (isPinned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

/**
 * 두 줄 패널(D-58). 1줄: 날짜·시간(누르면 캘린더) + 현재로, 2줄: ‹ 슬라이더 ›.
 * 슬라이더를 끄는 동안 칸마다 바뀌는 선택은 날짜 글자·손잡이 그리기에서만 읽는다(패널 전체를 다시 구성하지 않는다).
 * @param sunCenter 슬라이더 일출·일몰 기준(지도 중심). 날이 바뀔 때 읽는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeMachinePanel(
    clock: TimeMachineClock,
    sunCenter: () -> Pair<Double, Double>,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
) {
    val panel by clock.panel.collectAsStateWithLifecycle()
    val selection = clock.selection.collectAsStateWithLifecycle()
    var showsCalendar by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.then(if (isTablet) Modifier.widthIn(max = TimeMachinePanelMaxWidthTablet) else Modifier.fillMaxWidth()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(
            visible = panel != TimeMachineClock.Panel.HIDDEN,
            enter = fadeIn(tween(180)) + expandVertically(tween(180)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
        ) {
            TimeMachineDateRow(
                clock = clock,
                selection = selection,
                onCalendar = { showsCalendar = true },
            )
        }
        AnimatedVisibility(
            visible = panel == TimeMachineClock.Panel.FULL,
            enter = fadeIn(tween(180)) + expandVertically(tween(180)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
        ) {
            TimeMachineSliderRow(clock = clock, selection = selection, sunCenter = sunCenter)
        }
    }

    if (showsCalendar) {
        TimeMachineCalendarDialog(clock = clock, onDismiss = { showsCalendar = false })
    }
}

@Composable
private fun TimeMachinePill(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = TimeMachineShadow,
        modifier = modifier.fillMaxWidth(),
        content = content,
    )
}

@Composable
private fun TimeMachineDateRow(
    clock: TimeMachineClock,
    selection: State<TimeMachineSelection?>,
    onCalendar: () -> Unit,
) {
    val isPinned by remember { derivedStateOf { selection.value != null } }
    // 시제는 칸이 바뀌어도 미래·지금·지난이 바뀔 때만 다시 그린다
    val tense by remember { derivedStateOf { selection.value?.let { TimeMachineSlot.tense(it.timeMillis, clock.nowMillis()) } } }
    val tint = timeMachineTint(tense, isSystemInDarkTheme())
    TimeMachinePill {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 날짜·시간이 남은 폭을 모두 쓴다(빈 칸과 나누면 한국어 날짜의 시각이 잘린다)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 32.dp)
                    .clickable(onClick = onCalendar),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val color = tint ?: MaterialTheme.colorScheme.onSurface
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                TimeMachineDateText(clock = clock, selection = selection, color = color)
            }
            TextButton(onClick = { clock.returnToNow() }, enabled = isPinned, modifier = Modifier.heightIn(min = 32.dp)) {
                Text(
                    text = stringResource(R.string.timemachine_back_to_now),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** 칸마다 바뀌는 글자만 따로 둔다(이 함수만 다시 구성된다). */
@Composable
private fun TimeMachineDateText(clock: TimeMachineClock, selection: State<TimeMachineSelection?>, color: Color) {
    Text(
        text = timeMachineDateTimeText(selection.value?.timeMillis ?: clock.nowMillis()),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
    )
}

@Composable
private fun TimeMachineSliderRow(
    clock: TimeMachineClock,
    selection: State<TimeMachineSelection?>,
    sunCenter: () -> Pair<Double, Double>,
) {
    // 하루 넘기기 가능 여부는 날이 바뀔 때만 달라진다
    val canBack by remember { derivedStateOf { selection.value?.dayStartMillis; clock.canStepDay(-1) } }
    val canForward by remember { derivedStateOf { selection.value?.dayStartMillis; clock.canStepDay(1) } }
    TimeMachinePill {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DayStepButton(
                enabled = canBack,
                description = stringResource(R.string.timemachine_prev_day),
                forward = false,
                onClick = { clock.stepDay(-1) },
            )
            TimeMachineSlider(
                clock = clock,
                selection = selection,
                sunCenter = sunCenter,
                modifier = Modifier.weight(1f),
            )
            DayStepButton(
                enabled = canForward,
                description = stringResource(R.string.timemachine_next_day),
                forward = true,
                onClick = { clock.stepDay(1) },
            )
        }
    }
}

@Composable
private fun DayStepButton(enabled: Boolean, description: String, forward: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .requiredSize(32.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else 0.35f),
    ) {
        Icon(
            imageVector = if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * 그날 00:00~24:00 10분 슬라이더. 끄는 동안 칸마다 기준 시각을 바꾼다(카드가 바로 따라온다).
 * 오늘이면 지금 위치에 파란 선을 긋고, 지난 칸도 고를 수 있다(D-87).
 * 칸은 그리기 단계에서만 읽어 손잡이를 옮긴다. 다시 구성하는 것은 날이 바뀔 때(일출·일몰·오늘 과거)뿐이다.
 */
@Composable
private fun TimeMachineSlider(
    clock: TimeMachineClock,
    selection: State<TimeMachineSelection?>,
    sunCenter: () -> Pair<Double, Double>,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val dark = isSystemInDarkTheme()
    val day by remember { derivedStateOf { selection.value?.dayStartMillis ?: TimeMachineSlot.dayStart(clock.nowMillis()) } }
    val tense = remember { derivedStateOf { selection.value?.let { TimeMachineSlot.tense(it.timeMillis, clock.nowMillis()) } } }
    val slot = remember { derivedStateOf { selection.value?.slot ?: clock.slot() } }
    // 일출·일몰 칸(지도 중심 기준)과 오늘의 지금 위치는 날이 바뀔 때 계산한다
    val sun = remember(day) {
        val center = sunCenter()
        TimeMachineSun.slots(TimeMachineSun.times(day, center.first, center.second))
    }
    // 오늘이면 지금 위치(칸 단위, 소수). 지난 칸도 고를 수 있어 가리지 않고 선만 긋는다(D-87)
    val nowAt: Float? = remember(day) {
        val now = clock.nowMillis()
        if (day == TimeMachineSlot.dayStart(now)) (now - day) / TimeMachineSlot.SLOT_MILLIS.toFloat() else null
    }
    val latestSun by rememberUpdatedState(sun)
    val latestNowSlot by rememberUpdatedState(nowAt?.toInt())
    val (rise, set) = sun

    Box(
        modifier = modifier
            .height(32.dp)
            .pointerInput(Unit) {
                // iOS DragGesture(minimumDistance: 0): 누른 곳으로 바로 가고 끄는 동안 칸마다 바꾼다
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val width = size.width.toFloat()
                    var hitEdge = false
                    fun moveTo(x: Float) {
                        val raw = (x / maxOf(width, 1f) * TimeMachineSlot.COUNT).toInt()
                        val target = raw.coerceIn(0, TimeMachineSlot.COUNT - 1)
                        val before = clock.slot()
                        clock.setSlot(target)
                        val after = clock.slot()
                        // 햅틱: 정시·일출·일몰·지금을 지날 때 틱, 양 끝에 닿으면 한 번(iOS 와 같음)
                        if (after != before) {
                            val lo = minOf(before, after) + 1
                            val hi = maxOf(before, after)
                            val (r, s) = latestSun
                            val nowSlot = latestNowSlot
                            if ((lo..hi).any { it % 6 == 0 || it == r || it == s || it == nowSlot }) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            hitEdge = false
                        }
                        val atEdge = raw <= 0 || raw >= TimeMachineSlot.COUNT - 1
                        if (atEdge && !hitEdge && after == target) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            hitEdge = true
                        }
                    }
                    moveTo(down.position.x)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        moveTo(change.position.x)
                        change.consume()
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = 14.dp.toPx()
            val y = (size.height - h) / 2
            val unit = size.width / TimeMachineSlot.COUNT
            val track = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(0f, y, size.width, y + h, CornerRadius(h / 2, h / 2)),
                )
            }
            clipPath(track) {
                // 낮(밝음) 바탕, 밤 검정
                drawRect(TrackDay, topLeft = Offset(0f, y), size = Size(size.width, h))
                val night = Color.Black.copy(alpha = if (dark) 1f else 0.85f)
                if (rise != null && set != null) {
                    drawRect(night, topLeft = Offset(0f, y), size = Size(rise * unit, h))
                    drawRect(night, topLeft = Offset(set * unit, y), size = Size(size.width - set * unit, h))
                    for (s in listOf(rise, set)) {
                        if (s > 0 && s < TimeMachineSlot.COUNT) {
                            drawRect(TimeMachineOrange, topLeft = Offset(s * unit - 1.dp.toPx(), y), size = Size(2.dp.toPx(), h))
                        }
                    }
                } else {
                    drawRect(night, topLeft = Offset(0f, y), size = Size(size.width, h))
                }
                // 3시간 눈금
                for (hour in 3 until 24 step 3) {
                    val x = hour * 6 * unit
                    drawRect(
                        Color.Gray.copy(alpha = 0.7f),
                        topLeft = Offset(x - 0.5.dp.toPx(), y + h - 4.dp.toPx()),
                        size = Size(1.dp.toPx(), 4.dp.toPx()),
                    )
                }
            }
            // 오늘이면 지금 위치(지난 칸과 앞 칸의 경계)
            if (nowAt != null) {
                drawRect(
                    TrackPastBlue,
                    topLeft = Offset(nowAt * unit - 1.dp.toPx(), y - 2.dp.toPx()),
                    size = Size(2.dp.toPx(), h + 4.dp.toPx()),
                )
            }
            if (dark) {
                drawPath(track, Color.White.copy(alpha = 0.15f), style = Stroke(width = 1.dp.toPx()))
            }
            // 손잡이(22dp 흰 원, 그림자, 테두리 2dp — 미래·지금 칸 주황, 지난 칸 남색, 고르지 않음 회색). 칸은 여기서만 읽는다
            val radius = 11.dp.toPx()
            val center = Offset((slot.value + 0.5f) * unit, size.height / 2)
            drawCircle(Color.Black.copy(alpha = 0.18f), radius = radius + 1.dp.toPx(), center = center + Offset(0f, 1.dp.toPx()))
            drawCircle(Color.White, radius = radius, center = center)
            drawCircle(
                timeMachineTint(tense.value, dark) ?: Color.Gray,
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        TimeMachineSliderSemantics(clock = clock, slot = slot, sun = sun, modifier = Modifier.matchParentSize())
    }
}

/** TalkBack: 현재 칸 시각과 낮·밤 구분, 위·아래 쓸기로 한 칸씩 조절. 칸마다 바뀌는 글은 이 층만 다시 구성한다. */
@Composable
private fun TimeMachineSliderSemantics(
    clock: TimeMachineClock,
    slot: State<Int>,
    sun: Pair<Int?, Int?>,
    modifier: Modifier,
) {
    val a11yLabel = stringResource(R.string.timemachine_slider)
    val beforeSunrise = stringResource(R.string.timemachine_before_sunrise)
    val afterSunset = stringResource(R.string.timemachine_after_sunset)
    val daytime = stringResource(R.string.timemachine_daytime)
    val current = slot.value
    val minutes = current * TimeMachineSlot.MINUTES
    val timeText = "%d:%02d".format(Locale.ROOT, minutes / 60, minutes % 60)
    val (rise, set) = sun
    val part = when {
        rise == null || set == null -> ""
        current < rise -> beforeSunrise
        current >= set -> afterSunset
        else -> daytime
    }
    val a11yValue = if (part.isEmpty()) timeText else "$timeText, $part"
    Box(
        modifier = modifier.semantics {
            contentDescription = a11yLabel
            stateDescription = a11yValue
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = current.toFloat(),
                range = 0f..(TimeMachineSlot.COUNT - 1).toFloat(),
                steps = TimeMachineSlot.COUNT - 2,
            )
            setProgress { target ->
                clock.setSlot(target.roundToInt())
                true
            }
        },
    )
}

/** 날짜 고르기(날짜만, 오늘~+27일, KST). 시각은 그대로 둔다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeMachineCalendarDialog(clock: TimeMachineClock, onDismiss: () -> Unit) {
    val today = remember { TimeMachineSlot.localDate(clock.nowMillis()) }
    val last = remember(today) { today.plusDays(TimeMachineLimits.MAX_DAYS.toLong()) }
    val selected = remember { TimeMachineSlot.localDate(clock.day()) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return !d.isBefore(today) && !d.isAfter(last)
            }

            override fun isSelectableYear(year: Int): Boolean = year in today.year..last.year
        },
    )
    val confirm: () -> Unit = {
        state.selectedDateMillis?.let { utc ->
            val d: LocalDate = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
            clock.pickDay(d.atStartOfDay(TimeMachineZone).toInstant().toEpochMilli())
        }
        onDismiss()
    }
    // 날짜 선택기의 직접 입력 필드 라벨 보간이 줄 높이 단위 차이로 죽지 않도록(Type.kt withLabelSafeLineHeights 참고)
    MaterialTheme(typography = MaterialTheme.typography.withLabelSafeLineHeights()) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = confirm) { Text(stringResource(R.string.common_confirm)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
        ) {
            MaterialTheme(typography = MaterialTheme.typography.withLabelSafeLineHeights()) {
                DatePicker(
                    state = state,
                    title = {
                        Text(
                            text = stringResource(R.string.timemachine_calendar_title),
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        )
                    },
                )
            }
        }
    }
}
