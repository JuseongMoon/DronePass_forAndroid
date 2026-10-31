package com.ScienceFiction.DronePassAndroid.feature.shape

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import com.squareup.moshi.Moshi
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 공유 카드에 표시할 도형 정보 (iOS ShapeShareCardContent 정합). 메모는 넣지 않는다.
 */
internal data class ShapeShareCardContent(
    val title: String,
    /** 주소가 없거나 "-"이거나 꺼져 있으면 null (행을 그리지 않는다). 간략히를 켜면 시·군·구까지만 */
    val address: String?,
    /** "시작" 또는 "시작 ~ 종료". 일 단위 도형은 날짜만, 하루짜리면 날짜 하나만. 꺼져 있으면 null */
    val period: String?,
    val radius: String?,
    val height: String?,
    val droneName: String?,
    val droneColorHex: String?,
    val tagName: String?,
    /** 꺼져 있으면 null */
    val coordinate: String?,
    /** 지도에 그릴 도형 색. 만료 회색은 적용하지 않는다(공유 카드는 원래 색으로 보여 준다) */
    val shapeColorHex: String,
)

internal fun buildShapeShareCardContent(
    shape: ShapeModel,
    drone: DroneModel?,
    tag: TagModel? = null,
    options: ShapeShareCardOptions = ShapeShareCardOptions(),
    dateTimeFormat: DateFormat = localizedShapeDateTimeFormat(),
    dateFormat: DateFormat = shapeEditDateOnlyFormat(),
    timeZone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault(),
): ShapeShareCardContent {
    val trimmedAddress = shape.address?.trim().orEmpty()

    val address = when {
        !options.showAddress || trimmedAddress.isEmpty() || trimmedAddress == "-" -> null
        options.simplifyAddress -> koreanAddressRegionSummary(trimmedAddress)
        else -> trimmedAddress
    }

    val period = if (options.showPeriod) {
        val isDateOnly = shape.isDateOnlyFlight(timeZone)
        val format = if (isDateOnly) dateFormat else dateTimeFormat
        val start = format.format(Date(shape.flightStartDate))
        val end = shape.flightEndDate
        if (end != null && !(isDateOnly && isSameDay(shape.flightStartDate, end, timeZone))) {
            "$start ~ ${format.format(Date(end))}"
        } else {
            start
        }
    } else {
        null
    }

    return ShapeShareCardContent(
        title = shapeDetailTitleText(shape.title),
        address = address,
        period = period,
        radius = shapeShareCardMeters(shape.radius, locale),
        height = shapeShareCardMeters(shape.height, locale),
        droneName = drone?.name?.takeIf { options.showDrone },
        droneColorHex = drone?.color?.takeIf { options.showDrone },
        tagName = tag?.name?.takeIf { options.showTag },
        coordinate = shape.baseCoordinate.formattedCoordinate.takeIf { options.showCoordinate },
        shapeColorHex = shape.color,
    )
}

/** 상세 화면과 같은 미터 표기. 값이 없거나 0 이하면 null */
private fun shapeShareCardMeters(value: Double?, locale: Locale): String? {
    if (value == null || value <= 0) return null
    return formatShapeDetailMeters(value, locale)
}

private fun isSameDay(first: Long, second: Long, timeZone: TimeZone): Boolean {
    val a = Calendar.getInstance(timeZone).apply { timeInMillis = first }
    val b = Calendar.getInstance(timeZone).apply { timeInMillis = second }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

/**
 * 공유 카드에서 켜고 끄는 항목. 지도·제목·반경·고도·로고는 항상 들어간다.
 * 기기 설정이라 마지막 선택을 기억하고, 계정 전환 때 지우지 않는다 (iOS ShapeShareCardOptions 정합).
 */
internal data class ShapeShareCardOptions(
    val showAddress: Boolean = true,
    /**
     * 주소를 시·군·구까지만 (주소가 켜져 있을 때만 의미가 있다). 처음 기본값은 간략히다(D-50, 2026-10-30).
     * 저장값에는 이 키가 항상 들어가므로 이미 고른 사용자는 그 값을 그대로 쓴다.
     */
    val simplifyAddress: Boolean = true,
    val showCoordinate: Boolean = true,
    val showPeriod: Boolean = true,
    val showDrone: Boolean = true,
    val showTag: Boolean = false,
) {
    fun toJson(): String = optionsAdapter.toJson(
        mapOf(
            "showAddress" to showAddress,
            "simplifyAddress" to simplifyAddress,
            "showCoordinate" to showCoordinate,
            "showPeriod" to showPeriod,
            "showDrone" to showDrone,
            "showTag" to showTag,
        ),
    )

    companion object {
        const val STORAGE_KEY = "shapeShareCardOptions"
        private val optionsAdapter = Moshi.Builder().build().adapter(Map::class.java)

        /** 나중에 항목이 늘어도 저장된 옛 값을 읽을 수 있게 빠진 키는 기본값으로 둔다 */
        fun fromJson(json: String?): ShapeShareCardOptions {
            val fallback = ShapeShareCardOptions()
            val root = json?.let { runCatching { optionsAdapter.fromJson(it) }.getOrNull() } ?: return fallback
            fun flag(key: String, default: Boolean) = root[key] as? Boolean ?: default
            return ShapeShareCardOptions(
                showAddress = flag("showAddress", fallback.showAddress),
                simplifyAddress = flag("simplifyAddress", fallback.simplifyAddress),
                showCoordinate = flag("showCoordinate", fallback.showCoordinate),
                showPeriod = flag("showPeriod", fallback.showPeriod),
                showDrone = flag("showDrone", fallback.showDrone),
                showTag = flag("showTag", fallback.showTag),
            )
        }
    }
}

/**
 * 한국 주소를 지역 단위로 줄인다 (공유 카드 "주소 간략히", iOS KoreanAddress.regionSummary 정합).
 * 시·도 다음에 시/군/구로 끝나는 토큰을 잇고, 군이나 구에서 멈춘다. 규칙에 맞지 않는 주소(해외 등)는 앞의 두 토큰을 쓴다.
 */
internal fun koreanAddressRegionSummary(address: String): String {
    val tokens = address.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val first = tokens.firstOrNull() ?: return address
    if (!(first.endsWith("도") || first.endsWith("시"))) return tokens.take(2).joinToString(" ")

    val result = mutableListOf(first)
    for (token in tokens.drop(1)) {
        if (token.endsWith("군") || token.endsWith("구")) {
            result += token
            break
        }
        if (token.endsWith("시")) {
            result += token
            continue
        }
        break
    }
    return result.joinToString(" ")
}
