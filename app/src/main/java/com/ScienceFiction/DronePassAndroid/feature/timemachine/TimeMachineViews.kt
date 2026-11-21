package com.ScienceFiction.DronePassAndroid.feature.timemachine

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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

/** iOS .orange */
internal val TimeMachineOrange = Color(0xFFFF9500)
internal val TimeMachineCircleSize = 32.dp
/** 드론 드롭다운 셰브런과 동그라미 사이. */
internal val TimeMachineCircleSpacing = 8.dp
private val TimeMachineShadow = 4.dp
private val TimeMachinePanelMaxWidthTablet = 420.dp
private val TrackDay = Color(red = 1.0f, green = 0.86f, blue = 0.45f)
private val TrackPastBlue = Color(0xFF007AFF)

/** 동그라미 문구가 1분마다 줄어들도록(+Nh·+N0m) 화면이 보는 지금. */
@Composable
private fun rememberMinuteTick(clock: TimeMachineClock, key: Any?): Long {
    var tick by remember { mutableLongStateOf(clock.nowMillis()) }
    LaunchedEffect(key) {
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

/** 드론 드롭다운 셰브런 오른쪽 동그라미(지금 회색 "now", 미래 주황 D+N / +Nh / +N0m). */
@Composable
fun TimeMachineCircleButton(clock: TimeMachineClock, modifier: Modifier = Modifier) {
    val selection by clock.selection.collectAsStateWithLifecycle()
    val now = rememberMinuteTick(clock, selection)
    val current = selection
    val isFuture = current != null
    val label = current?.let { TimeMachineSlot.label(it.timeMillis, now) } ?: stringResource(R.string.timemachine_now)
    val a11yTime = if (current != null) timeMachineDateTimeText(current.timeMillis) else stringResource(R.string.timemachine_now)
    val a11y = stringResource(R.string.timemachine_circle_a11y, a11yTime)
    Surface(
        shape = CircleShape,
        color = if (isFuture) TimeMachineOrange else MaterialTheme.colorScheme.surface,
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
                color = if (isFuture) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

/**
 * 두 줄 패널(D-58). 1줄: 날짜·시간(누르면 캘린더) + 현재로, 2줄: ‹ 슬라이더 ›.
 * @param sunCenter 슬라이더 일출·일몰 기준(지도 중심). 그릴 때마다 읽는다.
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
    val selection by clock.selection.collectAsStateWithLifecycle()
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
                timeMillis = selection?.timeMillis ?: clock.nowMillis(),
                isFuture = selection != null,
                onCalendar = { showsCalendar = true },
                onNow = { clock.returnToNow() },
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
    timeMillis: Long,
    isFuture: Boolean,
    onCalendar: () -> Unit,
    onNow: () -> Unit,
) {
    TimeMachinePill {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 32.dp)
                    .clickable(onClick = onCalendar),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val color = if (isFuture) TimeMachineOrange else MaterialTheme.colorScheme.onSurface
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text(
                    text = timeMachineDateTimeText(timeMillis),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onNow, enabled = isFuture, modifier = Modifier.heightIn(min = 32.dp)) {
                Text(
                    text = stringResource(R.string.timemachine_back_to_now),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TimeMachineSliderRow(
    clock: TimeMachineClock,
    selection: TimeMachineSelection?,
    sunCenter: () -> Pair<Double, Double>,
) {
    // selection 이 바뀔 때마다 다시 그린다(canStepDay·slot 은 그 값을 읽는다)
    val canBack = remember(selection) { clock.canStepDay(-1) }
    val canForward = remember(selection) { clock.canStepDay(1) }
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

/** 그날 00:00~24:00 10분 슬라이더. 끄는 동안 칸마다 기준 시각을 바꾼다(카드가 바로 따라온다). */
@Composable
private fun TimeMachineSlider(
    clock: TimeMachineClock,
    selection: TimeMachineSelection?,
    sunCenter: () -> Pair<Double, Double>,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val dark = isSystemInDarkTheme()
    val now = clock.nowMillis()
    val day = selection?.dayStartMillis ?: TimeMachineSlot.dayStart(now)
    val slot = clock.slot()
    val center = sunCenter()
    // 일출·일몰 칸(지도 중심 기준). 같은 날·같은 0.01° 안에서는 다시 계산하지 않는다
    val latKey = (center.first * 100).roundToInt()
    val lonKey = (center.second * 100).roundToInt()
    val sun = remember(day, latKey, lonKey) { TimeMachineSun.slots(TimeMachineSun.times(day, center.first, center.second)) }
    val past = if (day == TimeMachineSlot.dayStart(now)) TimeMachineSlot.firstSlot(day, now) else 0
    val isFuture = selection != null
    val latestSun by rememberUpdatedState(sun)
    val latestPast by rememberUpdatedState(past)

    val a11yLabel = stringResource(R.string.timemachine_slider)
    val beforeSunrise = stringResource(R.string.timemachine_before_sunrise)
    val afterSunset = stringResource(R.string.timemachine_after_sunset)
    val daytime = stringResource(R.string.timemachine_daytime)
    val timeText = remember(day, slot) {
        val minutes = slot * TimeMachineSlot.MINUTES
        "%d:%02d".format(Locale.ROOT, minutes / 60, minutes % 60)
    }
    val (rise, set) = sun
    val part = when {
        rise == null || set == null -> ""
        slot < rise -> beforeSunrise
        slot >= set -> afterSunset
        else -> daytime
    }
    val a11yValue = if (part.isEmpty()) timeText else "$timeText, $part"

    BoxWithConstraints(
        modifier = modifier
            .height(32.dp)
            .semantics {
                contentDescription = a11yLabel
                stateDescription = a11yValue
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = slot.toFloat(),
                    range = 0f..(TimeMachineSlot.COUNT - 1).toFloat(),
                    steps = TimeMachineSlot.COUNT - 2,
                )
                setProgress { target ->
                    clock.setSlot(target.roundToInt())
                    true
                }
            }
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
                        // 햅틱: 정시·일출·일몰을 지날 때 틱, 양 끝(오늘 지금 포함)에 닿으면 한 번(iOS 와 같음)
                        if (after != before) {
                            val lo = minOf(before, after) + 1
                            val hi = maxOf(before, after)
                            val (r, s) = latestSun
                            if ((lo..hi).any { it % 6 == 0 || it == r || it == s }) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            hitEdge = false
                        }
                        val atEdge = raw <= maxOf(0, latestPast) || raw >= TimeMachineSlot.COUNT - 1
                        if (atEdge && !hitEdge && (after == target || raw < latestPast)) {
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
        val widthPx = with(density) { maxWidth.toPx() }
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
                // 오늘의 과거(고를 수 없음)
                if (past > 0) {
                    drawRect(Color.Gray.copy(alpha = 0.75f), topLeft = Offset(0f, y), size = Size(past * unit, h))
                }
            }
            if (past > 0) {
                drawRect(
                    TrackPastBlue,
                    topLeft = Offset(past * unit - 1.dp.toPx(), y - 2.dp.toPx()),
                    size = Size(2.dp.toPx(), h + 4.dp.toPx()),
                )
            }
            if (dark) {
                drawPath(track, Color.White.copy(alpha = 0.15f), style = Stroke(width = 1.dp.toPx()))
            }
        }
        // 손잡이
        val thumbRadiusPx = with(density) { 11.dp.toPx() }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(((slot + 0.5f) * widthPx / TimeMachineSlot.COUNT - thumbRadiusPx).roundToInt(), 0) }
                .size(22.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(2.dp, if (isFuture) TimeMachineOrange else Color.Gray, CircleShape),
        )
    }
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
