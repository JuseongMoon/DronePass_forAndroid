package com.ScienceFiction.DronePassAndroid.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * iOS `WeatherManager.precipitationIconName` 이 쓰는 SF Symbols(테두리형)에 맞춘 벡터 아이콘.
 * sun.max, cloud.sun, cloud, cloud.fog, wind.snow, cloud.bolt 와
 * 강수용 cloud.rain / cloud.drizzle / cloud.snow / cloud.sleet / cloud.hail / cloud.bolt.rain.
 * Material 아이콘에는 같은 조합이 없어 직접 그린다. 모두 24x24, 선 굵기 1.8.
 */
object IosWeatherSymbols {

    private const val Stroke = 1.8f

    /** 아래에 강수 표시가 붙는 구름(위로 올린 위치). */
    private const val UpperCloud =
        "M7.2,13.6A3.9,3.9 0,0 1,6.7 5.9A5.2,5.2 0,0 1,16.5 5.4A4.1,4.1 0,1 1,17.1 13.6Z"

    /** 단독 구름(세로 가운데). */
    private const val CenterCloud =
        "M7.2,17.2A4.1,4.1 0,0 1,6.6 9.1A5.5,5.5 0,0 1,16.9 8.5A4.35,4.35 0,1 1,17.5 17.2Z"

    val SunMax: ImageVector by lazy {
        symbol("sun.max") {
            stroke("M12,7.6A4.4,4.4 0,1 1,11.99 7.6Z")
            stroke(rays(cx = 12f, cy = 12f, inner = 6.9f, outer = 9.6f, count = 8))
        }
    }

    val CloudSun: ImageVector by lazy {
        symbol("cloud.sun") {
            // 왼쪽 위의 해(구름에 가려진 부분은 그리지 않는다)
            stroke("M5.3,10.1A3.4,3.4 0,0 1,11.3 6.9")
            stroke(rays(cx = 8.3f, cy = 8.6f, inner = 4.9f, outer = 6.8f, count = 8, skipFrom = 0, skipTo = 2))
            stroke("M9.2,19.4A3.6,3.6 0,0 1,8.7 12.3A4.8,4.8 0,0 1,17.6 11.8A3.8,3.8 0,1 1,18.1 19.4Z")
        }
    }

    val Cloud: ImageVector by lazy { symbol("cloud") { stroke(CenterCloud) } }

    val CloudFog: ImageVector by lazy {
        symbol("cloud.fog") {
            stroke(UpperCloud)
            stroke("M5,17.2H19M7,20.4H17")
        }
    }

    val WindSnow: ImageVector by lazy {
        symbol("wind.snow") {
            stroke("M2.5,9H12A2.4,2.4 0,1 0,9.6 6.6M2.5,13H15.5A2.4,2.4 0,1 1,13.1 15.4M2.5,17H9")
            stroke(
                "M19,3.6V7.4M17.35,4.55L20.65,6.45M17.35,6.45L20.65,4.55" +
                    "M19,16.1V19.9M17.35,17.05L20.65,18.95M17.35,18.95L20.65,17.05",
                width = 1.4f,
            )
        }
    }

    val CloudBolt: ImageVector by lazy {
        symbol("cloud.bolt") {
            stroke(UpperCloud)
            fill("M12.9,14.6L10,19H12.2L11.2,22.6L14.5,17.9H12.3Z")
        }
    }

    val CloudRain: ImageVector by lazy {
        symbol("cloud.rain") {
            stroke(UpperCloud)
            stroke("M8.2,16.6L7,20.2M12.2,16.6L11,20.2M16.2,16.6L15,20.2")
        }
    }

    val CloudDrizzle: ImageVector by lazy {
        symbol("cloud.drizzle") {
            stroke(UpperCloud)
            stroke("M8,17L8,17.1M12,17L12,17.1M16,17L16,17.1M10,20L10,20.1M14,20L14,20.1", width = 2.2f)
        }
    }

    val CloudSnow: ImageVector by lazy {
        symbol("cloud.snow") {
            stroke(UpperCloud)
            stroke(
                "M8,16.4V20.4M6.3,17.4L9.7,19.4M6.3,19.4L9.7,17.4" +
                    "M16,16.4V20.4M14.3,17.4L17.7,19.4M14.3,19.4L17.7,17.4",
                width = 1.4f,
            )
        }
    }

    val CloudSleet: ImageVector by lazy {
        symbol("cloud.sleet") {
            stroke(UpperCloud)
            stroke("M8.2,16.6L7,20.2")
            stroke("M16,16.4V20.4M14.3,17.4L17.7,19.4M14.3,19.4L17.7,17.4", width = 1.4f)
        }
    }

    val CloudHail: ImageVector by lazy {
        symbol("cloud.hail") {
            stroke(UpperCloud)
            fill(
                "M8,17.1A1.1,1.1 0,1 1,7.99 17.1ZM12,19.1A1.1,1.1 0,1 1,11.99 19.1Z" +
                    "M16,17.1A1.1,1.1 0,1 1,15.99 17.1Z",
            )
        }
    }

    val CloudBoltRain: ImageVector by lazy {
        symbol("cloud.bolt.rain") {
            stroke(UpperCloud)
            stroke("M7.6,16.6L6.6,19.6M17.4,16.6L16.4,19.6")
            fill("M12.8,15.2L10,19.2H12.2L11.2,22.6L14.4,18.2H12.2Z")
        }
    }

    private class SymbolScope(val builder: ImageVector.Builder) {
        fun stroke(path: String, width: Float = Stroke) {
            builder.addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }

        fun fill(path: String) {
            builder.addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
        }
    }

    private fun symbol(name: String, draw: SymbolScope.() -> Unit): ImageVector {
        val builder = ImageVector.Builder(
            name = "IosWeatherSymbols.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        SymbolScope(builder).draw()
        return builder.build()
    }

    /** 원 둘레의 빛살. [skipFrom]..[skipTo] 번째(0 = 오른쪽, 시계 방향)는 가려진 부분이라 생략한다. */
    private fun rays(
        cx: Float,
        cy: Float,
        inner: Float,
        outer: Float,
        count: Int,
        skipFrom: Int = -1,
        skipTo: Int = -1,
    ): String = buildString {
        for (k in 0 until count) {
            if (k in skipFrom..skipTo) continue
            val a = Math.PI * 2 * k / count
            val x1 = cx + inner * cos(a)
            val y1 = cy + inner * sin(a)
            val x2 = cx + outer * cos(a)
            val y2 = cy + outer * sin(a)
            append("M%.2f,%.2fL%.2f,%.2f".format(java.util.Locale.ROOT, x1, y1, x2, y2))
        }
    }
}
