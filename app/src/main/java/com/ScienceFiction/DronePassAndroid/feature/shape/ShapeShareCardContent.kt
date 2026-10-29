package com.ScienceFiction.DronePassAndroid.feature.shape

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
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
    /** 주소가 없거나 "-"이면 null (행을 그리지 않는다) */
    val address: String?,
    /** "시작" 또는 "시작 ~ 종료". 일 단위 도형은 날짜만, 하루짜리면 날짜 하나만 */
    val period: String,
    val radius: String?,
    val height: String?,
    val droneName: String?,
    val droneColorHex: String?,
    val coordinate: String,
    /** 지도에 그릴 도형 색. 만료 회색은 적용하지 않는다(공유 카드는 원래 색으로 보여 준다) */
    val shapeColorHex: String,
)

internal fun buildShapeShareCardContent(
    shape: ShapeModel,
    drone: DroneModel?,
    dateTimeFormat: DateFormat = localizedShapeDateTimeFormat(),
    dateFormat: DateFormat = shapeEditDateOnlyFormat(),
    timeZone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault(),
): ShapeShareCardContent {
    val trimmedAddress = shape.address?.trim().orEmpty()

    val isDateOnly = shape.isDateOnlyFlight(timeZone)
    val format = if (isDateOnly) dateFormat else dateTimeFormat
    val start = format.format(Date(shape.flightStartDate))
    val end = shape.flightEndDate
    val period = if (end != null && !(isDateOnly && isSameDay(shape.flightStartDate, end, timeZone))) {
        "$start ~ ${format.format(Date(end))}"
    } else {
        start
    }

    return ShapeShareCardContent(
        title = shapeDetailTitleText(shape.title),
        address = trimmedAddress.takeUnless { it.isEmpty() || it == "-" },
        period = period,
        radius = shapeShareCardMeters(shape.radius, locale),
        height = shapeShareCardMeters(shape.height, locale),
        droneName = drone?.name,
        droneColorHex = drone?.color,
        coordinate = shape.baseCoordinate.formattedCoordinate,
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
