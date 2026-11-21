package com.ScienceFiction.DronePassAndroid.feature.shape

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.parseIosOpaqueRgbHexColor
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 지도 없는 공유 카드(D-71, iOS ShapeShareSchematicView)의 구도 계산. 길이는 모두 dp 다.
 * - 원: 칸 높이에 맞춘 원(배율 1) + 축척 막대(1·2·5×10ⁿ) + 축척 거리의 정수배 격자.
 *   두 손가락으로 돌리고 확대·축소한다(1/8~4배). 축척·격자는 배율에 따라 다시 계산한다
 * - 원이 아니거나 반경이 없으면: 기준 위치에 점만 찍는다(거리 기준이 없어 축척·격자·회전·배율이 없다)
 */
internal class ShapeShareSchematicLayout(shape: ShapeModel, width: Float, height: Float, zoom: Double = 1.0) {
    /** 원 반지름(dp). 원이 아니면 null */
    val circleRadius: Float?
    /** 축척 막대가 나타내는 거리(m)와 길이(dp). 원이 아니면 null */
    val scaleMeters: Double?
    val scaleBarLength: Float?
    /** 격자 간격(dp) = 축척 길이 × [gridMultiple]. 원이 아니면 null */
    val gridSpacing: Float?
    val gridMultiple: Int?

    init {
        val radius = shape.radius
        if (shape.shapeType != ShapeType.CIRCLE || radius == null || radius <= 0) {
            circleRadius = null
            scaleMeters = null
            scaleBarLength = null
            gridSpacing = null
            gridMultiple = null
        } else {
            val pointRadius = ((min(width, height) / 2 - FIT_PADDING) * clampedZoom(zoom)).toFloat()
            val metersPerPoint = radius / pointRadius
            val meters = niceDistance(MAX_SCALE_BAR_LENGTH * metersPerPoint)
            val length = (meters / metersPerPoint).toFloat()
            val multiple = max(1, ceil(MIN_GRID_SPACING / length).toInt())
            circleRadius = pointRadius
            scaleMeters = meters
            scaleBarLength = length
            gridMultiple = multiple
            gridSpacing = length * multiple
        }
    }

    /** 돌릴 수 있는가(거리 기준이 있는 원만) */
    val isRotatable: Boolean get() = circleRadius != null

    companion object {
        /** 원 둘레와 칸 가장자리 사이 여백. 지도 카드의 fit 여백과 같다 */
        const val FIT_PADDING = 40f
        /** 축척 막대 최대 길이 */
        const val MAX_SCALE_BAR_LENGTH = 50f
        /** 격자 간격 최소. 이 값 이상이 되는 축척 거리의 가장 작은 정수배를 쓴다 */
        const val MIN_GRID_SPACING = 40f
        /** 이 각도(도) 안이면 북쪽 위로 맞추고 나침반을 숨긴다 */
        const val NORTH_SNAP_DEGREES = 5.0
        /** 배율 범위(칸에 맞춘 크기 = 1) */
        const val MIN_ZOOM = 0.125
        const val MAX_ZOOM = 4.0
        /** 손을 뗐을 때 1배에서 이 비율 안이면 1배로 맞춘다 */
        const val ZOOM_SNAP = 0.05

        /** 범위 안으로 자른다. NaN·무한·0 이하는 1배(iOS clampedZoom) */
        fun clampedZoom(zoom: Double): Double = if (!zoom.isFinite() || zoom <= 0) 1.0 else zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

        /** 손을 뗀 뒤 맞출 배율 */
        fun settledZoom(zoom: Double): Double {
            val clamped = clampedZoom(zoom)
            return if (kotlin.math.abs(clamped - 1) <= ZOOM_SNAP) 1.0 else clamped
        }

        /** [limit] 이하에서 가장 큰 1·2·5×10ⁿ 거리 */
        fun niceDistance(limit: Double): Double {
            if (!(limit > 0)) return 1.0
            val magnitude = 10.0.pow(floor(log10(limit)))
            for (step in listOf(5.0, 2.0, 1.0)) {
                if (step * magnitude <= limit) return step * magnitude
            }
            return magnitude
        }

        /** 0..<360 으로 맞추고, 북쪽에서 [NORTH_SNAP_DEGREES] 안이면 0 으로 맞춘다 */
        fun snappedDegrees(degrees: Double): Double {
            if (!degrees.isFinite()) return 0.0
            var value = degrees % 360
            if (value < 0) value += 360
            if (value < NORTH_SNAP_DEGREES || 360 - value < NORTH_SNAP_DEGREES) return 0.0
            return value
        }

        /** 나침반을 그리는가(북쪽 위가 아닐 때만) */
        fun showsCompass(degrees: Double): Boolean = snappedDegrees(degrees) != 0.0

        /** 손을 뗀 뒤 맞출 각도. 지금 각도에서 가장 가까운 같은 방향 값이라 0°/360° 경계에서 반대로 한 바퀴 돌지 않는다 */
        fun settledDegrees(current: Double): Double {
            val snapped = snappedDegrees(current)
            return snapped + 360 * Math.round((current - snapped) / 360).toDouble()
        }

        /** 나침반을 눌렀을 때 돌아갈 북쪽 위 각도(지금과 가장 가까운 360 의 배수) */
        fun northUpDegrees(current: Double): Double = 360 * Math.round(current / 360).toDouble()

        /** 축척 막대 표기. 1 km 이상은 km, 그 아래는 m(소수 한 자리까지) */
        fun distanceText(meters: Double, locale: Locale = Locale.getDefault()): String {
            val format = NumberFormat.getNumberInstance(locale).apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 1
            }
            return if (meters >= 1000) "${format.format(meters / 1000)} km" else "${format.format(meters)} m"
        }
    }
}

/**
 * 지도 없는 카드의 색. 배경 밝기로 두 벌 중 하나를 고른다(iOS ShapeShareSchematicPalette, 11/15 대표 2차).
 * 밝은 배경 = 어두운 글자·막대 + 흰 외곽선, 어두운 배경 = 밝은 글자·막대 + 어두운 외곽선.
 * 나침반 북쪽 바늘은 빨강, 남쪽 바늘은 옅은 청회색이라 적록색약에서도 밝기·푸른 기로 구분된다.
 */
internal data class ShapeShareSchematicPalette(val background: Rgb) {
    data class Rgb(val r: Double, val g: Double, val b: Double) {
        val color: Color get() = Color(r.toFloat(), g.toFloat(), b.toFloat())

        /** WCAG 상대 휘도 */
        val luminance: Double
            get() {
                fun linear(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
                return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
            }
    }

    enum class Tone { LIGHT, DARK }

    val tone: Tone get() = if (background.luminance > 0.18) Tone.LIGHT else Tone.DARK
    private val isLight get() = tone == Tone.LIGHT

    val ink: Rgb get() = if (isLight) Rgb(0.25, 0.26, 0.29) else Rgb(0.93, 0.94, 0.96)
    val halo: Rgb get() = if (isLight) Rgb(1.0, 1.0, 1.0) else Rgb(0.05, 0.06, 0.08)
    val grid: Rgb get() = if (isLight) Rgb(0.86, 0.88, 0.91) else Rgb(0.24, 0.26, 0.30)
    val northNeedle: Rgb get() = Rgb(0.90, 0.22, 0.21)
    val southNeedle: Rgb get() = if (isLight) Rgb(0.66, 0.74, 0.84) else Rgb(0.62, 0.71, 0.83)
    val compassRim: Rgb get() = if (isLight) Rgb(0.25, 0.26, 0.29) else Rgb(0.93, 0.94, 0.96)
    /**
     * 나침반 뒤 바탕 원(11/15 대표). 확대한 원 테두리가 나침반과 겹쳐도 북쪽 바늘이 보이게 한다. 축척에는 바탕을 두지 않는다.
     * 값은 iOS compassFace 와 같다(불투명도 [COMPASS_BACKGROUND_ALPHA]).
     */
    val compassBackground: Rgb get() = if (isLight) Rgb(1.0, 1.0, 1.0) else Rgb(0.10, 0.11, 0.13)

    companion object {
        /** 지금 카드가 쓰는 바탕(카드는 다크 모드와 상관없이 늘 밝게 그린다) */
        val lightBackground = Rgb(0.95, 0.96, 0.97)
        /** 어두운 바탕 예시(지금은 쓰는 곳 없음, 렌더 확인용) */
        val darkBackground = Rgb(0.12, 0.13, 0.15)
        val light = ShapeShareSchematicPalette(lightBackground)
        /** 나침반 바탕 원의 불투명도(불투명에 가깝게) */
        const val COMPASS_BACKGROUND_ALPHA = 0.92f
    }
}

private val SchematicFallbackShapeColor = Color(0xFF007AFF)
/** 글자·막대·나침반 테두리에 두르는 외곽선의 바깥 두께 */
private val SchematicHaloWidth = 1.dp
private const val SchematicCompassSize = 16f
/** 나침반을 누르기 쉽게 그림 둘레로 넓힌 영역 */
private const val SchematicCompassTouchInset = 6f

/**
 * 지도 없는 카드의 지도 칸. 지도 SDK·네트워크를 쓰지 않는다. 미리보기와 저장·복사·공유 이미지가 같은 함수·같은 각도라 늘 같다.
 * [rotationDegrees] 는 시계 방향(0 = 위쪽이 북쪽), [zoom] 은 칸에 맞춘 크기 대비 배율이고 둘 다 원일 때만 쓴다.
 * [onTransform]·[onTransformEnd]·[onNorthUp] 은 미리보기에서만 넘긴다.
 */
@Composable
internal fun ShapeShareSchematic(
    shape: ShapeModel,
    colorHex: String,
    rotationDegrees: Double,
    zoom: Double,
    modifier: Modifier = Modifier,
    width: Dp = ShapeShareCardWidth,
    height: Dp = ShapeShareCardMapHeight,
    palette: ShapeShareSchematicPalette = ShapeShareSchematicPalette.light,
    onTransform: ((rotationDelta: Double, zoomFactor: Double) -> Unit)? = null,
    onTransformEnd: (() -> Unit)? = null,
    onNorthUp: (() -> Unit)? = null,
) {
    val layout = remember(shape, width, height, zoom) { ShapeShareSchematicLayout(shape, width.value, height.value, zoom) }
    val degrees = if (layout.isRotatable) rotationDegrees else 0.0
    val showsCompass = ShapeShareSchematicLayout.showsCompass(degrees)
    val shapeColor = remember(colorHex) {
        parseIosOpaqueRgbHexColor(colorHex)?.let { Color(it) } ?: SchematicFallbackShapeColor
    }
    val description = schematicDescription(shape, layout, degrees)
    val compassLabel = stringResource(R.string.shape_share_schematic_compass)

    Box(
        modifier = modifier
            .size(width, height)
            .clipToBounds()
            .background(palette.background.color)
            .clearAndSetSemantics {
                contentDescription = description
                if (showsCompass && onNorthUp != null) {
                    customActions = listOf(CustomAccessibilityAction(compassLabel) { onNorthUp(); true })
                }
            }
            .then(
                if (layout.isRotatable && onTransform != null) {
                    Modifier.pointerInput(Unit) { detectTwoFingerTransform(onTransform, onTransformEnd ?: {}) }
                } else Modifier,
            ),
    ) {
        Canvas(modifier = Modifier.size(width, height)) {
            rotate(degrees.toFloat()) {
                layout.gridSpacing?.let { drawSchematicGrid(it.dp.toPx(), palette.grid.color) }
                val radius = layout.circleRadius
                if (radius != null) drawSchematicCircle(radius.dp.toPx(), shapeColor) else drawSchematicPoint(shapeColor)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (showsCompass) {
                SchematicCompass(degrees = degrees, palette = palette, onTap = onNorthUp)
            }
            val meters = layout.scaleMeters
            val length = layout.scaleBarLength
            if (meters != null && length != null) {
                SchematicScaleBar(meters = meters, length = length.dp, palette = palette)
            }
        }
    }
}

@Composable
private fun schematicDescription(shape: ShapeModel, layout: ShapeShareSchematicLayout, degrees: Double): String {
    val meters = layout.scaleMeters
    val radius = shape.radius
    if (meters == null || radius == null) return stringResource(R.string.shape_share_schematic_a11y_point)
    val angle = ShapeShareSchematicLayout.snappedDegrees(degrees).roundToInt() % 360
    val radiusText = formatShapeDetailMeters(radius)
    val scaleText = ShapeShareSchematicLayout.distanceText(meters)
    return if (angle == 0) {
        stringResource(R.string.shape_share_schematic_a11y_circle, radiusText, scaleText)
    } else {
        stringResource(R.string.shape_share_schematic_a11y_circle_rotated, radiusText, scaleText, angle)
    }
}

/**
 * 두 손가락일 때만 회전·확대를 함께 받는다. 한 손가락 끌기는 소비하지 않아 시트 스크롤을 막지 않는다.
 * 회전은 화면 기준 시계 방향 증가분(도), 확대는 이전 대비 배수다.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTwoFingerTransform(
    onTransform: (Double, Double) -> Unit,
    onTransformEnd: () -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var transformed = false
        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } >= 2) {
                val rotation = event.calculateRotation()
                val zoom = event.calculateZoom()
                if (rotation != 0f || zoom != 1f) {
                    transformed = true
                    onTransform(rotation.toDouble(), zoom.toDouble())
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (transformed) onTransformEnd()
    }
}

/** 축척 거리 정수배 간격의 옅은 격자(원 중심을 지난다). 돌려도 칸을 덮도록 대각선 길이만큼 그린다 */
private fun DrawScope.drawSchematicGrid(spacing: Float, color: Color) {
    val half = sqrt(size.width * size.width + size.height * size.height) / 2
    val count = ceil(half / spacing).toInt()
    val stroke = 0.5.dp.toPx()
    for (i in -count..count) {
        val offset = i * spacing
        drawLine(color, Offset(center.x + offset, center.y - half), Offset(center.x + offset, center.y + half), stroke)
        drawLine(color, Offset(center.x - half, center.y + offset), Offset(center.x + half, center.y + offset), stroke)
    }
}

/** 메인 지도 원 오버레이와 같은 스타일(채움 0.3, 안쪽 외곽선 2dp) + 중심점 */
private fun DrawScope.drawSchematicCircle(radius: Float, color: Color) {
    val line = 2.dp.toPx()
    drawCircle(color.copy(alpha = 0.3f), radius, center)
    drawCircle(color, radius - line / 2, center, style = Stroke(line))
    drawCircle(color, 2.5.dp.toPx(), center)
}

private fun DrawScope.drawSchematicPoint(color: Color) {
    drawCircle(color.copy(alpha = 0.18f), 18.dp.toPx(), center)
    drawCircle(color, 7.dp.toPx(), center)
    val border = 2.dp.toPx()
    drawCircle(Color.White, 7.dp.toPx() - border / 2, center, style = Stroke(border))
}

/** 숫자는 막대 가운데 위. 글자와 막대에 같은 외곽선을 둘러 한 덩어리로 보이게 한다(칸 없음) */
@Composable
private fun SchematicScaleBar(meters: Double, length: Dp, palette: ShapeShareSchematicPalette) {
    val haloPx = with(LocalDensity.current) { SchematicHaloWidth.toPx() }
    val text = ShapeShareSchematicLayout.distanceText(meters)
    val style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
        Box {
            BasicText(
                text = text,
                style = style.copy(
                    color = palette.halo.color,
                    drawStyle = Stroke(width = haloPx * 2, join = StrokeJoin.Round),
                ),
                softWrap = false,
            )
            BasicText(text = text, style = style.copy(color = palette.ink.color), softWrap = false)
        }
        Canvas(modifier = Modifier.size(length, 3.dp)) {
            val bar = Path().apply {
                moveTo(0f, 0f)
                lineTo(0f, size.height)
                lineTo(size.width, size.height)
                lineTo(size.width, 0f)
            }
            val line = 1.dp.toPx()
            drawPath(bar, palette.halo.color, style = Stroke(line + haloPx * 2, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(bar, palette.ink.color, style = Stroke(line, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/**
 * 북쪽을 가리키는 작은 나침반(빨강 바늘 = 북쪽, 옅은 청회색 바늘 = 남쪽). 그림이 돌아간 만큼 같이 돈다.
 * 바탕 원 위에 테두리 링과 바늘을 그린다.
 * 누르기 쉽도록 둘레를 넓힌 영역을 두고, 그만큼 옮겨 그림 자리는 16dp 나침반과 같게 둔다.
 */
@Composable
private fun SchematicCompass(degrees: Double, palette: ShapeShareSchematicPalette, onTap: (() -> Unit)?) {
    val inset = SchematicCompassTouchInset.dp
    val haloPx = with(LocalDensity.current) { SchematicHaloWidth.toPx() }
    Box(
        modifier = Modifier
            .offset(x = inset, y = inset)
            .size(SchematicCompassSize.dp + inset * 2)
            .then(
                if (onTap != null) {
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap)
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(SchematicCompassSize.dp)) {
            val unit = size.width / SchematicCompassSize
            rotate(degrees.toFloat()) {
                // 외곽선은 테두리에만 둔다(바늘에 두르면 빨강 바늘 안쪽이 번진다)
                val rim = 0.75.dp.toPx()
                val rimRadius = size.minDimension / 2 - rim / 2
                drawCircle(
                    palette.compassBackground.color.copy(alpha = ShapeShareSchematicPalette.COMPASS_BACKGROUND_ALPHA),
                    size.minDimension / 2,
                )
                drawCircle(palette.halo.color, rimRadius, style = Stroke(rim + haloPx * 2))
                drawCircle(palette.compassRim.color.copy(alpha = 0.55f), rimRadius, style = Stroke(rim))
                fun needle(tipY: Float) = Path().apply {
                    moveTo(8 * unit, tipY * unit)
                    lineTo(10 * unit, 8 * unit)
                    lineTo(6 * unit, 8 * unit)
                    close()
                }
                drawPath(needle(2.5f), palette.northNeedle.color)
                drawPath(needle(13.5f), palette.southNeedle.color)
            }
        }
    }
}
