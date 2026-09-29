package com.ScienceFiction.DronePassAndroid.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * iOS `WeatherManager.precipitationIconName` 의 SF Symbols(cloud.rain, cloud.drizzle, cloud.snow,
 * cloud.sleet, cloud.bolt.rain)에 맞춘 벡터 아이콘. Material 아이콘에는 구름+강수 조합이 없어 직접 그린다.
 * 모두 24x24, 위쪽에 같은 구름을 두고 아래에 강수 표시만 바꾼다.
 */
object IosWeatherSymbols {

    private const val CloudPath =
        "M7,14A4.2,4.2 0,0 1,6.2 5.7A5.6,5.6 0,0 1,16.7 5A4.5,4.5 0,1 1,17.3 14Z"

    val CloudRain: ImageVector by lazy {
        symbol("cloud.rain", strokes = "M8.2,16.6L7,20.2M12.2,16.6L11,20.2M16.2,16.6L15,20.2")
    }

    val CloudDrizzle: ImageVector by lazy {
        symbol("cloud.drizzle", strokes = "M8,17L8,17.1M12,17L12,17.1M16,17L16,17.1M10,20L10,20.1M14,20L14,20.1")
    }

    val CloudSnow: ImageVector by lazy {
        symbol(
            "cloud.snow",
            strokes = "M8,16.4V20.4M6.3,17.4L9.7,19.4M6.3,19.4L9.7,17.4" +
                "M16,16.4V20.4M14.3,17.4L17.7,19.4M14.3,19.4L17.7,17.4",
            strokeWidth = 1.4f,
        )
    }

    val CloudSleet: ImageVector by lazy {
        symbol(
            "cloud.sleet",
            strokes = "M8.2,16.6L7,20.2M16,16.4V20.4M14.3,17.4L17.7,19.4M14.3,19.4L17.7,17.4",
            strokeWidth = 1.5f,
        )
    }

    val CloudBoltRain: ImageVector by lazy {
        symbol(
            "cloud.bolt.rain",
            strokes = "M7.6,16.6L6.6,19.6M17.4,16.6L16.4,19.6",
            fills = "M12.8,15.2L10,19.2H12.2L11.2,22.6L14.4,18.2H12.2Z",
        )
    }

    private fun symbol(
        name: String,
        strokes: String,
        fills: String? = null,
        strokeWidth: Float = 1.9f,
    ): ImageVector = ImageVector.Builder(
        name = "IosWeatherSymbols.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(pathData = addPathNodes(CloudPath), fill = SolidColor(Color.Black))
        addPath(
            pathData = addPathNodes(strokes),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        if (fills != null) {
            addPath(pathData = addPathNodes(fills), fill = SolidColor(Color.Black))
        }
    }.build()
}
