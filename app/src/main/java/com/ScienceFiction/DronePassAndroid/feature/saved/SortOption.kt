package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.annotation.StringRes
import com.ScienceFiction.DronePassAndroid.R

/**
 * 저장 목록 정렬 기준.
 *
 * [labelRes] 는 strings.xml 에 정의된 다국어 문자열 리소스 ID 이다.
 * 화면에서는 `stringResource(option.labelRes)` 로 사용한다.
 */
enum class SortOption(
    val rawValue: String,
    @StringRes val labelRes: Int,
) {
    TITLE("title", R.string.saved_sort_title),
    DATE_CREATED("dateCreated", R.string.saved_sort_date_created),
    FLIGHT_START("flightStartDate", R.string.saved_sort_flight_start),
    FLIGHT_END("flightEndDate", R.string.saved_sort_flight_end);

    companion object {
        fun fromRawValue(rawValue: String?): SortOption {
            return entries.firstOrNull { it.rawValue == rawValue } ?: TITLE
        }
    }
}

enum class SortDirection(
    val rawValue: String,
    @StringRes val labelRes: Int,
) {
    ASCENDING("ascending", R.string.saved_sort_ascending),
    DESCENDING("descending", R.string.saved_sort_descending);

    companion object {
        fun fromRawValue(rawValue: String?): SortDirection {
            return entries.firstOrNull { it.rawValue == rawValue } ?: ASCENDING
        }
    }
}

/** iOS ShapeSortingManager.cycleSortOption 동등 — enum 다음 값으로 순환한다. */
internal fun nextSavedSortOption(current: SortOption): SortOption {
    val values = SortOption.entries
    return values[(current.ordinal + 1) % values.size]
}
